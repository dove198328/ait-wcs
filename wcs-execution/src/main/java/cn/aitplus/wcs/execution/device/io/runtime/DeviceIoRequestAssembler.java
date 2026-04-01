package cn.aitplus.wcs.execution.device.io.runtime;

import cn.aitplus.wcs.core.domain.model.device.DevicePointDefinition;
import cn.aitplus.wcs.core.spi.device.DeviceIoItem;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.execution.device.io.runtime.model.DeviceIoPlan;
import cn.aitplus.wcs.execution.device.io.runtime.model.DeviceRuntimeProfile;
import cn.aitplus.wcs.execution.device.io.runtime.model.ResolvedDevicePoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 设备 IO 请求装配器。
 */
@Component
public class DeviceIoRequestAssembler {

    private final DevicePointAddressResolver devicePointAddressResolver;

    public DeviceIoRequestAssembler(DevicePointAddressResolver devicePointAddressResolver) {
        this.devicePointAddressResolver = devicePointAddressResolver;
    }

    public DeviceIoPlan assembleReadPlan(DeviceRuntimeProfile runtimeProfile, Collection<String> pointIds) {
        validateRuntimeProfile(runtimeProfile);
        if (pointIds == null || pointIds.isEmpty()) {
            throw new IllegalStateException("读取点位列表为空，无法装配设备读取请求");
        }
        List<DeviceIoItem> items = new ArrayList<>();
        List<ResolvedDevicePoint> resolvedPoints = new ArrayList<>();
        for (String pointId : pointIds) {
            ResolvedDevicePoint resolvedPoint = resolvePoint(runtimeProfile, pointId);
            validateReadable(resolvedPoint.getPointDefinition());
            resolvedPoints.add(resolvedPoint);
            items.add(DeviceIoItem.builder()
                .address(resolvedPoint.getAdapterAddress())
                .write(Boolean.FALSE)
                .build());
        }
        return DeviceIoPlan.builder()
            .runtimeProfile(runtimeProfile)
            .request(buildRequest(runtimeProfile, items))
            .resolvedPoints(resolvedPoints)
            .build();
    }

    public DeviceIoPlan assembleWritePlan(DeviceRuntimeProfile runtimeProfile, Map<String, Object> pointValues) {
        validateRuntimeProfile(runtimeProfile);
        if (pointValues == null || pointValues.isEmpty()) {
            throw new IllegalStateException("写入点位列表为空，无法装配设备写入请求");
        }
        List<DeviceIoItem> items = new ArrayList<>();
        List<ResolvedDevicePoint> resolvedPoints = new ArrayList<>();
        for (Map.Entry<String, Object> entry : pointValues.entrySet()) {
            ResolvedDevicePoint resolvedPoint = resolvePoint(runtimeProfile, entry.getKey());
            validateWritable(resolvedPoint.getPointDefinition());
            resolvedPoint.setWriteValue(entry.getValue());
            resolvedPoints.add(resolvedPoint);
            items.add(DeviceIoItem.builder()
                .address(resolvedPoint.getAdapterAddress())
                .value(entry.getValue())
                .write(Boolean.TRUE)
                .build());
        }
        return DeviceIoPlan.builder()
            .runtimeProfile(runtimeProfile)
            .request(buildRequest(runtimeProfile, items))
            .resolvedPoints(resolvedPoints)
            .build();
    }

    private DeviceIoRequest buildRequest(DeviceRuntimeProfile runtimeProfile, List<DeviceIoItem> items) {
        return DeviceIoRequest.builder()
            .warehouseId(runtimeProfile.getWarehouseId())
            .domain(runtimeProfile.getDomain())
            .endpoint(runtimeProfile.getEndpoint())
            .deviceId(runtimeProfile.getDeviceConfig().getDeviceId())
            .items(items)
            .build();
    }

    private ResolvedDevicePoint resolvePoint(DeviceRuntimeProfile runtimeProfile, String pointId) {
        if (!StringUtils.hasText(pointId)) {
            throw new IllegalStateException("点位编号为空，无法装配设备 IO 请求");
        }
        DevicePointDefinition pointDefinition = runtimeProfile.getDevicePointsConfig().getPointsConfig().get(pointId.trim());
        if (pointDefinition == null) {
            throw new IllegalStateException("未找到设备点位定义，deviceId=" + runtimeProfile.getDeviceConfig().getDeviceId()
                + "，pointId=" + pointId);
        }
        validatePointDefinition(runtimeProfile.getDeviceConfig().getDeviceId(), pointDefinition);
        return ResolvedDevicePoint.builder()
            .pointId(pointId.trim())
            .pointDefinition(pointDefinition)
            .adapterAddress(devicePointAddressResolver.resolve(runtimeProfile.getDomain(), pointDefinition))
            .build();
    }

    private void validateReadable(DevicePointDefinition pointDefinition) {
        if ("WRITE_ONLY".equals(normalizeAccess(pointDefinition))) {
            throw new IllegalStateException("点位不支持读取，pointId=" + pointDefinition.getPointId());
        }
    }

    private void validateWritable(DevicePointDefinition pointDefinition) {
        if ("READ_ONLY".equals(normalizeAccess(pointDefinition))) {
            throw new IllegalStateException("点位不支持写入，pointId=" + pointDefinition.getPointId());
        }
    }

    private String normalizeAccess(DevicePointDefinition pointDefinition) {
        return pointDefinition.getAccess() == null ? "" : pointDefinition.getAccess().trim().toUpperCase();
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

    private void validateRuntimeProfile(DeviceRuntimeProfile runtimeProfile) {
        if (runtimeProfile == null) {
            throw new IllegalStateException("设备运行时画像为空，无法装配设备 IO 请求");
        }
    }
}
