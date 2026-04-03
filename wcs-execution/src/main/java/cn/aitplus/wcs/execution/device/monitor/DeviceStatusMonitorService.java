package cn.aitplus.wcs.execution.device.monitor;

import cn.aitplus.wcs.adapters.io.connection.ConnectionKey;
import cn.aitplus.wcs.adapters.io.registry.DeviceTransportRegistry;
import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.domain.event.DeviceAlarmChangedEvent;
import cn.aitplus.wcs.core.domain.event.DeviceOnlineChangedEvent;
import cn.aitplus.wcs.core.domain.event.DevicePointValueChangedEvent;
import cn.aitplus.wcs.core.domain.model.device.DevicePointReadResult;
import cn.aitplus.wcs.core.domain.model.device.DevicePointValue;
import cn.aitplus.wcs.core.domain.model.device.DevicePointsConfig;
import cn.aitplus.wcs.core.spi.device.DeviceEndpoint;
import cn.aitplus.wcs.core.spi.device.DeviceIoItem;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.execution.device.io.runtime.model.ResolvedDevicePoint;
import cn.aitplus.wcs.execution.device.io.runtime.pipline.DeviceIoReadsParser;
import cn.aitplus.wcs.execution.device.io.runtime.point.DevicePointValueMapper;
import cn.aitplus.wcs.infra.service.device.DeviceConfigRedisScanner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 设备状态监控服务。
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
    private final DeviceIoReadsParser readResultParser;
    private final ObjectMapper objectMapper;

    private volatile MonitorIndex currentIndex;
    private final ConcurrentHashMap<String, DevicePointReadResult> lastSnapshots = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> lastDynamicPointValues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> heartbeatPhase = new ConcurrentHashMap<>();

    private ExecutorService ioExecutor;

    public DeviceStatusMonitorService(DeviceMonitorDefinitionBuilder definitionBuilder,
                                      DeviceTransportRegistry transportRegistry,
                                      DevicePointValueMapper devicePointValueMapper,
                                      ApplicationEventPublisher eventPublisher,
                                      DeviceMonitorProperties properties,
                                      DeviceConfigRedisScanner configScanner,
                                      DeviceIoReadsParser readResultParser,
                                      ObjectMapper objectMapper) {
        this.definitionBuilder = definitionBuilder;
        this.transportRegistry = transportRegistry;
        this.devicePointValueMapper = devicePointValueMapper;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.configScanner = configScanner;
        this.readResultParser = readResultParser;
        this.objectMapper = objectMapper;
    }

    public void initialize() {
        this.currentIndex = definitionBuilder.buildIndex();
        this.ioExecutor = Executors.newFixedThreadPool(properties.getIoPoolSize(), r -> {
            Thread t = new Thread(r, "wcs-monitor-io");
            t.setDaemon(true);
            return t;
        });

        for (DeviceMonitorDefinition def : currentIndex.getDefinitionsByDeviceId().values()) {
            if (!def.isActivePolling()) {
                lastSnapshots.put(def.getDeviceId(), buildPassiveSnapshot(def));
            }
        }

        log.info("DeviceStatusMonitorService 初始化完成，IO 线程池大小={}", properties.getIoPoolSize());
    }

    public void poll() {
        MonitorIndex index = currentIndex;
        if (index == null) {
            return;
        }

        long timeout = properties.getPollIntervalMillis() * 80 / 100;
        Map<ConnectionKey, List<DeviceMonitorDefinition>> groups = index.getGroups();
        Map<ConnectionKey, CompletableFuture<GroupReadResult>> futures = new LinkedHashMap<>();

        for (Map.Entry<ConnectionKey, List<DeviceMonitorDefinition>> entry : groups.entrySet()) {
            ConnectionKey ck = entry.getKey();
            List<DeviceMonitorDefinition> definitions = entry.getValue().stream()
                    .filter(DeviceMonitorDefinition::isActivePolling)
                    .toList();
            if (definitions.isEmpty()) {
                continue;
            }
            futures.put(ck, CompletableFuture.supplyAsync(() -> readGroup(ck, definitions), ioExecutor));
        }

        Map<ConnectionKey, GroupReadResult> results = new LinkedHashMap<>();
        for (Map.Entry<ConnectionKey, CompletableFuture<GroupReadResult>> entry : futures.entrySet()) {
            try {
                results.put(entry.getKey(), entry.getValue().get(timeout, TimeUnit.MILLISECONDS));
            } catch (Exception ex) {
                log.debug("读取超时或异常 connectionKey={}", entry.getKey(), ex);
                results.put(entry.getKey(), GroupReadResult.empty(entry.getKey()));
            }
        }

        for (DeviceMonitorDefinition def : index.getDefinitionsByDeviceId().values()) {
            if (!def.isActivePolling()) {
                continue;
            }
            try {
                processDeviceResult(def, results.get(def.getConnectionKey()));
            } catch (Exception ex) {
                log.warn("处理设备结果异常 deviceId={}", def.getDeviceId(), ex);
            }
        }
    }

    private GroupReadResult readGroup(ConnectionKey connectionKey, List<DeviceMonitorDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            log.info("监控轮询跳过 connectionKey={} reason=no_definitions", connectionKey);
            return GroupReadResult.empty(connectionKey);
        }
        DeviceMonitorDefinition sample = definitions.get(0);
        if (sample.getDomain() == DomainEnums.CommandDomain.MODBUS) {
            return readModbusGroup(connectionKey, definitions);
        }
        return readSharedGroup(connectionKey, definitions);
    }

    private GroupReadResult readSharedGroup(ConnectionKey connectionKey, List<DeviceMonitorDefinition> definitions) {
        DeviceMonitorDefinition sample = definitions.get(0);
        String deviceIds = definitions.stream()
                .map(DeviceMonitorDefinition::getDeviceId)
                .collect(Collectors.joining(","));
        String endpointLabel = resolveEndpointLabel(sample);
        Set<String> uniqueAddresses = collectUniqueAddresses(definitions);

        if (uniqueAddresses.isEmpty()) {
            log.info("监控轮询跳过 endpoint={} connectionKey={} devices={} reason=no_physical_addresses",
                    endpointLabel, connectionKey, deviceIds);
            return GroupReadResult.empty(connectionKey);
        }

        DeviceIoRequest readReq = buildReadRequest(sample, uniqueAddresses);
        DeviceIoResult result = transportRegistry.execute(readReq);
        if (result.isSuccess()) {
            log.info("监控轮询成功 endpoint={} connectionKey={} devices={} response={}",
                    endpointLabel, connectionKey, deviceIds, result.getResponseJson());
        } else {
            log.warn("监控轮询失败 endpoint={} connectionKey={} devices={} errorCode={} errorMessage={}",
                    endpointLabel, connectionKey, deviceIds, result.getErrorCode(), result.getErrorMessage());
        }

        String heartbeatWritePointId = properties.getHeartbeatWritePointId(sample.getDomain());
        if (result.isSuccess() && StringUtils.hasText(heartbeatWritePointId)) {
            writeHeartbeat(connectionKey, definitions);
        }

        return GroupReadResult.same(connectionKey, definitions, result);
    }

    private GroupReadResult readModbusGroup(ConnectionKey connectionKey, List<DeviceMonitorDefinition> definitions) {
        String deviceIds = definitions.stream()
                .map(DeviceMonitorDefinition::getDeviceId)
                .collect(Collectors.joining(","));
        String endpointLabel = resolveEndpointLabel(definitions.get(0));

        Map<Integer, List<DeviceMonitorDefinition>> unitGroups = definitions.stream()
                .collect(Collectors.groupingBy(this::resolveModbusUnitId, LinkedHashMap::new, Collectors.toList()));

        Map<String, DeviceIoResult> resultsByDeviceId = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<DeviceMonitorDefinition>> entry : unitGroups.entrySet()) {
            Integer unitId = entry.getKey();
            List<DeviceMonitorDefinition> unitDefinitions = entry.getValue();
            Set<String> uniqueAddresses = collectUniqueAddresses(unitDefinitions);
            if (uniqueAddresses.isEmpty()) {
                DeviceIoResult emptyResult = DeviceIoResult.ok(buildReadResponseJson(Map.of()));
                for (DeviceMonitorDefinition def : unitDefinitions) {
                    resultsByDeviceId.put(def.getDeviceId(), emptyResult);
                }
                continue;
            }

            DeviceMonitorDefinition sample = unitDefinitions.get(0);
            DeviceIoRequest readReq = buildReadRequest(sample, uniqueAddresses);
            DeviceIoResult result = transportRegistry.execute(readReq);
            if (result.isSuccess()) {
                log.info("监控轮询成功 endpoint={} connectionKey={} unitId={} devices={} response={}",
                        endpointLabel,
                        connectionKey,
                        unitId,
                        unitDefinitions.stream().map(DeviceMonitorDefinition::getDeviceId).collect(Collectors.joining(",")),
                        result.getResponseJson());
            } else {
                log.warn("监控轮询失败 endpoint={} connectionKey={} unitId={} devices={} errorCode={} errorMessage={}",
                        endpointLabel,
                        connectionKey,
                        unitId,
                        unitDefinitions.stream().map(DeviceMonitorDefinition::getDeviceId).collect(Collectors.joining(",")),
                        result.getErrorCode(),
                        result.getErrorMessage());
            }

            String heartbeatWritePointId = properties.getHeartbeatWritePointId(sample.getDomain());
            if (result.isSuccess() && StringUtils.hasText(heartbeatWritePointId)) {
                writeHeartbeat(connectionKey, unitDefinitions);
            }

            for (DeviceMonitorDefinition def : unitDefinitions) {
                resultsByDeviceId.put(def.getDeviceId(), result);
            }
        }

        log.info("监控轮询完成 endpoint={} connectionKey={} devices={}", endpointLabel, connectionKey, deviceIds);
        return new GroupReadResult(connectionKey, resultsByDeviceId);
    }

    private Set<String> collectUniqueAddresses(List<DeviceMonitorDefinition> definitions) {
        Set<String> uniqueAddresses = new LinkedHashSet<>();
        for (DeviceMonitorDefinition def : definitions) {
            for (ResolvedDevicePoint point : def.getEffectivePoints().values()) {
                uniqueAddresses.add(point.getPhysicalAddress());
            }
        }
        return uniqueAddresses;
    }

    private DeviceIoRequest buildReadRequest(DeviceMonitorDefinition sample, Set<String> uniqueAddresses) {
        List<DeviceIoItem> readItems = uniqueAddresses.stream()
                .map(addr -> DeviceIoItem.builder().address(addr).write(false).build())
                .toList();
        return DeviceIoRequest.builder()
                .domain(sample.getDomain())
                .endpoint(sample.getEndpoint())
                .warehouseId(sample.getWarehouseId())
                .items(readItems)
                .build();
    }

    private void writeHeartbeat(ConnectionKey connectionKey, List<DeviceMonitorDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return;
        }
        Map<String, List<DeviceMonitorDefinition>> heartbeatGroups = definitions.stream()
                .collect(Collectors.groupingBy(def -> resolveHeartbeatPhaseKey(connectionKey, def),
                        LinkedHashMap::new,
                        Collectors.toList()));
        for (List<DeviceMonitorDefinition> heartbeatDefinitions : heartbeatGroups.values()) {
            writeHeartbeatGroup(connectionKey, heartbeatDefinitions);
        }
    }

    private void writeHeartbeatGroup(ConnectionKey connectionKey, List<DeviceMonitorDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return;
        }
        DeviceMonitorDefinition sample = definitions.get(0);
        String hbPointId = properties.getHeartbeatWritePointId(sample.getDomain());
        if (!StringUtils.hasText(hbPointId)) {
            return;
        }
        String phaseKey = resolveHeartbeatPhaseKey(connectionKey, sample);
        Integer phase = heartbeatPhase.merge(phaseKey, 1, Integer::sum);
        boolean bit = phase != null && (phase & 1) == 1;

        for (DeviceMonitorDefinition def : definitions) {
            ResolvedDevicePoint hbPoint = def.getEffectivePoints().get(hbPointId);
            if (hbPoint == null) {
                continue;
            }
            DeviceIoRequest writeReq = DeviceIoRequest.builder()
                    .domain(def.getDomain())
                    .endpoint(def.getEndpoint())
                    .warehouseId(def.getWarehouseId())
                    .items(List.of(DeviceIoItem.builder()
                            .address(hbPoint.getPhysicalAddress())
                            .value(bit)
                            .write(true)
                            .build()))
                    .build();
            try {
                DeviceIoResult writeResult = transportRegistry.execute(writeReq);
                if (!writeResult.isSuccess()) {
                    log.debug("心跳写入失败 connectionKey={} error={}", connectionKey, writeResult.getErrorMessage());
                }
            } catch (Exception ex) {
                log.debug("心跳写入异常 connectionKey={}", connectionKey, ex);
            }
        }
    }

    private void processDeviceResult(DeviceMonitorDefinition def, GroupReadResult groupResult) {
        String deviceId = def.getDeviceId();
        DeviceIoResult ioResult = groupResult == null ? null : groupResult.getResult(deviceId);
        boolean online = ioResult != null && ioResult.isSuccess();

        Map<String, Object> rawValues = Collections.emptyMap();
        if (online) {
            rawValues = readResultParser.parseReadValues(ioResult.getResponseJson());
        }

        List<DevicePointValue> pointValues = new ArrayList<>();
        Map<String, ResolvedDevicePoint> effectivePoints = def.getEffectivePoints();

        for (Map.Entry<String, ResolvedDevicePoint> entry : effectivePoints.entrySet()) {
            String pointId = entry.getKey();
            ResolvedDevicePoint point = entry.getValue();
            Object rawValue = rawValues.get(point.getPhysicalAddress());
            DevicePointValue pointValue = devicePointValueMapper.toPointValue(def.getDomain(), point, rawValue);
            pointValues.add(pointValue);
            Object convertedValue = pointValue.getRawValue();

            if (convertedValue == null) {
                continue;
            }

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
        if (!def.isActivePolling()) {
            lastSnapshots.put(def.getDeviceId(), buildPassiveSnapshot(def));
        }
    }

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

    private DevicePointReadResult buildPassiveSnapshot(DeviceMonitorDefinition def) {
        return DevicePointReadResult.builder()
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

    private Integer resolveModbusUnitId(DeviceMonitorDefinition definition) {
        DeviceEndpoint endpoint = definition == null ? null : definition.getEndpoint();
        if (endpoint == null || endpoint.getModbusUnitId() == null) {
            return 1;
        }
        return endpoint.getModbusUnitId();
    }

    private String resolveHeartbeatPhaseKey(ConnectionKey connectionKey, DeviceMonitorDefinition definition) {
        if (definition != null && definition.getDomain() == DomainEnums.CommandDomain.MODBUS) {
            return connectionKey + "#unitId=" + resolveModbusUnitId(definition);
        }
        return String.valueOf(connectionKey);
    }

    private String buildReadResponseJson(Map<String, Object> readMap) {
        try {
            return objectMapper.writeValueAsString(Map.of("reads", readMap));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("构建监控读取结果失败", ex);
        }
    }

    static class GroupReadResult {
        private final ConnectionKey connectionKey;
        private final Map<String, DeviceIoResult> resultsByDeviceId;

        GroupReadResult(ConnectionKey connectionKey, Map<String, DeviceIoResult> resultsByDeviceId) {
            this.connectionKey = connectionKey;
            this.resultsByDeviceId = resultsByDeviceId != null ? resultsByDeviceId : Map.of();
        }

        static GroupReadResult empty(ConnectionKey connectionKey) {
            return new GroupReadResult(connectionKey, Map.of());
        }

        static GroupReadResult same(ConnectionKey connectionKey,
                                    List<DeviceMonitorDefinition> definitions,
                                    DeviceIoResult result) {
            Map<String, DeviceIoResult> resultsByDeviceId = new LinkedHashMap<>();
            for (DeviceMonitorDefinition definition : definitions) {
                resultsByDeviceId.put(definition.getDeviceId(), result);
            }
            return new GroupReadResult(connectionKey, resultsByDeviceId);
        }

        ConnectionKey getConnectionKey() {
            return connectionKey;
        }

        DeviceIoResult getResult(String deviceId) {
            return resultsByDeviceId.get(deviceId);
        }
    }
}
