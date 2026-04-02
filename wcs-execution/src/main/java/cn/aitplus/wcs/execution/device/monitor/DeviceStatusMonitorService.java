package cn.aitplus.wcs.execution.device.monitor;

import cn.aitplus.wcs.adapters.io.connection.ConnectionKey;
import cn.aitplus.wcs.adapters.io.registry.DeviceTransportRegistry;
import cn.aitplus.wcs.core.domain.event.DeviceAlarmChangedEvent;
import cn.aitplus.wcs.core.domain.event.DeviceOnlineChangedEvent;
import cn.aitplus.wcs.core.domain.event.DevicePointValueChangedEvent;
import cn.aitplus.wcs.core.domain.model.device.DevicePointReadResult;
import cn.aitplus.wcs.core.domain.model.device.DevicePointValue;
import cn.aitplus.wcs.core.domain.model.device.DevicePointsConfig;
import cn.aitplus.wcs.core.spi.device.DeviceIoItem;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.execution.device.io.runtime.DeviceIoReadResultParser;
import cn.aitplus.wcs.execution.device.io.runtime.DevicePointValueMapper;
import cn.aitplus.wcs.execution.device.io.runtime.model.ResolvedDevicePoint;
import cn.aitplus.wcs.infra.service.device.DeviceConfigRedisScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 设备状态监控核心服务。
 * <ul>
 *   <li>启动时构建监控索引（从 Redis 配置）</li>
 *   <li>每轮 poll 两阶段流水线：并行读取 → 串行 diff + 事件</li>
 *   <li>维护内存快照，暴露查询接口</li>
 *   <li>支持动态监控点注册/注销（引用计数）</li>
 * </ul>
 */
@Service
public class DeviceStatusMonitorService {

    private static final Logger log = LoggerFactory.getLogger(DeviceStatusMonitorService.class);

    private final DeviceMonitorDefinitionBuilder definitionBuilder;
    private final DeviceTransportRegistry transportRegistry;
    private final DevicePointValueMapper devicePointValueMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final DeviceMonitorProperties properties;
    private final DeviceConfigRedisScanner configScanner;
    private final DeviceIoReadResultParser readResultParser;

    private volatile MonitorIndex currentIndex;
    private final ConcurrentHashMap<String, DevicePointReadResult> lastSnapshots = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> lastDynamicPointValues = new ConcurrentHashMap<>();

    /** 心跳写入相位：ConnectionKey → 递增计数（奇偶决定写 0/1） */
    private final ConcurrentHashMap<ConnectionKey, Integer> heartbeatPhase = new ConcurrentHashMap<>();

    private ExecutorService ioExecutor;

    public DeviceStatusMonitorService(DeviceMonitorDefinitionBuilder definitionBuilder,
                                      DeviceTransportRegistry transportRegistry,
                                      DevicePointValueMapper devicePointValueMapper,
                                      ApplicationEventPublisher eventPublisher,
                                      DeviceMonitorProperties properties,
                                      DeviceConfigRedisScanner configScanner,
                                      DeviceIoReadResultParser readResultParser) {
        this.definitionBuilder = definitionBuilder;
        this.transportRegistry = transportRegistry;
        this.devicePointValueMapper = devicePointValueMapper;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.configScanner = configScanner;
        this.readResultParser = readResultParser;
    }

    public void initialize() {
        this.currentIndex = definitionBuilder.buildIndex();
        this.ioExecutor = Executors.newFixedThreadPool(properties.getIoPoolSize(), r -> {
            Thread t = new Thread(r, "wcs-monitor-io");
            t.setDaemon(true);
            return t;
        });

        for (DeviceMonitorDefinition def : currentIndex.getDefinitionsByDeviceId().values()) {
            if (!def.isPollingEnabled()) {
                DevicePointReadResult snapshot = DevicePointReadResult.builder()
                        .success(true)
                        .errorCode(null)
                        .errorMessage(null)
                        .deviceId(def.getDeviceId())
                        .deviceName(def.getDeviceName())
                        .warehouseId(def.getWarehouseId())
                        .protocolType(def.getProtocolType())
                        .rawResponseJson(null)
                        .items(List.of())
                        .updatedAt(Instant.now())
                        .build();
                lastSnapshots.put(def.getDeviceId(), snapshot);
            }
        }

        log.info("DeviceStatusMonitorService 初始化完成，IO 线程池大小={}", properties.getIoPoolSize());
    }

    // ==================== 轮询入口 ====================

    public void poll() {
        MonitorIndex index = currentIndex;
        if (index == null) {
            return;
        }

        // 阶段 ①：并行读取（每个 ConnectionKey 一个 Future）
        long timeout = properties.getPollIntervalMillis() * 80 / 100;
        Map<ConnectionKey, List<DeviceMonitorDefinition>> groups = index.getGroups();
        Map<ConnectionKey, CompletableFuture<GroupReadResult>> futures = new LinkedHashMap<>();

        for (Map.Entry<ConnectionKey, List<DeviceMonitorDefinition>> entry : groups.entrySet()) {
            ConnectionKey ck = entry.getKey();
            List<DeviceMonitorDefinition> definitions = entry.getValue();
            futures.put(ck, CompletableFuture.supplyAsync(() -> readGroup(ck, definitions), ioExecutor));
        }

        Map<ConnectionKey, GroupReadResult> results = new LinkedHashMap<>();
        for (Map.Entry<ConnectionKey, CompletableFuture<GroupReadResult>> entry : futures.entrySet()) {
            try {
                results.put(entry.getKey(), entry.getValue().get(timeout, TimeUnit.MILLISECONDS));
            } catch (Exception ex) {
                log.debug("读取超时或异常 connectionKey={}", entry.getKey(), ex);
                results.put(entry.getKey(), new GroupReadResult(entry.getKey(), null));
            }
        }

        // 阶段 ②：串行 diff + 事件（调度线程）
        for (DeviceMonitorDefinition def : index.getDefinitionsByDeviceId().values()) {
            if (!def.isPollingEnabled()) {
                continue;
            }
            try {
                processDeviceResult(def, results.get(def.getConnectionKey()));
            } catch (Exception ex) {
                log.warn("处理设备结果异常 deviceId={}", def.getDeviceId(), ex);
            }
        }
    }

    // ==================== 单组读取（IO 线程） ====================

    private GroupReadResult readGroup(ConnectionKey connectionKey, List<DeviceMonitorDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            log.info("监控轮询跳过 connectionKey={} reason=no_definitions", connectionKey);
            return new GroupReadResult(connectionKey, null);
        }
        DeviceMonitorDefinition sample = definitions.get(0);
        String deviceIds = definitions.stream()
                .map(DeviceMonitorDefinition::getDeviceId)
                .collect(Collectors.joining(","));
        String endpointLabel = resolveEndpointLabel(sample);

        // 1. 汇总所有设备的 effectivePoints 物理地址（去重）
        Set<String> uniqueAddresses = new LinkedHashSet<>();
        for (DeviceMonitorDefinition def : definitions) {
            for (ResolvedDevicePoint point : def.getEffectivePoints().values()) {
                uniqueAddresses.add(point.getPhysicalAddress());
            }
        }

        if (uniqueAddresses.isEmpty()) {
            log.info("监控轮询跳过 endpoint={} connectionKey={} devices={} reason=no_physical_addresses",
                    endpointLabel, connectionKey, deviceIds);
            return new GroupReadResult(connectionKey, null);
        }

        // 2. 构建批量读请求
        List<DeviceIoItem> readItems = uniqueAddresses.stream()
                .map(addr -> DeviceIoItem.builder().address(addr).write(false).build())
                .collect(Collectors.toList());

        DeviceIoRequest readReq = DeviceIoRequest.builder()
                .domain(sample.getDomain())
                .endpoint(sample.getEndpoint())
                .warehouseId(sample.getWarehouseId())
                .items(readItems)
                .build();

        // 3. executeOnce
        DeviceIoResult result = transportRegistry.executeOnce(readReq);
        if (result.isSuccess()) {
            log.info("监控轮询成功 endpoint={} connectionKey={} devices={} response={}",
                    endpointLabel, connectionKey, deviceIds, result.getResponseJson());
        } else {
            log.warn("监控轮询失败 endpoint={} connectionKey={} devices={} errorCode={} errorMessage={}",
                    endpointLabel, connectionKey, deviceIds, result.getErrorCode(), result.getErrorMessage());
        }

        // 4. 读成功后写心跳
        if (result.isSuccess() && StringUtils.hasText(properties.getHeartbeatWritePointId())) {
            writeHeartbeat(connectionKey, definitions);
        }

        return new GroupReadResult(connectionKey, result);
    }

    private void writeHeartbeat(ConnectionKey connectionKey, List<DeviceMonitorDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return;
        }
        DeviceMonitorDefinition sample = definitions.get(0);
        String hbPointId = properties.getHeartbeatWritePointId();
        for (DeviceMonitorDefinition def : definitions) {
            ResolvedDevicePoint hbPoint = def.getEffectivePoints().get(hbPointId);
            if (hbPoint == null) {
                continue;
            }
            Integer phase = heartbeatPhase.merge(connectionKey, 1, (left, right) -> left + right);
            boolean bit = phase != null && (phase & 1) == 1;

            DeviceIoRequest writeReq = DeviceIoRequest.builder()
                    .domain(sample.getDomain())
                    .endpoint(sample.getEndpoint())
                    .warehouseId(sample.getWarehouseId())
                    .items(List.of(DeviceIoItem.builder()
                            .address(hbPoint.getPhysicalAddress())
                            .value(bit)
                            .write(true)
                            .build()))
                    .build();
            try {
                DeviceIoResult writeResult = transportRegistry.executeOnce(writeReq);
                if (!writeResult.isSuccess()) {
                    log.debug("心跳写入失败 connectionKey={} error={}", connectionKey, writeResult.getErrorMessage());
                }
            } catch (Exception ex) {
                log.debug("心跳写入异常 connectionKey={}", connectionKey, ex);
            }
            return;
        }
    }

    // ==================== 设备级结果处理（调度线程） ====================

    private void processDeviceResult(DeviceMonitorDefinition def, GroupReadResult groupResult) {
        String deviceId = def.getDeviceId();
        DeviceIoResult ioResult = groupResult == null ? null : groupResult.getResult();
        boolean online = ioResult != null && ioResult.isSuccess();

        Map<String, Object> rawValues = Collections.emptyMap();
        if (online && ioResult != null) {
            rawValues = readResultParser.parseReadValues(ioResult.getResponseJson());
        }

        // 对每个监控点取值、转换、判告警
        List<DevicePointValue> pointValues = new ArrayList<>();
        Map<String, ResolvedDevicePoint> effectivePoints = def.getEffectivePoints();

        for (Map.Entry<String, ResolvedDevicePoint> entry : effectivePoints.entrySet()) {
            String pointId = entry.getKey();
            ResolvedDevicePoint point = entry.getValue();
            Object rawValue = rawValues.get(point.getPhysicalAddress());
            DevicePointValue pointValue = devicePointValueMapper.toPointValue(def.getDomain(), point, rawValue);
            pointValues.add(pointValue);
            Object convertedValue = pointValue.getRawValue();

            // 默认监控点 → 参与 alarm 聚合
            if (convertedValue == null) {
                continue;
            }

            // 动态附加点 → 值变化事件
            if (def.getDynamicPointSources().containsKey(pointId)) {
                String cacheKey = deviceId + ":" + pointId;
                Object oldValue = lastDynamicPointValues.get(cacheKey);
                if (!Objects.equals(oldValue, convertedValue)) {
                    lastDynamicPointValues.put(cacheKey, convertedValue);
                    DevicePointValueChangedEvent pointChangedEvent = DevicePointValueChangedEvent.builder()
                            .deviceId(deviceId)
                            .pointId(pointId)
                            .oldValue(oldValue)
                            .newValue(convertedValue)
                            .timestamp(Instant.now())
                            .build();
                    eventPublisher.publishEvent(Objects.requireNonNull(pointChangedEvent, "pointChangedEvent"));
                }
            }
        }

        // 构建新快照
        DevicePointReadResult newSnapshot = DevicePointReadResult.builder()
                .success(online)
                .errorCode(ioResult == null ? "UNKNOWN" : ioResult.getErrorCode())
                .errorMessage(ioResult == null ? "设备监控结果为空" : ioResult.getErrorMessage())
                .deviceId(deviceId)
                .deviceName(def.getDeviceName())
                .warehouseId(def.getWarehouseId())
                .protocolType(def.getProtocolType())
                .rawResponseJson(ioResult == null ? null : ioResult.getResponseJson())
                .items(pointValues)
                .updatedAt(Instant.now())
                .build();

        DevicePointReadResult oldSnapshot = lastSnapshots.put(deviceId, newSnapshot);
        boolean oldOnline = isOnline(oldSnapshot);
        boolean newOnline = isOnline(newSnapshot);
        Set<String> oldAlarmPointIds = resolveAlarmPointIds(def, oldSnapshot);
        Set<String> newAlarmPointIds = resolveAlarmPointIds(def, newSnapshot);
        boolean oldAlarm = !oldAlarmPointIds.isEmpty();
        boolean newAlarm = !newAlarmPointIds.isEmpty();

        // diff → 设备级事件
        if (oldSnapshot == null || oldOnline != newOnline) {
            DeviceOnlineChangedEvent onlineChangedEvent = DeviceOnlineChangedEvent.builder()
                    .deviceId(deviceId)
                    .deviceName(def.getDeviceName())
                    .warehouseId(def.getWarehouseId())
                    .protocolType(def.getProtocolType())
                    .online(newOnline)
                    .timestamp(Instant.now())
                    .build();
            eventPublisher.publishEvent(Objects.requireNonNull(onlineChangedEvent, "onlineChangedEvent"));
        }

        if (oldSnapshot == null || oldAlarm != newAlarm
                || !Objects.equals(oldAlarmPointIds, newAlarmPointIds)) {
            DeviceAlarmChangedEvent alarmChangedEvent = DeviceAlarmChangedEvent.builder()
                    .deviceId(deviceId)
                    .deviceName(def.getDeviceName())
                    .warehouseId(def.getWarehouseId())
                    .protocolType(def.getProtocolType())
                    .alarm(newAlarm)
                    .alarmPointIds(newAlarmPointIds)
                    .timestamp(Instant.now())
                    .build();
            eventPublisher.publishEvent(Objects.requireNonNull(alarmChangedEvent, "alarmChangedEvent"));
        }
    }

    // ==================== 动态监控点注册/注销 ====================

    public void registerMonitorPoints(String deviceId, Set<String> pointIds, String source) {
        MonitorIndex index = currentIndex;
        if (index == null) {
            return;
        }
        DeviceMonitorDefinition def = index.getDefinitionsByDeviceId().get(deviceId);
        if (def == null) {
            log.warn("动态注册失败：设备 {} 不在监控索引中", deviceId);
            return;
        }

        Map<String, DevicePointsConfig> allPointsConfigs = configScanner.findAllDevicePointsConfigs();
        for (String pointId : pointIds) {
            def.getDynamicPointSources()
                    .computeIfAbsent(pointId, k -> ConcurrentHashMap.newKeySet())
                    .add(source);

            if (!def.getDefaultPoints().containsKey(pointId)) {
                ResolvedDevicePoint resolved = definitionBuilder.resolvePoint(
                        deviceId, pointId, def.getDomain(), allPointsConfigs);
                if (resolved == null) {
                    log.debug("动态注册点位未找到定义 deviceId={} pointId={}", deviceId, pointId);
                }
            }
        }

        rebuildDynamicEffectivePoints(def, allPointsConfigs);
        log.info("动态注册完成 deviceId={} pointIds={} source={}", deviceId, pointIds, source);
    }

    public void unregisterMonitorPoints(String deviceId, Set<String> pointIds, String source) {
        MonitorIndex index = currentIndex;
        if (index == null) {
            return;
        }
        DeviceMonitorDefinition def = index.getDefinitionsByDeviceId().get(deviceId);
        if (def == null) {
            return;
        }

        for (String pointId : pointIds) {
            Set<String> sources = def.getDynamicPointSources().get(pointId);
            if (sources != null) {
                sources.remove(source);
                if (sources.isEmpty()) {
                    def.getDynamicPointSources().remove(pointId);
                    lastDynamicPointValues.remove(deviceId + ":" + pointId);
                }
            }
        }

        Map<String, DevicePointsConfig> allPointsConfigs = configScanner.findAllDevicePointsConfigs();
        rebuildDynamicEffectivePoints(def, allPointsConfigs);
        log.info("动态注销完成 deviceId={} pointIds={} source={}", deviceId, pointIds, source);
    }

    private void rebuildDynamicEffectivePoints(DeviceMonitorDefinition def,
                                               Map<String, DevicePointsConfig> allPointsConfigs) {
        Map<String, ResolvedDevicePoint> resolvedDynamic = new LinkedHashMap<>();
        for (String pointId : def.getDynamicPointSources().keySet()) {
            if (def.getDefaultPoints().containsKey(pointId)) {
                continue;
            }
            ResolvedDevicePoint resolved = definitionBuilder.resolvePoint(
                    def.getDeviceId(), pointId, def.getDomain(), allPointsConfigs);
            if (resolved != null) {
                resolvedDynamic.put(pointId, resolved);
            }
        }
        def.refreshEffectivePoints(resolvedDynamic);
    }

    // ==================== 查询接口 ====================

    public boolean isDeviceOnline(String deviceId) {
        return isOnline(lastSnapshots.get(deviceId));
    }

    public boolean isDeviceAlarm(String deviceId) {
        DevicePointReadResult snapshot = lastSnapshots.get(deviceId);
        DeviceMonitorDefinition def = currentIndex == null ? null : currentIndex.getDefinitionsByDeviceId().get(deviceId);
        return def != null && !resolveAlarmPointIds(def, snapshot).isEmpty();
    }

    public DevicePointReadResult getSnapshot(String deviceId) {
        return lastSnapshots.get(deviceId);
    }

    public List<DevicePointReadResult> getAllSnapshots() {
        return new ArrayList<>(lastSnapshots.values());
    }

    private String resolveEndpointLabel(DeviceMonitorDefinition definition) {
        if (definition == null || definition.getEndpoint() == null) {
            return "unknown";
        }
        if (StringUtils.hasText(definition.getEndpoint().getHost())) {
            return definition.getEndpoint().getHost();
        }
        if (StringUtils.hasText(definition.getEndpoint().getHttpBaseUrl())) {
            return definition.getEndpoint().getHttpBaseUrl();
        }
        if (StringUtils.hasText(definition.getEndpoint().getOpcEndpointUrl())) {
            return definition.getEndpoint().getOpcEndpointUrl();
        }
        return "unknown";
    }

    private boolean isOnline(DevicePointReadResult snapshot) {
        return snapshot != null && snapshot.isSuccess();
    }

    private Set<String> resolveAlarmPointIds(DeviceMonitorDefinition def, DevicePointReadResult snapshot) {
        if (def == null || snapshot == null || snapshot.getItems() == null) {
            return Collections.emptySet();
        }
        Set<String> alarmPointIds = new LinkedHashSet<>();
        for (DevicePointValue item : snapshot.getItems()) {
            if (item == null || !def.getDefaultPoints().containsKey(item.getPointId())) {
                continue;
            }
            if (Boolean.TRUE.equals(item.getAlarm())) {
                alarmPointIds.add(item.getPointId());
            }
        }
        return alarmPointIds;
    }

    // ==================== 内部结果类 ====================

    static class GroupReadResult {
        private final ConnectionKey connectionKey;
        private final DeviceIoResult result;

        GroupReadResult(ConnectionKey connectionKey, DeviceIoResult result) {
            this.connectionKey = connectionKey;
            this.result = result;
        }

        ConnectionKey getConnectionKey() {
            return connectionKey;
        }

        DeviceIoResult getResult() {
            return result;
        }
    }
}
