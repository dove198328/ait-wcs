package cn.aitplus.wcs.infra.service.task.impl;

import cn.aitplus.wcs.core.domain.model.WorkflowDefinition;
import cn.aitplus.wcs.infra.persistence.task.WorkflowDefinitionsMapper;
import cn.aitplus.wcs.infra.service.task.WorkflowDefinitionsService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkflowDefinitionsServiceImpl implements WorkflowDefinitionsService {

    private final WorkflowDefinitionsMapper workflowDefinitionsMapper;

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
    public WorkflowDefinition findByBizType(Long wareHouseId, String bizType) {
        return workflowDefinitionsMapper.findByBizType(wareHouseId, bizType);
    }

    @Override
    public WorkflowDefinition queryByWorkflowId(Long wareHouseId, String workflowId) {
        return workflowDefinitionsMapper.queryByWorkflowId(wareHouseId, workflowId);
    }

    @Override
    public String getFirstSubDefString(Long wareHouseId, String workflowId) {
        return workflowDefinitionsMapper.getFirstSubDefString(wareHouseId, workflowId);
    }
}
