package cn.aitplus.wcs.execution.device.io.runtime;

import cn.aitplus.wcs.core.domain.model.device.DeviceConfig;
import cn.aitplus.wcs.core.domain.model.device.DevicePointsConfig;
import cn.aitplus.wcs.execution.device.io.runtime.model.DeviceRuntimeProfile;
import cn.aitplus.wcs.infra.service.device.DeviceConfigRedisReader;
import cn.aitplus.wcs.infra.service.device.DevicePointsConfigRedisReader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 设备运行时画像装配服务。
 */
@Service
public class DeviceRuntimeProfileService {

    private final DeviceConfigRedisReader deviceConfigRedisReader;
    private final DevicePointsConfigRedisReader devicePointsConfigRedisReader;
    private final DeviceRuntimeProfileResolver runtimeProfileResolver;

    public DeviceRuntimeProfileService(DeviceConfigRedisReader deviceConfigRedisReader,
                                       DevicePointsConfigRedisReader devicePointsConfigRedisReader,
                                       DeviceRuntimeProfileResolver runtimeProfileResolver) {
        this.deviceConfigRedisReader = deviceConfigRedisReader;
        this.devicePointsConfigRedisReader = devicePointsConfigRedisReader;
        this.runtimeProfileResolver = runtimeProfileResolver;
    }

    public DeviceRuntimeProfile load(Long warehouseId, String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            throw new IllegalStateException("设备编号为空，无法装配设备运行时画像");
        }
        DeviceConfig deviceConfig = deviceConfigRedisReader.findByDeviceId(deviceId)
            .orElseThrow(() -> new IllegalStateException("未找到设备配置，deviceId=" + deviceId));
        DevicePointsConfig devicePointsConfig = devicePointsConfigRedisReader.findByDeviceId(deviceId)
            .orElseThrow(() -> new IllegalStateException("未找到设备点位配置，deviceId=" + deviceId));
        return runtimeProfileResolver.resolveForIo(deviceConfig, devicePointsConfig, warehouseId);
    }
}
