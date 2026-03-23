package cn.aitplus.wcs.infra.service.task;

import cn.aitplus.wcs.core.domain.model.WorkflowDefinition;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface WorkflowDefinitionsService {

    IPage<WorkflowDefinition> queryByPage(Long wareHouseId, IPage<WorkflowDefinition> page, WorkflowDefinition workflowDefinition);

    List<WorkflowDefinition> queryList(Long wareHouseId, WorkflowDefinition workflowDefinition);

    WorkflowDefinition findByBizType(Long wareHouseId, String bizType);

    WorkflowDefinition queryByWorkflowId(Long wareHouseId, String workflowId);

    String getFirstSubDefString(Long wareHouseId, String workflowId);
}
