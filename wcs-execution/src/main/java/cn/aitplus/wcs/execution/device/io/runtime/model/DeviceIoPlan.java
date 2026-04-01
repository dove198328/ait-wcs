package cn.aitplus.wcs.execution.device.io.runtime.model;

import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备 IO 执行计划。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceIoPlan {

    private DeviceRuntimeProfile runtimeProfile;
    private DeviceIoRequest request;

    @Builder.Default
    private List<ResolvedDevicePoint> resolvedPoints = new ArrayList<>();
}
