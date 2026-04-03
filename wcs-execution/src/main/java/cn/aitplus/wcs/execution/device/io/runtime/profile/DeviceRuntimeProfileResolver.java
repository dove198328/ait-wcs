package cn.aitplus.wcs.execution.device.io.runtime.profile;

import cn.aitplus.wcs.core.domain.model.device.DeviceConfig;
import cn.aitplus.wcs.core.domain.model.device.DevicePointsConfig;
import cn.aitplus.wcs.execution.device.io.runtime.model.DeviceRuntimeProfile;
import cn.aitplus.wcs.execution.device.io.runtime.protocol.DeviceEndpointResolver;
import cn.aitplus.wcs.execution.device.io.runtime.protocol.DeviceProtocolResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * 统一解析设备配置为运行时画像，供业务 IO 与监控共同复用。
 */
@Component
public class DeviceRuntimeProfileResolver {

    private final DeviceProtocolResolver deviceProtocolResolver;
    private final DeviceEndpointResolver deviceEndpointResolver;

    public DeviceRuntimeProfileResolver(DeviceProtocolResolver deviceProtocolResolver,
                                        DeviceEndpointResolver deviceEndpointResolver) {
        this.deviceProtocolResolver = deviceProtocolResolver;
        this.deviceEndpointResolver = deviceEndpointResolver;
    }

    public DeviceRuntimeProfile resolveForIo(DeviceConfig deviceConfig,
                                             DevicePointsConfig devicePointsConfig,
                                             Long warehouseId) {
        validateDeviceConfig(deviceConfig);
        return buildProfile(deviceConfig, devicePointsConfig, resolveWarehouseIdForIo(warehouseId, deviceConfig));
    }

    public DeviceRuntimeProfile resolveForMonitoring(DeviceConfig deviceConfig,
                                                     DevicePointsConfig devicePointsConfig) {
        validateDeviceConfig(deviceConfig);
        return buildProfile(deviceConfig, devicePointsConfig, parseFirstWarehouseId(deviceConfig.getWarehouseIds()));
    }

    private DeviceRuntimeProfile buildProfile(DeviceConfig deviceConfig,
                                              DevicePointsConfig devicePointsConfig,
                                              Long warehouseId) {
        var domain = deviceProtocolResolver.resolveDomain(deviceConfig.getProtocolType());
        var endpoint = deviceEndpointResolver.resolveEndpoint(domain, deviceConfig.getConnectionString());
        return DeviceRuntimeProfile.builder()
                .warehouseId(warehouseId)
                .deviceConfig(deviceConfig)
                .devicePointsConfig(devicePointsConfig)
                .domain(domain)
                .endpoint(endpoint)
                .build();
    }

    private Long resolveWarehouseIdForIo(Long warehouseId, DeviceConfig deviceConfig) {
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

    private Long parseFirstWarehouseId(String warehouseIds) {
        if (!StringUtils.hasText(warehouseIds)) {
            return null;
        }
        String first = warehouseIds.split(",")[0].trim();
        if (!StringUtils.hasText(first)) {
            return null;
        }
        try {
            return Long.valueOf(first);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("设备 warehouseIds 配置不是合法数字，warehouseIds=" + warehouseIds, ex);
        }
    }

    private void validateDeviceConfig(DeviceConfig deviceConfig) {
        if (deviceConfig == null) {
            throw new IllegalStateException("设备配置为空，无法解析运行时画像");
        }
    }
}
