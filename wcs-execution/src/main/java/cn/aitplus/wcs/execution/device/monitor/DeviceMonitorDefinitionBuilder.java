package cn.aitplus.wcs.execution.device.monitor;

import cn.aitplus.wcs.adapters.io.connection.ConnectionKey;
import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.domain.model.device.DeviceConfig;
import cn.aitplus.wcs.core.domain.model.device.DevicePointsConfig;
import cn.aitplus.wcs.execution.device.io.runtime.profile.DeviceRuntimeProfileResolver;
import cn.aitplus.wcs.execution.device.io.runtime.point.ResolvedDevicePointResolver;
import cn.aitplus.wcs.execution.device.io.runtime.model.DeviceRuntimeProfile;
import cn.aitplus.wcs.execution.device.io.runtime.model.ResolvedDevicePoint;
import cn.aitplus.wcs.infra.service.device.DeviceConfigRedisScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 从 Redis 配置构建 {@link MonitorIndex}。
 */
@Component
public class DeviceMonitorDefinitionBuilder {

    private static final Logger log = LoggerFactory.getLogger(DeviceMonitorDefinitionBuilder.class);

    private final DeviceConfigRedisScanner configScanner;
    private final DeviceRuntimeProfileResolver runtimeProfileResolver;
    private final ResolvedDevicePointResolver resolvedDevicePointResolver;
    private final DeviceMonitorProperties properties;

    public DeviceMonitorDefinitionBuilder(DeviceConfigRedisScanner configScanner,
                                          DeviceRuntimeProfileResolver runtimeProfileResolver,
                                          ResolvedDevicePointResolver resolvedDevicePointResolver,
                                          DeviceMonitorProperties properties) {
        this.configScanner = configScanner;
        this.runtimeProfileResolver = runtimeProfileResolver;
        this.resolvedDevicePointResolver = resolvedDevicePointResolver;
        this.properties = properties;
    }

    public MonitorIndex buildIndex() {
        List<DeviceConfig> allConfigs = configScanner.findAllDeviceConfigs();
        Map<String, DevicePointsConfig> allPointsConfigs = configScanner.findAllDevicePointsConfigs();

        Map<String, DeviceMonitorDefinition> definitionsByDeviceId = new LinkedHashMap<>();
        Map<ConnectionKey, List<DeviceMonitorDefinition>> groups = new LinkedHashMap<>();

        for (DeviceConfig config : allConfigs) {
            try {
                processDevice(config, allPointsConfigs, definitionsByDeviceId, groups);
            } catch (Exception ex) {
                log.warn("构建设备监控定义失败 deviceId={}", config.getDeviceId(), ex);
            }
        }

        long passiveDevices = definitionsByDeviceId.values().stream().filter(def -> !def.isActivePolling()).count();
        log.info("监控索引构建完成：轮询组 {} 个，设备 {} 台，非轮询设备 {} 台",
                groups.size(), definitionsByDeviceId.size(), passiveDevices);
        for (Map.Entry<ConnectionKey, List<DeviceMonitorDefinition>> entry : groups.entrySet()) {
            List<DeviceMonitorDefinition> defs = entry.getValue();
            if (defs == null || defs.isEmpty()) {
                continue;
            }
            DeviceMonitorDefinition sample = defs.get(0);
            String deviceIds = defs.stream()
                    .map(DeviceMonitorDefinition::getDeviceId)
                    .collect(Collectors.joining(","));
            log.info("监控轮询组 connectionKey={} endpoint={} 设备数={} deviceIds={}",
                    entry.getKey(), endpointLabel(sample), defs.size(), deviceIds);
        }

        return MonitorIndex.builder()
                .definitionsByDeviceId(definitionsByDeviceId)
                .groups(groups)
                .build();
    }

    private void processDevice(DeviceConfig config,
                               Map<String, DevicePointsConfig> allPointsConfigs,
                               Map<String, DeviceMonitorDefinition> definitionsByDeviceId,
                               Map<ConnectionKey, List<DeviceMonitorDefinition>> groups) {

        String deviceId = config.getDeviceId();
        if (!StringUtils.hasText(deviceId)) {
            return;
        }
        DevicePointsConfig devicePointsConfig = allPointsConfigs.get(deviceId);
        DeviceRuntimeProfile runtimeProfile = runtimeProfileResolver.resolveForMonitoring(config, devicePointsConfig);
        DomainEnums.CommandDomain domain = runtimeProfile.getDomain();

        // OPC UA 本期不纳入监控
        if (domain == DomainEnums.CommandDomain.OPC) {
            log.debug("跳过 OPC UA 设备 {} (本期不纳入监控)", deviceId);
            return;
        }

        Long warehouseId = runtimeProfile.getWarehouseId();

        // HTTP/RCS 默认在线
        if (domain == DomainEnums.CommandDomain.HTTP) {
            DeviceMonitorDefinition def = DeviceMonitorDefinition.builder()
                    .runtimeProfile(runtimeProfile)
                    .connectionKey(ConnectionKey.from(warehouseId, domain, runtimeProfile.getEndpoint()))
                    .pollingEnabled(false)
                    .defaultPoints(Map.of())
                    .build();
            definitionsByDeviceId.put(deviceId, def);
            return;
        }

        // S7 / Modbus：构建监控定义
        ConnectionKey connectionKey = ConnectionKey.from(warehouseId, domain, runtimeProfile.getEndpoint());
        Map<String, ResolvedDevicePoint> defaultPoints = resolveDefaultPoints(deviceId, domain, allPointsConfigs);

        DeviceMonitorDefinition def = DeviceMonitorDefinition.builder()
                .runtimeProfile(runtimeProfile)
                .connectionKey(connectionKey)
                .pollingEnabled(true)
                .defaultPoints(defaultPoints)
                .build();

        definitionsByDeviceId.put(deviceId, def);
        groups.computeIfAbsent(connectionKey, k -> new ArrayList<>()).add(def);
    }

    private Map<String, ResolvedDevicePoint> resolveDefaultPoints(
            String deviceId, DomainEnums.CommandDomain domain,
            Map<String, DevicePointsConfig> allPointsConfigs) {

        DevicePointsConfig pointsConfig = allPointsConfigs.get(deviceId);
        if (pointsConfig == null || pointsConfig.getPointsConfig() == null) {
            log.warn("设备 {} 无点位配置，默认监控点为空", deviceId);
            return Map.of();
        }

        Map<String, ResolvedDevicePoint> result = new LinkedHashMap<>();
        for (String pointId : properties.getDefaultPointIds(domain)) {
            try {
                ResolvedDevicePoint resolved = resolvedDevicePointResolver.resolveNullable(deviceId, domain, pointsConfig, pointId);
                if (resolved == null) {
                    log.debug("设备 {} 不含默认监控点 {}", deviceId, pointId);
                    continue;
                }
                result.put(pointId, resolved);
            } catch (Exception ex) {
                log.warn("设备 {} 点位 {} 地址转换失败", deviceId, pointId, ex);
            }
        }
        return result;
    }

    /**
     * 解析动态点位（register 时调用）。
     */
    public ResolvedDevicePoint resolvePoint(String deviceId, String pointId,
                                            DomainEnums.CommandDomain domain,
                                            Map<String, DevicePointsConfig> allPointsConfigs) {
        DevicePointsConfig pointsConfig = allPointsConfigs.get(deviceId);
        return resolvedDevicePointResolver.resolveNullable(deviceId, domain, pointsConfig, pointId);
    }

    private static String endpointLabel(DeviceMonitorDefinition definition) {
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

}
