package cn.aitplus.wcs.infra.service.device.impl;

import cn.aitplus.wcs.core.domain.model.TaskDefinition;
import cn.aitplus.wcs.core.domain.model.WorkflowDefinition;
import cn.aitplus.wcs.infra.persistence.task.WorkflowDefinitionsMapper;
import cn.aitplus.wcs.infra.service.device.DeviceConfigService;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static cn.aitplus.wcs.common.constant.WcsConstants.*;


/**
 * 设备配置服务实现类
 * 使用Redis存储设备配置信息
 */
@Service
@Slf4j
@Transactional
public class DeviceConfigServiceImpl implements DeviceConfigService {
    
    private final RedisTemplate<String, String> redisTemplate;

    private final WorkflowDefinitionsMapper workflowDefinitionsMapper;

    @Autowired
    public DeviceConfigServiceImpl(RedisTemplate<String, String> redisTemplate,
                                   WorkflowDefinitionsMapper workflowDefinitionsMapper) {
        this.redisTemplate = redisTemplate;
        this.workflowDefinitionsMapper = workflowDefinitionsMapper;
    }

    /**
     * 获取设备流程定义的Redis键
     * @param warehouseId 仓库ID
     * @param workflowDefId 工作流定义ID
     * @return Redis键
     */
    private String getDeviceProcessKey(Long warehouseId, String workflowDefId) {
        return String.format(DEVICE_PROCESS_KEY_FORMAT, warehouseId) + workflowDefId;
    }

    /**
     * 获取设备流程定义
     * @param warehouseId 仓库ID
     * @param workflowDefId 工作流定义ID
     * @return 设备流程定义
     */
    @Override
    public TaskDefinition getDeviceProcess(Long warehouseId, String workflowDefId) {
        if (workflowDefId == null || workflowDefId.isEmpty() || warehouseId == null) {
            return null;
        }
        try {
            // 从Redis缓存中获取
            String redisKey = getDeviceProcessKey(warehouseId, workflowDefId);
            TaskDefinition taskDefinition =  JSONObject.parseObject(redisTemplate.opsForValue().get(redisKey), TaskDefinition.class);
            // 如果Redis中不存在，尝试从数据库获取
            if (taskDefinition == null) {
                WorkflowDefinition workflowDefinition = workflowDefinitionsMapper.queryByWorkflowId(warehouseId,workflowDefId);
                taskDefinition = TaskDefinition.builder()
                        .workflowDefId(workflowDefinition.getWorkflowId())
                        .warehouseId(workflowDefinition.getWarehouseId())
                        .subtasks(workflowDefinition.getSubtaskDefinitions())
                        .build();
            }
            return taskDefinition;
        } catch (Exception e) {
            log.error("获取设备流程定义失败: {}", workflowDefId, e);
            return null;
        }
    }
}