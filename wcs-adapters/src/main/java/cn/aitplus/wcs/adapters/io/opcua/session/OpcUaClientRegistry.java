package cn.aitplus.wcs.adapters.io.opcua.session;

import cn.aitplus.wcs.adapters.io.connection.ConnectionKey;
import cn.aitplus.wcs.adapters.io.opcua.config.OpcUaAdapterProperties;
import cn.aitplus.wcs.core.spi.device.DeviceEndpoint;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.api.UaSession;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.client.security.ClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 按 {@link ConnectionKey} 复用 {@link OpcUaClient}：会话探活、空闲淘汰、Session 失效拆除，并向 {@link OpcUaConnectionListener} 广播。
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

    public <T> T executeWithClient(ConnectionKey key, DeviceEndpoint endpoint, long timeoutMs, OpcUaClientCallback<T> callback)
        throws Exception {
        ManagedClient managed = pool.computeIfAbsent(key, k -> new ManagedClient());
        synchronized (managed.lock) {
            if (destroyed) {
                throw new IllegalStateException("OPC UA registry is destroyed");
            }
            OpcUaClient client = internalEnsureConnected(key, managed, endpoint, timeoutMs);
            managed.lastUsedAtMillis = System.currentTimeMillis();
            return callback.apply(client);
        }
    }

    /**
     * 在连接锁内读取当前 client 引用（可能为 null）；供订阅模块删除 Subscription 等。
     */
    public OpcUaClient peekClient(ConnectionKey key) {
        ManagedClient m = pool.get(key);
        if (m == null) {
            return null;
        }
        synchronized (m.lock) {
            return m.client;
        }
    }

    /**
     * 丢弃指定键下客户端（如同步读/写失败时）；与 {@link #executeWithClient} 使用同一把连接锁。
     */
    public void teardownConnection(ConnectionKey key) {
        ManagedClient m = pool.get(key);
        if (m != null) {
            synchronized (m.lock) {
                teardownClient(key, m);
            }
        }
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void destroy() {
        destroyed = true;
        for (Map.Entry<ConnectionKey, ManagedClient> e : pool.entrySet()) {
            synchronized (e.getValue().lock) {
                teardownClient(e.getKey(), e.getValue());
            }
        }
        pool.clear();
    }

    private OpcUaClient internalEnsureConnected(ConnectionKey key, ManagedClient managed, DeviceEndpoint endpoint, long timeoutMs)
        throws Exception {
        String endpointUrl = endpoint.getOpcEndpointUrl().trim();
        long now = System.currentTimeMillis();
        long idleMax = properties.getIdleMaxMillis();
        if (managed.client != null && idleMax > 0 && managed.lastUsedAtMillis > 0 && now - managed.lastUsedAtMillis > idleMax) {
            LOG.debug("OPC UA idle {} ms exceeded for {}, reconnecting", idleMax, key);
            teardownClient(key, managed);
        }

        if (managed.client != null) {
            try {
                managed.client.getSession().get(Math.min(500L, timeoutMs), TimeUnit.MILLISECONDS);
                if (properties.isSessionProbeEnabled()) {
                    long probeMs = properties.getSessionProbeTimeoutMillis() > 0
                        ? properties.getSessionProbeTimeoutMillis()
                        : Math.min(timeoutMs, 3_000L);
                    DataValue hb = managed.client.readValue(0, TimestampsToReturn.Neither, Identifiers.Server_ServerStatus_State)
                        .get(probeMs, TimeUnit.MILLISECONDS);
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
                teardownClient(key, managed);
            }
        }

        SecurityPolicy policy = parseSecurityPolicy(properties.getSecurityPolicy());
        IdentityProvider identity = StringUtils.hasText(properties.getUsername())
            ? new UsernameProvider(properties.getUsername(),
            properties.getPassword() != null ? properties.getPassword() : "")
            : AnonymousProvider.INSTANCE;

        ClientCertificateValidator certValidator = new ClientCertificateValidator.InsecureValidator();
        if (!properties.isInsecureTrustServerCertificate()) {
            LOG.warn("wcs.adapter.opcua.insecure-trust-server-certificate=false but strict trust store is not wired yet; still using insecure server certificate acceptance");
        }

        OpcUaClient client = OpcUaClient.create(
            endpointUrl,
            endpoints -> selectEndpoint(endpoints, policy),
            (OpcUaClientConfigBuilder b) -> {
                OpcUaClientConfigBuilder builder = b
                    .setApplicationName(LocalizedText.english(properties.getApplicationName()))
                    .setApplicationUri(properties.getApplicationUri())
                    .setIdentityProvider(identity)
                    .setCertificateValidator(certValidator)
                    .setRequestTimeout(Unsigned.uint(properties.getDefaultRequestTimeoutMillis()));
                return builder.build();
            }
        );

        client.connect().get(timeoutMs, TimeUnit.MILLISECONDS);
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
            }

            @Override
            public void onSessionInactive(UaSession session) {
                LOG.warn("OPC UA session inactive {}, tearing down client", key);
                synchronized (managed.lock) {
                    if (managed.client == c) {
                        teardownClient(key, managed);
                    }
                }
            }
        });
        managed.sessionListenerAttachedTo = c;
    }

    void teardownClient(ConnectionKey key, ManagedClient managed) {
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
        if (c != null) {
            try {
                c.disconnect().get(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("OPC UA disconnect interrupted", e);
            } catch (ExecutionException | TimeoutException e) {
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

    private static Optional<EndpointDescription> selectEndpoint(List<EndpointDescription> endpoints, SecurityPolicy policy) {
        if (endpoints == null || endpoints.isEmpty()) {
            return Optional.empty();
        }
        String uri = policy.getUri();
        Optional<EndpointDescription> match = endpoints.stream()
            .filter(e -> uri.equals(e.getSecurityPolicyUri()))
            .findFirst();
        return match.isPresent() ? match : endpoints.stream().findFirst();
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

    static final class ManagedClient {
        final Object lock = new Object();
        OpcUaClient client;
        long lastUsedAtMillis;
        OpcUaClient sessionListenerAttachedTo;
    }
}
