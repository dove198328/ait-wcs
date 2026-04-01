package cn.aitplus.wcs.infra.persistence.task;

import cn.aitplus.wcs.core.domain.model.workflow.WorkflowDefinition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * (WorkflowDefinitions)表数据库访问层
 *
 * @author makejava
 * @since 2025-05-14 16:00:53
 */
@Mapper
public interface WorkflowDefinitionsMapper extends BaseMapper<WorkflowDefinition>{

   IPage<WorkflowDefinition> queryByPage(@Param("page") IPage<WorkflowDefinition> page, @Param("ew") WorkflowDefinition workflowDefinitions);

   List<WorkflowDefinition> queryList(@Param("ew") WorkflowDefinition workflowDefinitions);

   WorkflowDefinition findByBizType(@Param("warehouseId") Long warehouseId, @Param("bizType") String bizType);

   WorkflowDefinition findByName(@Param("warehouseId") Long warehouseId, @Param("name") String name);

    WorkflowDefinition queryByWorkflowId(@Param("warehouseId") Long warehouseId, @Param("workflowId") String workflowId);

    String getFirstSubDefString(@Param("warehouseId") Long warehouseId, @Param("workflowId") String workflowId);

}

