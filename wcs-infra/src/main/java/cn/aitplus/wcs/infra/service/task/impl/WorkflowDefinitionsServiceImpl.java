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
 * 流程定义：读路径 {@link Cached}；写路径通过 {@link CacheInvalidate} 淘汰相关 key，下次读再加载（与「远程可长期驻留、靠业务失效」一致）。
 * <p>
 * 一级/二级过期：{@code @Cached} 上 {@code localExpire}（秒）控制本地；{@code expire}（秒）控制远程，不设则走 {@code application.yml} 默认。
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
            name = WcsConstants.WORKFLOW_BY_ID_CACHE_NAME,
            key = "#warehouseId + ':' + #workflowDefinitionId",
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
    @CacheInvalidate(name = WcsConstants.WORKFLOW_BY_ID_CACHE_NAME,
            key = "#cacheKeySnapshotBeforeUpdate.warehouseId + ':' + #cacheKeySnapshotBeforeUpdate.id",
            condition = "#cacheKeySnapshotBeforeUpdate != null && #cacheKeySnapshotBeforeUpdate.id != null && #cacheKeySnapshotBeforeUpdate.warehouseId != null")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_BY_BIZ_TYPE_CACHE_NAME,
            key = "#cacheKeySnapshotBeforeUpdate.warehouseId + ':' + #cacheKeySnapshotBeforeUpdate.bizType",
            condition = "#cacheKeySnapshotBeforeUpdate != null && #cacheKeySnapshotBeforeUpdate.warehouseId != null && T(org.springframework.util.StringUtils).hasText(#cacheKeySnapshotBeforeUpdate.bizType)")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_BY_NAME_CACHE_NAME,
            key = "#cacheKeySnapshotBeforeUpdate.warehouseId + ':' + #cacheKeySnapshotBeforeUpdate.name",
            condition = "#cacheKeySnapshotBeforeUpdate != null && #cacheKeySnapshotBeforeUpdate.warehouseId != null && T(org.springframework.util.StringUtils).hasText(#cacheKeySnapshotBeforeUpdate.name)")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_BY_WORKFLOW_ID_CACHE_NAME,
            key = "#cacheKeySnapshotBeforeUpdate.warehouseId + ':' + #cacheKeySnapshotBeforeUpdate.workflowId",
            condition = "#cacheKeySnapshotBeforeUpdate != null && #cacheKeySnapshotBeforeUpdate.warehouseId != null && T(org.springframework.util.StringUtils).hasText(#cacheKeySnapshotBeforeUpdate.workflowId)")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_BY_ID_CACHE_NAME,
            key = "#persisted.warehouseId + ':' + #persisted.id",
            condition = "#persisted != null && #persisted.id != null && #persisted.warehouseId != null")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_BY_BIZ_TYPE_CACHE_NAME,
            key = "#persisted.warehouseId + ':' + #persisted.bizType",
            condition = "#persisted != null && #persisted.warehouseId != null && T(org.springframework.util.StringUtils).hasText(#persisted.bizType)")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_BY_NAME_CACHE_NAME,
            key = "#persisted.warehouseId + ':' + #persisted.name",
            condition = "#persisted != null && #persisted.warehouseId != null && T(org.springframework.util.StringUtils).hasText(#persisted.name)")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_BY_WORKFLOW_ID_CACHE_NAME,
            key = "#persisted.warehouseId + ':' + #persisted.workflowId",
            condition = "#persisted != null && #persisted.warehouseId != null && T(org.springframework.util.StringUtils).hasText(#persisted.workflowId)")
    public void updateDefinition(WorkflowDefinition persisted, WorkflowDefinition cacheKeySnapshotBeforeUpdate) {
        workflowDefinitionsMapper.updateById(persisted);
    }

    @Override
    @CacheInvalidate(name = WcsConstants.WORKFLOW_BY_ID_CACHE_NAME,
            key = "#loaded.warehouseId + ':' + #loaded.id",
            condition = "#loaded != null && #loaded.id != null && #loaded.warehouseId != null")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_BY_BIZ_TYPE_CACHE_NAME,
            key = "#loaded.warehouseId + ':' + #loaded.bizType",
            condition = "#loaded != null && #loaded.warehouseId != null && T(org.springframework.util.StringUtils).hasText(#loaded.bizType)")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_BY_NAME_CACHE_NAME,
            key = "#loaded.warehouseId + ':' + #loaded.name",
            condition = "#loaded != null && #loaded.warehouseId != null && T(org.springframework.util.StringUtils).hasText(#loaded.name)")
    @CacheInvalidate(name = WcsConstants.WORKFLOW_BY_WORKFLOW_ID_CACHE_NAME,
            key = "#loaded.warehouseId + ':' + #loaded.workflowId",
            condition = "#loaded != null && #loaded.warehouseId != null && T(org.springframework.util.StringUtils).hasText(#loaded.workflowId)")
    public void deleteDefinitionPhysical(WorkflowDefinition loaded) {
        int deletedRows = workflowDefinitionsMapper.deleteById(loaded.getId());
        if (deletedRows <= 0) {
            throw new IllegalStateException("流程定义删除失败");
        }
    }

    @Override
    @Cached(
            name = WcsConstants.WORKFLOW_BY_BIZ_TYPE_CACHE_NAME,
            key = "#wareHouseId + ':' + #bizType",
            cacheType = CacheType.BOTH,
            localExpire = 300,
            syncLocal = true)
    public WorkflowDefinition findByBizType(Long wareHouseId, String bizType) {
        return workflowDefinitionsMapper.findByBizType(wareHouseId, bizType);
    }

    @Override
    @Cached(
            name = WcsConstants.WORKFLOW_BY_NAME_CACHE_NAME,
            key = "#wareHouseId + ':' + #name",
            cacheType = CacheType.BOTH,
            localExpire = 300,
            syncLocal = true)
    public WorkflowDefinition findByName(Long wareHouseId, String name) {
        return workflowDefinitionsMapper.findByName(wareHouseId, name);
    }

    @Override
    @Cached(
            name = WcsConstants.WORKFLOW_BY_WORKFLOW_ID_CACHE_NAME,
            key = "#wareHouseId + ':' + #workflowId",
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
