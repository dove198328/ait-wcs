package cn.aitplus.wcs.execution.pipeline;

import cn.aitplus.wcs.adapters.io.registry.DeviceTransportRegistry;
import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.infra.service.execution.CommandExecutionService;
import org.springframework.stereotype.Service;

/**
 * CommandPipeline 下发步：调用设备注册表执行 IO，并通过 {@link CommandExecutionService} 回写状态。
 */
@Service
public class CommandPipelineDeviceDispatch {

    private final DeviceTransportRegistry deviceTransportRegistry;
    private final CommandExecutionService commandExecutionService;

    public CommandPipelineDeviceDispatch(DeviceTransportRegistry deviceTransportRegistry,
                                         CommandExecutionService commandExecutionService) {
        this.deviceTransportRegistry = deviceTransportRegistry;
        this.commandExecutionService = commandExecutionService;
    }

    /**
     * 仅执行 IO，不写库（测试或编排中自行落库时使用）。
     */
    public DeviceIoResult executeDeviceIo(DeviceIoRequest request) {
        return deviceTransportRegistry.execute(request);
    }

    /**
     * 使用临时连接执行 IO，不复用连接池（由具体协议适配器决定是否覆盖该能力）。
     */
    public DeviceIoResult executeDeviceIoWithNewConnection(DeviceIoRequest request) {
        return deviceTransportRegistry.executeWithNewConnection(request);
    }

    /**
     * 执行 IO 并按结果更新 {@code CommandExecution} 状态。
     */
    public void dispatchAndUpdateExecution(Long warehouseId, Long executionId, DeviceIoRequest request) {
        if (warehouseId == null || executionId == null) {
            throw new IllegalArgumentException("warehouseId and executionId required");
        }
        DeviceIoResult result = deviceTransportRegistry.execute(request);
        if (result.isSuccess()) {
            commandExecutionService.updateStatus(warehouseId, executionId,
                DomainEnums.CommandStatus.DONE.name(), result.getResponseJson(), null, null);
        } else {
            commandExecutionService.updateStatus(warehouseId, executionId,
                DomainEnums.CommandStatus.ERROR.name(), result.getResponseJson(),
                result.getErrorCode(), result.getErrorMessage());
        }
    }
}
