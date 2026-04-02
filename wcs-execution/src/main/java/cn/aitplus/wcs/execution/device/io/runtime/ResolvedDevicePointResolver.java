package cn.aitplus.wcs.execution.device.io.runtime;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.domain.model.device.DevicePointDefinition;
import cn.aitplus.wcs.core.domain.model.device.DevicePointsConfig;
import cn.aitplus.wcs.execution.device.io.runtime.model.ResolvedDevicePoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 统一解析设备点位定义为运行时可执行点位。
 */
@Component
public class ResolvedDevicePointResolver {

    private final DevicePointAddressResolver devicePointAddressResolver;

    public ResolvedDevicePointResolver(DevicePointAddressResolver devicePointAddressResolver) {
        this.devicePointAddressResolver = devicePointAddressResolver;
    }

    public ResolvedDevicePoint resolveRequired(String deviceId,
                                               DomainEnums.CommandDomain domain,
                                               DevicePointsConfig devicePointsConfig,
                                               String pointId) {
        ResolvedDevicePoint resolved = resolveNullable(deviceId, domain, devicePointsConfig, pointId);
        if (resolved == null) {
            throw new IllegalStateException("未找到设备点位定义，deviceId=" + deviceId + "，pointId=" + pointId);
        }
        return resolved;
    }

    public ResolvedDevicePoint resolveNullable(String deviceId,
                                               DomainEnums.CommandDomain domain,
                                               DevicePointsConfig devicePointsConfig,
                                               String pointId) {
        if (!StringUtils.hasText(pointId)) {
            throw new IllegalStateException("点位编号为空，无法解析设备点位");
        }
        if (devicePointsConfig == null || devicePointsConfig.getPointsConfig() == null) {
            return null;
        }
        String normalizedPointId = pointId.trim();
        DevicePointDefinition pointDefinition = devicePointsConfig.getPointsConfig().get(normalizedPointId);
        if (pointDefinition == null) {
            return null;
        }
        validatePointDefinition(deviceId, pointDefinition);
        return ResolvedDevicePoint.builder()
                .pointId(normalizedPointId)
                .pointDefinition(pointDefinition)
                .adapterAddress(devicePointAddressResolver.resolve(domain, pointDefinition))
                .build();
    }

    private void validatePointDefinition(String deviceId, DevicePointDefinition pointDefinition) {
        if (!StringUtils.hasText(pointDefinition.getPointId())) {
            throw new IllegalStateException("设备点位配置缺少 pointId，deviceId=" + deviceId);
        }
        if (!StringUtils.hasText(pointDefinition.getName())) {
            throw new IllegalStateException("设备点位配置缺少 name，deviceId=" + deviceId + "，pointId=" + pointDefinition.getPointId());
        }
        if (!StringUtils.hasText(pointDefinition.getAddress())) {
            throw new IllegalStateException("设备点位配置缺少 address，deviceId=" + deviceId + "，pointId=" + pointDefinition.getPointId());
        }
        if (!StringUtils.hasText(pointDefinition.getDataType())) {
            throw new IllegalStateException("设备点位配置缺少 dataType，deviceId=" + deviceId + "，pointId=" + pointDefinition.getPointId());
        }
        if (!StringUtils.hasText(pointDefinition.getAccess())) {
            throw new IllegalStateException("设备点位配置缺少 access，deviceId=" + deviceId + "，pointId=" + pointDefinition.getPointId());
        }
    }
}
