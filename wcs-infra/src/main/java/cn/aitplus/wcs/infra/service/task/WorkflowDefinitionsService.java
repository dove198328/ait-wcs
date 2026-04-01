package cn.aitplus.wcs.infra.service.task;

import cn.aitplus.wcs.core.domain.model.workflow.WorkflowDefinition;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface WorkflowDefinitionsService {

    IPage<WorkflowDefinition> queryByPage(Long wareHouseId, IPage<WorkflowDefinition> page, WorkflowDefinition workflowDefinition);

    List<WorkflowDefinition> queryList(Long wareHouseId, WorkflowDefinition workflowDefinition);

    /**
     * 按主键查询；缓存键包含 warehouseId（租户），调用方须传入路径上的仓库 ID。
     */
    WorkflowDefinition queryById(Long warehouseId, Long workflowDefinitionId);

    /**
     * 新增流程定义（仅持久化）；缓存刷新在事务提交后由本服务完成。
     */
    void insertDefinition(WorkflowDefinition definition);

    /**
     * 更新流程定义；按 {@code cacheKeySnapshotBeforeUpdate} 与 {@code persisted} 各淘汰一遍缓存键，
     * 避免 bizType/name/workflowId 变更后旧键残留。
     */
    void updateDefinition(WorkflowDefinition persisted, WorkflowDefinition cacheKeySnapshotBeforeUpdate);

    /**
     * 物理删除；{@code loaded} 须已校验租户与存在性。
     */
    void deleteDefinitionPhysical(WorkflowDefinition loaded);

    WorkflowDefinition findByBizType(Long wareHouseId, String bizType);

    WorkflowDefinition findByName(Long wareHouseId, String name);

    WorkflowDefinition queryByWorkflowId(Long wareHouseId, String workflowId);

    String getFirstSubDefString(Long wareHouseId, String workflowId);
}
