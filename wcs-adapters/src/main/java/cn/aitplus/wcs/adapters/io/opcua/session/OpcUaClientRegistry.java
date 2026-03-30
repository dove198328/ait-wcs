package cn.aitplus.wcs.adapters.io.opcua.session;

import cn.aitplus.wcs.adapters.io.connection.ConnectionKey;
import cn.aitplus.wcs.adapters.io.opcua.config.OpcUaAdapterProperties;
import cn.aitplus.wcs.core.spi.device.DeviceEndpoint;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.UaSession;
import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 按 {@link ConnectionKey} 复用 {@link OpcUaClient}：会话探活、空闲淘汰、Session 状态回调，但不主动干扰 Milo 自动重连。
 */
public final class OpcUaClientRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(OpcUaClientRegistry.class);

    private final OpcUaAdapterProperties properties;
    private final ConcurrentHashMap<ConnectionKey, ManagedClient> pool = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<OpcUaConnectionListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean destroyed;

    public OpcUaClientRegistry(OpcUaAdapterProperties properties) {
        this.properties = properties;
    }

    public void addListener(OpcUaConnectionListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public <T> T executeWithClient(ConnectionKey key, DeviceEndpoint endpoint, OpcUaClientCallback<T> callback)
        throws Exception {
        ManagedClient managed = pool.computeIfAbsent(key, k -> new ManagedClient());
        synchronized (managed.lock) {
            if (destroyed) {
                throw new IllegalStateException("OPC UA registry is destroyed");
            }
            OpcUaClient client = internalEnsureConnected(key, managed, endpoint);
            managed.lastUsedAtMillis = System.currentTimeMillis();
            return callback.apply(client);
        }
    }

    public void teardownConnection(ConnectionKey key) {
        ManagedClient m = pool.get(key);
        if (m != null) {
            synchronized (m.lock) {
                teardownClient(key, m, true);
            }
        }
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void destroy() {
        destroyed = true;
        List<Map.Entry<ConnectionKey, ManagedClient>> entries = new ArrayList<>(pool.entrySet());
        for (Map.Entry<ConnectionKey, ManagedClient> e : entries) {
            synchronized (e.getValue().lock) {
                teardownClient(e.getKey(), e.getValue(), true);
            }
        }
        pool.clear();
    }

    private OpcUaClient internalEnsureConnected(ConnectionKey key, ManagedClient managed, DeviceEndpoint endpoint)
        throws Exception {
        String endpointUrl = endpoint.getOpcEndpointUrl().trim();
        ConnectionContext ctx = buildConnectionContext(endpointUrl);
        long now = System.currentTimeMillis();
        long idleMax = properties.getIdleMaxMillis();
        if (managed.client != null && idleMax > 0 && managed.lastUsedAtMillis > 0 && now - managed.lastUsedAtMillis > idleMax) {
            LOG.debug("OPC UA idle {} ms exceeded for {}, reconnecting", idleMax, key);
            teardownClient(key, managed, false);
        }

        if (managed.client != null) {
            try {
                managed.client.getSession();
                if (properties.isSessionProbeEnabled()) {
                    DataValue hb = managed.client.readValue(0.0, TimestampsToReturn.Neither, Identifiers.Server_ServerStatus_State);
                    boolean good = hb != null && hb.getStatusCode() != null && hb.getStatusCode().isGood();
                    if (!good) {
                        StatusCode sc = hb != null ? hb.getStatusCode() : null;
                        throw new IllegalStateException("OPC UA session probe not good: " + sc);
                    }
                }
                attachSessionListenerIfNeeded(key, managed);
                return managed.client;
            } catch (Exception e) {
                LOG.debug("OPC UA session check or probe failed, reconnecting: {}", e.getMessage());
                teardownClient(key, managed, false);
            }
        }

        if (!properties.isInsecureTrustServerCertificate()) {
            throw new IllegalStateException(
                "Strict OPC UA server certificate validation is not wired yet; keep insecure-trust-server-certificate=true or implement a trust store");
        }

        CertificateValidator certValidator = new CertificateValidator.InsecureCertificateValidator();
        OpcUaClient client = OpcUaClient.create(
            endpointUrl,
            endpoints -> selectEndpoint(endpoints, ctx.securityPolicy, ctx.messageSecurityMode, ctx.userTokenType),
            null,
            (OpcUaClientConfigBuilder b) -> b
                .setApplicationName(LocalizedText.english(properties.getApplicationName()))
                .setApplicationUri(properties.getApplicationUri())
                .setIdentityProvider(ctx.identityProvider)
                .setCertificateValidator(certValidator)
                .setRequestTimeout(Unsigned.uint(properties.getDefaultRequestTimeoutMillis()))
                .build()
        );

        client.connect();
        managed.client = client;
        managed.sessionListenerAttachedTo = null;
        attachSessionListenerIfNeeded(key, managed);
        for (OpcUaConnectionListener l : listeners) {
            try {
                l.onClientConnected(key, client);
            } catch (Exception e) {
                LOG.warn("OpcUaConnectionListener.onClientConnected failed for {}", key, e);
            }
        }
        return client;
    }

    private void attachSessionListenerIfNeeded(ConnectionKey key, ManagedClient managed) {
        OpcUaClient c = managed.client;
        if (c == null || c == managed.sessionListenerAttachedTo) {
            return;
        }
        c.addSessionActivityListener(new SessionActivityListener() {
            @Override
            public void onSessionActive(UaSession session) {
                LOG.debug("OPC UA session active {}", key);
                for (OpcUaConnectionListener l : listeners) {
                    try {
                        l.onSessionActive(key, c);
                    } catch (Exception e) {
                        LOG.warn("OpcUaConnectionListener.onSessionActive failed for {}", key, e);
                    }
                }
            }

            @Override
            public void onSessionInactive(UaSession session) {
                LOG.warn("OPC UA session inactive {}, waiting for Milo reconnect", key);
                for (OpcUaConnectionListener l : listeners) {
                    try {
                        l.onSessionInactive(key, c);
                    } catch (Exception e) {
                        LOG.warn("OpcUaConnectionListener.onSessionInactive failed for {}", key, e);
                    }
                }
            }
        });
        managed.sessionListenerAttachedTo = c;
    }

    private void teardownClient(ConnectionKey key, ManagedClient managed, boolean removeEntry) {
        OpcUaClient c = managed.client;
        for (OpcUaConnectionListener l : listeners) {
            try {
                l.beforeClientTeardown(key, c);
            } catch (Exception e) {
                LOG.warn("OpcUaConnectionListener.beforeClientTeardown failed for {}", key, e);
            }
        }
        managed.client = null;
        managed.sessionListenerAttachedTo = null;
        managed.lastUsedAtMillis = 0L;
        if (removeEntry) {
            pool.remove(key, managed);
        }
        if (c != null) {
            try {
                c.disconnect();
            } catch (Exception e) {
                LOG.warn("OPC UA disconnect failed", e);
            }
        }
        for (OpcUaConnectionListener l : listeners) {
            try {
                l.afterClientTeardown(key);
            } catch (Exception e) {
                LOG.warn("OpcUaConnectionListener.afterClientTeardown failed for {}", key, e);
            }
        }
    }

    private ConnectionContext buildConnectionContext(String endpointUrl) {
        SecurityPolicy securityPolicy = parseSecurityPolicy(properties.getSecurityPolicy());
        MessageSecurityMode messageSecurityMode = parseMessageSecurityMode(properties.getMessageSecurityMode());
        if (securityPolicy == SecurityPolicy.None && messageSecurityMode != MessageSecurityMode.None) {
            throw new IllegalArgumentException("messageSecurityMode must be None when securityPolicy=None for " + endpointUrl);
        }

        boolean usernameMode = StringUtils.hasText(properties.getUsername());
        IdentityProvider identity = usernameMode
            ? new UsernameProvider(properties.getUsername(), properties.getPassword() != null ? properties.getPassword() : "")
            : AnonymousProvider.INSTANCE;
        UserTokenType tokenType = usernameMode ? UserTokenType.UserName : UserTokenType.Anonymous;
        return new ConnectionContext(securityPolicy, messageSecurityMode, identity, tokenType);
    }

    private static Optional<EndpointDescription> selectEndpoint(List<EndpointDescription> endpoints,
                                                                SecurityPolicy policy,
                                                                MessageSecurityMode messageSecurityMode,
                                                                UserTokenType userTokenType) {
        if (endpoints == null || endpoints.isEmpty()) {
            return Optional.empty();
        }
        String policyUri = policy.getUri();
        return endpoints.stream()
            .filter(e -> policyUri.equals(e.getSecurityPolicyUri()))
            .filter(e -> e.getSecurityMode() == messageSecurityMode)
            .filter(e -> supportsUserToken(e, userTokenType))
            .findFirst();
    }

    private static boolean supportsUserToken(EndpointDescription endpoint, UserTokenType expected) {
        UserTokenPolicy[] policies = endpoint.getUserIdentityTokens();
        if (policies == null || policies.length == 0) {
            return expected == UserTokenType.Anonymous;
        }
        for (UserTokenPolicy policy : policies) {
            if (policy != null && policy.getTokenType() == expected) {
                return true;
            }
        }
        return false;
    }

    private static SecurityPolicy parseSecurityPolicy(String name) {
        if (!StringUtils.hasText(name)) {
            return SecurityPolicy.None;
        }
        String n = name.trim();
        try {
            return SecurityPolicy.valueOf(n);
        } catch (IllegalArgumentException ignored) {
        }
        switch (n.toUpperCase(Locale.ROOT)) {
            case "NONE":
                return SecurityPolicy.None;
            case "BASIC128RSA15":
                return SecurityPolicy.Basic128Rsa15;
            case "BASIC256":
                return SecurityPolicy.Basic256;
            case "BASIC256SHA256":
                return SecurityPolicy.Basic256Sha256;
            case "AES128_SHA256_RSAOAEP":
                return SecurityPolicy.Aes128_Sha256_RsaOaep;
            case "AES256_SHA256_RSAPSS":
                return SecurityPolicy.Aes256_Sha256_RsaPss;
            default:
                LOG.warn("Unknown OPC UA security policy '{}', using None", name);
                return SecurityPolicy.None;
        }
    }

    private static MessageSecurityMode parseMessageSecurityMode(String name) {
        if (!StringUtils.hasText(name)) {
            return MessageSecurityMode.None;
        }
        String n = name.trim();
        try {
            return MessageSecurityMode.valueOf(n);
        } catch (IllegalArgumentException ignored) {
        }
        switch (n.toUpperCase(Locale.ROOT)) {
            case "NONE":
                return MessageSecurityMode.None;
            case "SIGN":
                return MessageSecurityMode.Sign;
            case "SIGNANDENCRYPT":
            case "SIGN_AND_ENCRYPT":
                return MessageSecurityMode.SignAndEncrypt;
            default:
                LOG.warn("Unknown OPC UA message security mode '{}', using None", name);
                return MessageSecurityMode.None;
        }
    }

    private static final class ConnectionContext {
        final SecurityPolicy securityPolicy;
        final MessageSecurityMode messageSecurityMode;
        final IdentityProvider identityProvider;
        final UserTokenType userTokenType;

        ConnectionContext(SecurityPolicy securityPolicy,
                          MessageSecurityMode messageSecurityMode,
                          IdentityProvider identityProvider,
                          UserTokenType userTokenType) {
            this.securityPolicy = securityPolicy;
            this.messageSecurityMode = messageSecurityMode;
            this.identityProvider = identityProvider;
            this.userTokenType = userTokenType;
        }
    }

    static final class ManagedClient {
        final Object lock = new Object();
        OpcUaClient client;
        long lastUsedAtMillis;
        OpcUaClient sessionListenerAttachedTo;
    }
}
