package cn.aitplus.wcs.execution.device.io.runtime;

import cn.aitplus.wcs.core.domain.model.device.DeviceConfig;
import cn.aitplus.wcs.core.domain.model.device.DevicePointsConfig;
import cn.aitplus.wcs.execution.device.io.runtime.model.DeviceRuntimeProfile;
import cn.aitplus.wcs.infra.service.device.DeviceConfigRedisReader;
import cn.aitplus.wcs.infra.service.device.DevicePointsConfigRedisReader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 设备运行时画像装配服务。
 */
@Service
public class DeviceRuntimeProfileService {

    private final DeviceConfigRedisReader deviceConfigRedisReader;
    private final DevicePointsConfigRedisReader devicePointsConfigRedisReader;
    private final DeviceProtocolResolver deviceProtocolResolver;
    private final DeviceEndpointResolver deviceEndpointResolver;

    public DeviceRuntimeProfileService(DeviceConfigRedisReader deviceConfigRedisReader,
                                       DevicePointsConfigRedisReader devicePointsConfigRedisReader,
                                       DeviceProtocolResolver deviceProtocolResolver,
                                       DeviceEndpointResolver deviceEndpointResolver) {
        this.deviceConfigRedisReader = deviceConfigRedisReader;
        this.devicePointsConfigRedisReader = devicePointsConfigRedisReader;
        this.deviceProtocolResolver = deviceProtocolResolver;
        this.deviceEndpointResolver = deviceEndpointResolver;
    }

    public DeviceRuntimeProfile load(Long warehouseId, String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            throw new IllegalStateException("设备编号为空，无法装配设备运行时画像");
        }
        DeviceConfig deviceConfig = deviceConfigRedisReader.findByDeviceId(deviceId)
            .orElseThrow(() -> new IllegalStateException("未找到设备配置，deviceId=" + deviceId));
        DevicePointsConfig devicePointsConfig = devicePointsConfigRedisReader.findByDeviceId(deviceId)
            .orElseThrow(() -> new IllegalStateException("未找到设备点位配置，deviceId=" + deviceId));
        Long resolvedWarehouseId = resolveWarehouseId(warehouseId, deviceConfig);
        var domain = deviceProtocolResolver.resolveDomain(deviceConfig.getProtocolType());
        var endpoint = deviceEndpointResolver.resolveEndpoint(domain, deviceConfig.getConnectionString());
        return DeviceRuntimeProfile.builder()
            .warehouseId(resolvedWarehouseId)
            .deviceConfig(deviceConfig)
            .devicePointsConfig(devicePointsConfig)
            .domain(domain)
            .endpoint(endpoint)
            .build();
    }

    private Long resolveWarehouseId(Long warehouseId, DeviceConfig deviceConfig) {
        if (warehouseId != null) {
            return warehouseId;
        }
        if (!StringUtils.hasText(deviceConfig.getWarehouseIds())) {
            throw new IllegalStateException("设备未配置 warehouseIds，且调用时未显式传入 warehouseId，deviceId="
                + deviceConfig.getDeviceId());
        }
        String[] segments = Arrays.stream(deviceConfig.getWarehouseIds().split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toArray(String[]::new);
        if (segments.length == 0) {
            throw new IllegalStateException("设备 warehouseIds 配置为空，deviceId=" + deviceConfig.getDeviceId());
        }
        if (segments.length > 1) {
            throw new IllegalStateException("设备存在多个 warehouseIds，请显式指定 warehouseId，deviceId="
                + deviceConfig.getDeviceId());
        }
        try {
            return Long.parseLong(segments[0]);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("设备 warehouseIds 配置不是合法数字，deviceId=" + deviceConfig.getDeviceId(), ex);
        }
    }
}
