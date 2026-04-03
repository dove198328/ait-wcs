package cn.aitplus.wcs.execution.device.io;

import cn.aitplus.wcs.adapters.io.registry.DeviceTransportRegistry;
import cn.aitplus.wcs.core.domain.model.device.DevicePointReadResult;
import cn.aitplus.wcs.core.domain.model.device.DevicePointWriteResult;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.execution.device.io.runtime.pipline.DeviceIoRequestAssembler;
import cn.aitplus.wcs.execution.device.io.runtime.pipline.DeviceIoResultMapper;
import cn.aitplus.wcs.execution.device.io.runtime.profile.DeviceRuntimeProfileService;
import cn.aitplus.wcs.execution.device.io.runtime.model.DeviceIoPlan;
import cn.aitplus.wcs.execution.device.io.runtime.model.DeviceRuntimeProfile;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 统一设备 IO 门面。
 */
@Service
public class DeviceIoFacade {

    private final DeviceRuntimeProfileService deviceRuntimeProfileService;
    private final DeviceIoRequestAssembler deviceIoRequestAssembler;
    private final DeviceTransportRegistry deviceTransportRegistry;
    private final DeviceIoResultMapper deviceIoResultMapper;

    public DeviceIoFacade(DeviceRuntimeProfileService deviceRuntimeProfileService,
                          DeviceIoRequestAssembler deviceIoRequestAssembler,
                          DeviceTransportRegistry deviceTransportRegistry,
                          DeviceIoResultMapper deviceIoResultMapper) {
        this.deviceRuntimeProfileService = deviceRuntimeProfileService;
        this.deviceIoRequestAssembler = deviceIoRequestAssembler;
        this.deviceTransportRegistry = deviceTransportRegistry;
        this.deviceIoResultMapper = deviceIoResultMapper;
    }

    public DevicePointReadResult readPoint(Long warehouseId, String deviceId, String pointId) {
        return readPoints(warehouseId, deviceId, List.of(pointId));
    }

    public DevicePointReadResult readPoints(Long warehouseId, String deviceId, Collection<String> pointIds) {
        DeviceRuntimeProfile runtimeProfile = deviceRuntimeProfileService.load(warehouseId, deviceId);
        DeviceIoPlan ioPlan = deviceIoRequestAssembler.assembleReadPlan(runtimeProfile, pointIds);
        DeviceIoResult ioResult = deviceTransportRegistry.execute(ioPlan.getRequest());
        return deviceIoResultMapper.toReadResult(ioPlan, ioResult);
    }

    public DevicePointWriteResult writePoint(Long warehouseId, String deviceId, String pointId, Object value) {
        return writePoints(warehouseId, deviceId, Map.of(pointId, value));
    }

    public DevicePointWriteResult writePoints(Long warehouseId, String deviceId, Map<String, Object> pointValues) {
        DeviceRuntimeProfile runtimeProfile = deviceRuntimeProfileService.load(warehouseId, deviceId);
        DeviceIoPlan ioPlan = deviceIoRequestAssembler.assembleWritePlan(runtimeProfile, pointValues);
        DeviceIoResult ioResult = deviceTransportRegistry.execute(ioPlan.getRequest());
        return deviceIoResultMapper.toWriteResult(ioPlan, ioResult);
    }
}
