package cn.aitplus.wcs.infra.service.task.impl;

import cn.aitplus.wcs.common.constant.WcsConstants;
import cn.aitplus.wcs.core.domain.model.WorkflowDefinition;
import cn.aitplus.wcs.infra.persistence.task.WorkflowDefinitionsMapper;
import cn.aitplus.wcs.infra.service.task.WorkflowDefinitionsService;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程定义：统一 {@link WcsConstants#WORKFLOW_DEFINITION_CACHE_NAME}，用 key 前缀区分主键 / workflowId / bizType / name。
 */
@Service
public class WorkflowDefinitionsServiceImpl implements WorkflowDefinitionsService {

    private final WorkflowDefinitionsMapper workflowDefinitionsMapper;

    @Autowired
    @Lazy
    private WorkflowDefinitionsService self;

    public WorkflowDefinitionsServiceImpl(WorkflowDefinitionsMapper workflowDefinitionsMapper) {
        this.workflowDefinitionsMapper = workflowDefinitionsMapper;
    }

    @Override
    public IPage<WorkflowDefinition> queryByPage(Long wareHouseId, IPage<WorkflowDefinition> page, WorkflowDefinition workflowDefinition) {
        return workflowDefinitionsMapper.queryByPage(page, workflowDefinition);
    }

    @Override
    public List<WorkflowDefinition> queryList(Long wareHouseId, WorkflowDefinition workflowDefinition) {
        return workflowDefinitionsMapper.queryList(workflowDefinition);
    }

    @Override
    @Cached(
            name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'id:' + #warehouseId + ':' + #workflowDefinitionId",
            cacheType = CacheType.BOTH,
            localExpire = 300,
            syncLocal = true)
    public WorkflowDefinition queryById(Long warehouseId, Long workflowDefinitionId) {
        if (warehouseId == null || workflowDefinitionId == null) {
            return null;
        }
        WorkflowDefinition w = workflowDefinitionsMapper.selectById(workflowDefinitionId);
        if (w == null || w.getWarehouseId() == null || !warehouseId.equals(w.getWarehouseId().longValue())) {
            return null;
        }
        return w;
    }

    @Override
    public void insertDefinition(WorkflowDefinition definition) {
        workflowDefinitionsMapper.insert(definition);
    }

    @Override
    @CacheInvalidate(name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'id:' + #cacheKeySnapshotBeforeUpdate.warehouseId + ':' + #cacheKeySnapshotBeforeUpdate.id")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'wf:' + #cacheKeySnapshotBeforeUpdate.warehouseId + ':' + #cacheKeySnapshotBeforeUpdate.workflowId")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'biz:' + #cacheKeySnapshotBeforeUpdate.warehouseId + ':' + #cacheKeySnapshotBeforeUpdate.bizType")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'name:' + #cacheKeySnapshotBeforeUpdate.warehouseId + ':' + #cacheKeySnapshotBeforeUpdate.name")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'id:' + #persisted.warehouseId + ':' + #persisted.id")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'wf:' + #persisted.warehouseId + ':' + #persisted.workflowId")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'biz:' + #persisted.warehouseId + ':' + #persisted.bizType")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'name:' + #persisted.warehouseId + ':' + #persisted.name")
    public void updateDefinition(WorkflowDefinition persisted, WorkflowDefinition cacheKeySnapshotBeforeUpdate) {
        workflowDefinitionsMapper.updateById(persisted);
    }

    @Override
    @CacheInvalidate(name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'id:' + #loaded.warehouseId + ':' + #loaded.id")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'wf:' + #loaded.warehouseId + ':' + #loaded.workflowId")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'biz:' + #loaded.warehouseId + ':' + #loaded.bizType")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'name:' + #loaded.warehouseId + ':' + #loaded.name")
    public void deleteDefinitionPhysical(WorkflowDefinition loaded) {
        int deletedRows = workflowDefinitionsMapper.deleteById(loaded.getId());
        if (deletedRows <= 0) {
            throw new IllegalStateException("流程定义删除失败");
        }
    }

    @Override
    @Cached(
            name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'biz:' + #wareHouseId + ':' + #bizType",
            cacheType = CacheType.BOTH,
            localExpire = 300,
            syncLocal = true)
    public WorkflowDefinition findByBizType(Long wareHouseId, String bizType) {
        return workflowDefinitionsMapper.findByBizType(wareHouseId, bizType);
    }

    @Override
    @Cached(
            name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'name:' + #wareHouseId + ':' + #name",
            cacheType = CacheType.BOTH,
            localExpire = 300,
            syncLocal = true)
    public WorkflowDefinition findByName(Long wareHouseId, String name) {
        return workflowDefinitionsMapper.findByName(wareHouseId, name);
    }

    @Override
    @Cached(
            name = WcsConstants.WORKFLOW_DEFINITION_CACHE_NAME,
            key = "'wf:' + #wareHouseId + ':' + #workflowId",
            cacheType = CacheType.BOTH,
            localExpire = 300,
            syncLocal = true)
    public WorkflowDefinition queryByWorkflowId(Long wareHouseId, String workflowId) {
        return workflowDefinitionsMapper.queryByWorkflowId(wareHouseId, workflowId);
    }

    @Override
    public String getFirstSubDefString(Long wareHouseId, String workflowId) {
        WorkflowDefinition workflowDefinition = self.queryByWorkflowId(wareHouseId, workflowId);
        return workflowDefinition == null ? null : workflowDefinition.getFirstSubDef();
    }
}
