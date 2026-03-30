package cn.aitplus.wcs.adapters.io.opcua.subscription;

import cn.aitplus.wcs.adapters.io.connection.ConnectionKey;
import cn.aitplus.wcs.adapters.io.opcua.config.OpcUaAdapterProperties;
import cn.aitplus.wcs.adapters.io.opcua.session.OpcUaClientRegistry;
import cn.aitplus.wcs.adapters.io.opcua.session.OpcUaConnectionListener;
import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceEndpoint;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OPC UA MonitoredItem 订阅：与 {@link OpcUaClientRegistry} 共享会话；
 * 连接拆除或 Publish 异常时重建订阅；每条数据通知发布 {@link OpcUaSubscriptionNotificationEvent}。
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
    private final ConcurrentHashMap<ConnectionKey, Object> rebuildLocks = new ConcurrentHashMap<>();
    private final AtomicLong clientHandleSeq = new AtomicLong(1);

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

    /**
     * 注册一组 NodeId；同一 endpoint+仓 下多设备可多次注册。返回 registrationId，{@link #unregister(String)} 注销。
     */
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
        for (String n : nodeIds) {
            if (StringUtils.hasText(n)) {
                nodes.add(n.trim());
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
        scheduleRebuild(key);
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
        Set<String> set = regIdsByKey.get(reg.key);
        if (set != null) {
            set.remove(registrationId);
            if (set.isEmpty()) {
                regIdsByKey.remove(reg.key, set);
            }
        }
        scheduleRebuild(reg.key);
    }

    @Override
    public void onClientConnected(ConnectionKey key, OpcUaClient client) {
        if (!properties.isSubscriptionEnabled() || destroyed) {
            return;
        }
        client.getSubscriptionManager().addSubscriptionListener(new UaSubscriptionManager.SubscriptionListener() {
            @Override
            public void onPublishFailure(UaException exception) {
                LOG.warn("OPC UA publish failure {}: {}", key, exception.getMessage());
                scheduleRebuild(key);
            }

            @Override
            public void onSubscriptionTransferFailed(UaSubscription subscription, StatusCode statusCode) {
                LOG.warn("OPC UA subscription transfer failed {}: {}", key, statusCode);
                scheduleRebuild(key);
            }
        });
    }

    @Override
    public void beforeClientTeardown(ConnectionKey key, OpcUaClient client) {
        SubscriptionState st = stateByKey.get(key);
        if (st != null) {
            st.subscription = null;
        }
    }

    @Override
    public void afterClientTeardown(ConnectionKey key) {
        if (properties.isSubscriptionEnabled() && !destroyed) {
            scheduleRebuild(key);
        }
    }

    private void scheduleRebuild(ConnectionKey key) {
        if (!properties.isSubscriptionEnabled() || destroyed) {
            return;
        }
        taskScheduler.schedule(() -> rebuildForKey(key), Instant.now().plusMillis(150));
    }

    private void rebuildForKey(ConnectionKey key) {
        Object lk = rebuildLocks.computeIfAbsent(key, k -> new Object());
        synchronized (lk) {
            try {
                Set<String> ids = regIdsByKey.get(key);
                if (ids == null || ids.isEmpty()) {
                    deleteSubscriptionQuietly(key, null);
                    stateByKey.remove(key);
                    return;
                }
                List<Registration> regs = loadRegistrations(ids);
                if (regs.isEmpty()) {
                    return;
                }
                DeviceEndpoint endpoint = regs.get(0).endpoint;
                long timeout = properties.getDefaultRequestTimeoutMillis();
                registry.executeWithClient(key, endpoint, timeout, client -> {
                    rebuildSubscriptionOnClient(key, client, regs, timeout);
                    return null;
                });
            } catch (Exception e) {
                LOG.warn("OPC UA subscription rebuild failed for {}", key, e);
            }
        }
    }

    private List<Registration> loadRegistrations(Set<String> ids) {
        List<Registration> regs = new ArrayList<>();
        for (String id : ids) {
            Registration r = byId.get(id);
            if (r != null) {
                regs.add(r);
            }
        }
        return regs;
    }

    private void rebuildSubscriptionOnClient(ConnectionKey key, OpcUaClient client, List<Registration> regs, long timeoutMs)
        throws Exception {
        SubscriptionState st = stateByKey.computeIfAbsent(key, k -> new SubscriptionState());
        deleteSubscriptionQuietly(key, client);

        UaSubscription sub = client.getSubscriptionManager()
            .createSubscription(properties.getSubscriptionPublishingIntervalMillis())
            .get(timeoutMs, TimeUnit.MILLISECONDS);
        st.subscription = sub;

        Map<String, List<RoutingTarget>> routing = buildRouting(regs);
        List<MonitoredItemCreateRequest> allRequests = new ArrayList<>();
        for (Map.Entry<String, List<RoutingTarget>> e : routing.entrySet()) {
            NodeId nodeId = NodeId.parse(e.getKey());
            long handle = clientHandleSeq.getAndIncrement();
            ReadValueId rv = new ReadValueId(nodeId, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE);
            MonitoringParameters params = new MonitoringParameters(
                Unsigned.uint(handle),
                properties.getSubscriptionPublishingIntervalMillis(),
                null,
                Unsigned.uint(256),
                true
            );
            allRequests.add(new MonitoredItemCreateRequest(rv, MonitoringMode.Reporting, params));
        }

        int batch = Math.max(1, properties.getMaxMonitoredItemsPerCreateBatch());
        for (int i = 0; i < allRequests.size(); i += batch) {
            List<MonitoredItemCreateRequest> chunk = allRequests.subList(i, Math.min(i + batch, allRequests.size()));
            List<MonitoredItemCreateRequest> chunkCopy = new ArrayList<>(chunk);
            sub.createMonitoredItems(TimestampsToReturn.Both, chunkCopy, (item, idx) -> {
                ReadValueId rv = (ReadValueId) chunkCopy.get(idx).getItemToMonitor();
                NodeId nid = rv.getNodeId();
                String norm = nid.toParseableString();
                item.setValueConsumer((UaMonitoredItem mi, DataValue dv) -> deliver(norm, routing, dv));
            }).get(timeoutMs, TimeUnit.MILLISECONDS);
        }
        LOG.info("OPC UA subscription rebuilt for {} with {} monitored items", key, allRequests.size());
    }

    private void deliver(String norm, Map<String, List<RoutingTarget>> routing, DataValue dv) {
        if (dv == null || dv.getStatusCode() == null || !dv.getStatusCode().isGood()) {
            return;
        }
        if (dv.getValue() == null || dv.getValue().isNull()) {
            return;
        }
        Object val = dv.getValue().getValue();
        List<RoutingTarget> targets = routing.get(norm);
        if (targets == null) {
            return;
        }
        for (RoutingTarget t : targets) {
            eventPublisher.publishEvent(new OpcUaSubscriptionNotificationEvent(
                eventSource, t.warehouseId, t.deviceId, t.originalNodeIdString, val));
        }
    }

    private void deleteSubscriptionQuietly(ConnectionKey key, OpcUaClient clientOrNull) {
        SubscriptionState st = stateByKey.get(key);
        if (st == null || st.subscription == null) {
            return;
        }
        UaSubscription sub = st.subscription;
        st.subscription = null;
        OpcUaClient client = clientOrNull != null ? clientOrNull : registry.peekClient(key);
        if (client == null) {
            return;
        }
        long timeout = properties.getDefaultRequestTimeoutMillis();
        try {
            client.getSubscriptionManager().deleteSubscription(sub.getSubscriptionId()).get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            LOG.debug("deleteSubscription timeout {}", key);
        } catch (Exception e) {
            LOG.debug("deleteSubscription {}: {}", key, e.getMessage());
        }
    }

    private static Map<String, List<RoutingTarget>> buildRouting(List<Registration> regs) {
        Map<String, List<RoutingTarget>> routing = new LinkedHashMap<>();
        for (Registration r : regs) {
            for (String raw : r.nodeIds) {
                NodeId nid = NodeId.parse(raw);
                String norm = nid.toParseableString();
                routing.computeIfAbsent(norm, k -> new ArrayList<>())
                    .add(new RoutingTarget(r.warehouseId, r.deviceId, raw));
            }
        }
        return routing;
    }

    public void destroy() {
        destroyed = true;
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

        Registration(Long warehouseId, String deviceId, DeviceEndpoint endpoint, Set<String> nodeIds, ConnectionKey key) {
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

        RoutingTarget(Long warehouseId, String deviceId, String originalNodeIdString) {
            this.warehouseId = warehouseId;
            this.deviceId = deviceId;
            this.originalNodeIdString = originalNodeIdString;
        }
    }

    private static final class SubscriptionState {
        volatile UaSubscription subscription;
    }
}
