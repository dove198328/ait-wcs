package cn.aitplus.wcs.execution.device.io.runtime;

import cn.aitplus.wcs.core.domain.model.device.DevicePointReadResult;
import cn.aitplus.wcs.core.domain.model.device.DevicePointWriteResult;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.execution.device.io.runtime.model.DeviceIoPlan;
import cn.aitplus.wcs.execution.device.io.runtime.model.DeviceRuntimeProfile;
import cn.aitplus.wcs.execution.device.io.runtime.model.ResolvedDevicePoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 设备 IO 结果映射器。
 */
@Component
public class DeviceIoResultMapper {

    private final DeviceIoReadResultParser readResultParser;
    private final DevicePointValueMapper devicePointValueMapper;

    public DeviceIoResultMapper(DeviceIoReadResultParser readResultParser,
                                DevicePointValueMapper devicePointValueMapper) {
        this.readResultParser = readResultParser;
        this.devicePointValueMapper = devicePointValueMapper;
    }

    public DevicePointReadResult toReadResult(DeviceIoPlan ioPlan, DeviceIoResult ioResult) {
        DeviceRuntimeProfile profile = ioPlan.getRuntimeProfile();
        DevicePointReadResult.DevicePointReadResultBuilder builder = DevicePointReadResult.builder()
            .success(ioResult != null && ioResult.isSuccess())
            .errorCode(ioResult != null ? ioResult.getErrorCode() : "UNKNOWN")
            .errorMessage(ioResult != null ? ioResult.getErrorMessage() : "设备读取结果为空")
            .deviceId(profile.getDeviceConfig().getDeviceId())
            .deviceName(profile.getDeviceConfig().getDeviceName())
            .warehouseId(profile.getWarehouseId())
            .protocolType(profile.getDeviceConfig().getProtocolType())
            .rawResponseJson(ioResult != null ? ioResult.getResponseJson() : null)
            .updatedAt(java.time.Instant.now());
        if (ioResult == null || !ioResult.isSuccess()) {
            return builder.items(new ArrayList<>()).build();
        }
        Map<String, Object> valueByAddress = readResultParser.parseReadValues(ioResult.getResponseJson());
        List<cn.aitplus.wcs.core.domain.model.device.DevicePointValue> items = new ArrayList<>();
        for (ResolvedDevicePoint resolvedPoint : ioPlan.getResolvedPoints()) {
            Object rawValue = valueByAddress.get(resolvedPoint.getAdapterAddress());
            items.add(devicePointValueMapper.toPointValue(profile.getDomain(), resolvedPoint, rawValue));
        }
        return builder.items(items).build();
    }

    public DevicePointWriteResult toWriteResult(DeviceIoPlan ioPlan, DeviceIoResult ioResult) {
        DeviceRuntimeProfile profile = ioPlan.getRuntimeProfile();
        List<String> pointIds = ioPlan.getResolvedPoints().stream()
            .map(ResolvedDevicePoint::getPointId)
            .toList();
        return DevicePointWriteResult.builder()
            .success(ioResult != null && ioResult.isSuccess())
            .errorCode(ioResult != null ? ioResult.getErrorCode() : "UNKNOWN")
            .errorMessage(ioResult != null ? ioResult.getErrorMessage() : "设备写入结果为空")
            .deviceId(profile.getDeviceConfig().getDeviceId())
            .deviceName(profile.getDeviceConfig().getDeviceName())
            .protocolType(profile.getDeviceConfig().getProtocolType())
            .rawResponseJson(ioResult != null ? ioResult.getResponseJson() : null)
            .pointIds(pointIds)
            .build();
    }

}
