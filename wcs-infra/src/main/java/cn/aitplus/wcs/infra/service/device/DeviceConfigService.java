package cn.aitplus.wcs.infra.service.device;


import cn.aitplus.wcs.core.domain.model.TaskDefinition;


/**
 * 设备配置服务接口
 * 负责设备配置的增删改查
 */
public interface DeviceConfigService {
    /**
     * 获取设备流程定义
     * @param warehouseId 仓库ID
     * @param workflowDefId 工作流定义ID
     * @return 设备流程定义
     */
    TaskDefinition getDeviceProcess(Long warehouseId, String workflowDefId);

}