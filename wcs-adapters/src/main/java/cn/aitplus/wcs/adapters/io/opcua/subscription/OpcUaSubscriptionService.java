package cn.aitplus.wcs.adapters.io.opcua.subscription;

import cn.aitplus.wcs.adapters.io.connection.ConnectionKey;
import cn.aitplus.wcs.adapters.io.opcua.config.OpcUaAdapterProperties;
import cn.aitplus.wcs.adapters.io.opcua.session.OpcUaClientRegistry;
import cn.aitplus.wcs.adapters.io.opcua.session.OpcUaConnectionListener;
import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceEndpoint;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemServiceOperationResult;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * OPC UA MonitoredItem 订阅：与 {@link OpcUaClientRegistry} 共享会话，含 watch-dog、合并重建与状态清理。
 * 重建执行中（{@code rebuildRunning}）若再次请求重建，会置 {@code rebuildDirty}，本轮结束后补调度一次，避免注册集与服务端不一致。
 */
public final class OpcUaSubscriptionService implements OpcUaConnectionListener {

    private static final Logger LOG = LoggerFactory.getLogger(OpcUaSubscriptionService.class);

    private final OpcUaAdapterProperties properties;
    private final OpcUaClientRegistry registry;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskScheduler taskScheduler;
    private final Object eventSource;

    private final ConcurrentHashMap<String, Registration> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ConnectionKey, Set<String>> regIdsByKey = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ConnectionKey, SubscriptionState> stateByKey = new ConcurrentHashMap<>();

    private volatile boolean destroyed;

    public OpcUaSubscriptionService(OpcUaAdapterProperties properties,
                                    OpcUaClientRegistry registry,
                                    ApplicationEventPublisher eventPublisher,
                                    TaskScheduler taskScheduler) {
        this.properties = properties;
        this.registry = registry;
        this.eventPublisher = eventPublisher;
        this.taskScheduler = taskScheduler;
        this.eventSource = this;
    }

    public String register(Long warehouseId, String deviceId, DeviceEndpoint endpoint, Iterable<String> nodeIds) {
        if (!properties.isSubscriptionEnabled()) {
            throw new IllegalStateException("wcs.adapter.opcua.subscription-enabled=false");
        }
        if (warehouseId == null || !StringUtils.hasText(deviceId) || endpoint == null) {
            throw new IllegalArgumentException("warehouseId, deviceId, endpoint required");
        }
        if (!StringUtils.hasText(endpoint.getOpcEndpointUrl())) {
            throw new IllegalArgumentException("endpoint.opcEndpointUrl required");
        }
        Set<String> nodes = new LinkedHashSet<>();
        for (String nodeId : nodeIds) {
            if (StringUtils.hasText(nodeId)) {
                nodes.add(nodeId.trim());
            }
        }
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("nodeIds must not be empty");
        }

        ConnectionKey key = ConnectionKey.from(warehouseId, DomainEnums.CommandDomain.OPC, endpoint);
        String registrationId = UUID.randomUUID().toString();
        Registration reg = new Registration(warehouseId, deviceId, endpoint, nodes, key);
        byId.put(registrationId, reg);
        regIdsByKey.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(registrationId);
        scheduleRebuild(key, 150L, true, "register");
        return registrationId;
    }

    public void unregister(String registrationId) {
        if (registrationId == null) {
            return;
        }
        Registration reg = byId.remove(registrationId);
        if (reg == null) {
            return;
        }
        Set<String> ids = regIdsByKey.get(reg.key);
        if (ids != null) {
            ids.remove(registrationId);
            if (ids.isEmpty()) {
                regIdsByKey.remove(reg.key, ids);
            }
        }
        scheduleRebuild(reg.key, 150L, true, "unregister");
    }

    @Override
    public void onSessionActive(ConnectionKey key, OpcUaClient client) {
        SubscriptionState state = stateByKey.computeIfAbsent(key, k -> new SubscriptionState());
        synchronized (state.lock) {
            state.sessionInactive = false;
            state.markPublishActivity();
        }
        scheduleRebuild(key, 150L, true, "session-active");
    }

    @Override
    public void onSessionInactive(ConnectionKey key, OpcUaClient client) {
        SubscriptionState state = stateByKey.computeIfAbsent(key, k -> new SubscriptionState());
        synchronized (state.lock) {
            state.sessionInactive = true;
        }
    }

    @Override
    public void beforeClientTeardown(ConnectionKey key, OpcUaClient client) {
        SubscriptionState state = stateByKey.get(key);
        if (state != null) {
            synchronized (state.lock) {
                state.subscription = null;
                state.sessionInactive = true;
            }
        }
    }

    @Override
    public void afterClientTeardown(ConnectionKey key) {
        if (!destroyed && properties.isSubscriptionEnabled() && hasRegistrations(key)) {
            scheduleRebuild(key, 0L, false, "client-teardown");
        }
    }

    private void scheduleRebuild(ConnectionKey key, long requestedDelayMs, boolean resetBackoff, String reason) {
        if (destroyed || !properties.isSubscriptionEnabled()) {
            return;
        }
        SubscriptionState state = stateByKey.computeIfAbsent(key, k -> new SubscriptionState());
        synchronized (state.lock) {
            if (resetBackoff) {
                state.consecutiveFailures = 0;
            }
            if (state.rebuildRunning) {
                state.rebuildDirty = true;
                return;
            }
            if (state.pendingRebuild != null) {
                return;
            }
            long delay = requestedDelayMs > 0 ? requestedDelayMs : computeBackoffDelayMillis(state.consecutiveFailures);
            state.lastRebuildReason = reason;
            state.pendingRebuild = taskScheduler.schedule(() -> runRebuild(key), Instant.now().plusMillis(delay));
        }
    }

    private void runRebuild(ConnectionKey key) {
        SubscriptionState state = stateByKey.computeIfAbsent(key, k -> new SubscriptionState());
        synchronized (state.lock) {
            state.pendingRebuild = null;
            if (state.rebuildRunning || destroyed) {
                return;
            }
            state.rebuildRunning = true;
        }
        boolean scheduleCoalesce = false;
        try {
            rebuildForKey(key, state);
        } finally {
            synchronized (state.lock) {
                state.rebuildRunning = false;
                if (state.rebuildDirty) {
                    state.rebuildDirty = false;
                    scheduleCoalesce = true;
                }
            }
            cleanupStateIfIdle(key, state);
            if (scheduleCoalesce && !destroyed && properties.isSubscriptionEnabled() && hasRegistrations(key)) {
                scheduleRebuild(key, 0L, false, "coalesce");
            }
        }
    }

    private void rebuildForKey(ConnectionKey key, SubscriptionState state) {
        try {
            Set<String> ids = regIdsByKey.get(key);
            if (ids == null || ids.isEmpty()) {
                synchronized (state.lock) {
                    deleteSubscriptionQuietly(key, state);
                }
                stateByKey.remove(key, state);
                return;
            }

            List<Registration> registrations = loadRegistrations(ids);
            if (registrations.isEmpty()) {
                return;
            }

            DeviceEndpoint endpoint = registrations.get(0).endpoint;
            registry.executeWithClient(key, endpoint, client -> {
                synchronized (state.lock) {
                    rebuildSubscriptionOnClient(key, state, client, registrations);
                    state.consecutiveFailures = 0;
                    state.sessionInactive = false;
                    state.markPublishActivity();
                }
                return null;
            });
        } catch (Exception e) {
            synchronized (state.lock) {
                state.consecutiveFailures++;
            }
            LOG.warn("OPC UA subscription rebuild failed for {} reason={}", key, state.lastRebuildReason, e);
            if (!destroyed && hasRegistrations(key)) {
                scheduleRebuild(key, 0L, false, "retry");
            }
        }
    }

    private List<Registration> loadRegistrations(Set<String> ids) {
        List<Registration> registrations = new ArrayList<>();
        for (String id : ids) {
            Registration reg = byId.get(id);
            if (reg != null) {
                registrations.add(reg);
            }
        }
        return registrations;
    }

    private void rebuildSubscriptionOnClient(ConnectionKey key,
                                             SubscriptionState state,
                                             OpcUaClient client,
                                             List<Registration> registrations) throws Exception {
        deleteSubscriptionQuietly(key, state);

        Map<String, List<RoutingTarget>> routing = buildRouting(registrations);
        OpcUaSubscription subscription = new OpcUaSubscription(client, properties.getSubscriptionPublishingIntervalMillis());
        long targetKeepAliveMs = properties.getSubscriptionWatchdogIntervalMillis();
        if (targetKeepAliveMs > 0L) {
            subscription.setTargetKeepAliveInterval((double) targetKeepAliveMs);
        } else {
            subscription.setMaxKeepAliveCount(Unsigned.uint(properties.getSubscriptionMaxKeepAliveCount()));
            subscription.setLifetimeCount(Unsigned.uint(properties.getSubscriptionLifetimeCount()));
        }
        subscription.setMaxNotificationsPerPublish(Unsigned.uint(properties.getSubscriptionMaxNotificationsPerPublish()));
        subscription.setPriority(Unsigned.ubyte(properties.getSubscriptionPriority()));
        subscription.setWatchdogMultiplier((double) Math.max(2L, properties.getSubscriptionWatchdogSilenceMultiplier()));
        subscription.setSubscriptionListener(new OpcUaSubscription.SubscriptionListener() {
            @Override
            public void onDataReceived(OpcUaSubscription sub,
                                       List<OpcUaMonitoredItem> items,
                                       List<DataValue> values) {
                markPublishActivity(state);
            }

            @Override
            public void onKeepAliveReceived(OpcUaSubscription sub) {
                markPublishActivity(state);
            }

            @Override
            public void onNotificationDataLost(OpcUaSubscription sub) {
                LOG.warn("OPC UA notification data lost {}", key);
                scheduleRebuild(key, 0L, false, "notification-data-lost");
            }

            @Override
            public void onWatchdogTimerElapsed(OpcUaSubscription sub) {
                LOG.warn("OPC UA subscription watchdog elapsed {}", key);
                scheduleRebuild(key, 0L, false, "watchdog");
            }

            @Override
            public void onStatusChanged(OpcUaSubscription sub, StatusCode statusCode) {
                markPublishActivity(state);
                if (statusCode != null && !statusCode.isGood()) {
                    LOG.warn("OPC UA subscription status changed {}: {}", key, statusCode);
                }
            }

            @Override
            public void onTransferFailed(OpcUaSubscription sub, StatusCode statusCode) {
                LOG.warn("OPC UA subscription transfer failed {}: {}", key, statusCode);
                scheduleRebuild(key, 0L, false, "transfer-failed");
            }
        });

        subscription.create();
        state.subscription = subscription;
        state.markPublishActivity();

        List<OpcUaMonitoredItem> items = new ArrayList<>();
        for (Map.Entry<String, List<RoutingTarget>> entry : routing.entrySet()) {
            NodeId nodeId = NodeId.parse(entry.getKey());
            OpcUaMonitoredItem item = OpcUaMonitoredItem.newDataItem(nodeId, MonitoringMode.Reporting);
            item.setSamplingInterval(properties.getSubscriptionPublishingIntervalMillis());
            item.setQueueSize(Unsigned.uint(256));
            item.setDiscardOldest(true);
            item.setUserObject(entry.getKey());
            item.setDataValueListener((OpcUaMonitoredItem monitoredItem, DataValue value) ->
                deliver(state, (String) monitoredItem.getUserObject().orElse(entry.getKey()), routing, value));
            items.add(item);
        }

        subscription.addMonitoredItems(items);
        List<MonitoredItemServiceOperationResult> results = subscription.createMonitoredItems();
        for (MonitoredItemServiceOperationResult result : results) {
            if (!result.isGood()) {
                throw new IllegalStateException("CreateMonitoredItems failed: " + result.serviceResult());
            }
        }

        LOG.info("OPC UA subscription rebuilt for {} with {} monitored items", key, items.size());
    }

    private void deliver(SubscriptionState state, String norm, Map<String, List<RoutingTarget>> routing, DataValue dv) {
        markPublishActivity(state);
        if (dv == null || dv.getStatusCode() == null || !dv.getStatusCode().isGood()) {
            return;
        }
        if (dv.getValue() == null || dv.getValue().isNull()) {
            return;
        }
        Object value = dv.getValue().getValue();
        List<RoutingTarget> targets = routing.get(norm);
        if (targets == null) {
            return;
        }
        for (RoutingTarget target : targets) {
            eventPublisher.publishEvent(new OpcUaSubscriptionNotificationEvent(
                eventSource, target.warehouseId, target.deviceId, target.originalNodeIdString, value));
        }
    }

    private void deleteSubscriptionQuietly(ConnectionKey key, SubscriptionState state) {
        if (state.subscription == null) {
            return;
        }
        OpcUaSubscription subscription = state.subscription;
        state.subscription = null;
        try {
            subscription.delete();
        } catch (Exception e) {
            LOG.debug("deleteSubscription {}: {}", key, e.getMessage());
        }
    }

    private boolean hasRegistrations(ConnectionKey key) {
        Set<String> ids = regIdsByKey.get(key);
        return ids != null && !ids.isEmpty();
    }

    private void markPublishActivity(SubscriptionState state) {
        synchronized (state.lock) {
            state.markPublishActivity();
        }
    }

    private long computeBackoffDelayMillis(long failures) {
        long initial = Math.max(100L, properties.getRebuildInitialDelayMillis());
        long max = Math.max(initial, properties.getRebuildMaxDelayMillis());
        long delay = initial;
        for (long i = 0; i < failures; i++) {
            if (delay >= max / 2L) {
                return max;
            }
            delay *= 2L;
        }
        return Math.min(delay, max);
    }

    private void cleanupStateIfIdle(ConnectionKey key, SubscriptionState state) {
        synchronized (state.lock) {
            if (state.subscription != null) {
                return;
            }
            if (state.pendingRebuild != null || state.rebuildRunning) {
                return;
            }
            if (hasRegistrations(key)) {
                return;
            }
        }
        stateByKey.remove(key, state);
    }

    private static Map<String, List<RoutingTarget>> buildRouting(List<Registration> registrations) {
        Map<String, List<RoutingTarget>> routing = new LinkedHashMap<>();
        for (Registration reg : registrations) {
            for (String raw : reg.nodeIds) {
                NodeId nodeId = NodeId.parse(raw);
                routing.computeIfAbsent(nodeId.toParseableString(), k -> new ArrayList<>())
                    .add(new RoutingTarget(reg.warehouseId, reg.deviceId, raw));
            }
        }
        return routing;
    }

    public void destroy() {
        destroyed = true;
        for (Map.Entry<ConnectionKey, SubscriptionState> entry : stateByKey.entrySet()) {
            SubscriptionState state = entry.getValue();
            synchronized (state.lock) {
                if (state.pendingRebuild != null) {
                    state.pendingRebuild.cancel(false);
                    state.pendingRebuild = null;
                }
                deleteSubscriptionQuietly(entry.getKey(), state);
            }
        }
        byId.clear();
        regIdsByKey.clear();
        stateByKey.clear();
    }

    private static final class Registration {
        final Long warehouseId;
        final String deviceId;
        final DeviceEndpoint endpoint;
        final Set<String> nodeIds;
        final ConnectionKey key;

        private Registration(Long warehouseId, String deviceId, DeviceEndpoint endpoint, Set<String> nodeIds, ConnectionKey key) {
            this.warehouseId = warehouseId;
            this.deviceId = deviceId;
            this.endpoint = endpoint;
            this.nodeIds = nodeIds;
            this.key = key;
        }
    }

    private static final class RoutingTarget {
        final Long warehouseId;
        final String deviceId;
        final String originalNodeIdString;

        private RoutingTarget(Long warehouseId, String deviceId, String originalNodeIdString) {
            this.warehouseId = warehouseId;
            this.deviceId = deviceId;
            this.originalNodeIdString = originalNodeIdString;
        }
    }

    private static final class SubscriptionState {
        final Object lock = new Object();
        OpcUaSubscription subscription;
        ScheduledFuture<?> pendingRebuild;
        boolean rebuildRunning;
        /** 重建执行期间有新的 register/unregister 等请求时置位，本轮结束后补一次 scheduleRebuild */
        boolean rebuildDirty;
        boolean sessionInactive;
        long lastPublishActivityAtMillis = System.currentTimeMillis();
        long consecutiveFailures;
        String lastRebuildReason = "init";

        void markPublishActivity() {
            lastPublishActivityAtMillis = System.currentTimeMillis();
        }
    }
}
