package cn.aitplus.wcs.workflow.service;

import cn.aitplus.wcs.common.constant.WcsConstants;
import cn.aitplus.wcs.core.domain.model.WorkflowDefinition;
import cn.aitplus.wcs.infra.service.task.TasksService;
import cn.aitplus.wcs.infra.service.task.WorkflowDefinitionsService;
import com.alibaba.fastjson2.JSON;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;

@Service
public class WorkflowDefinitionCommandService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowDefinitionCommandService.class);

    private final TasksService tasksService;
    private final WorkflowDefinitionsService workflowDefinitionsService;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;

    public WorkflowDefinitionCommandService(TasksService tasksService,
                                            WorkflowDefinitionsService workflowDefinitionsService,
                                            RepositoryService repositoryService,
                                            RuntimeService runtimeService) {
        this.tasksService = tasksService;
        this.workflowDefinitionsService = workflowDefinitionsService;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition createAndDeploy(Long wareHouseId, WorkflowDefinition workflowDefinition) {
        if (wareHouseId == null) {
            throw new IllegalArgumentException("wareHouseId不能为空");
        }
        if (workflowDefinition == null) {
            throw new IllegalArgumentException("流程定义不能为空");
        }

        workflowDefinition.setWarehouseId(wareHouseId.intValue());
        validateCreateRequest(wareHouseId, workflowDefinition);
        validateProcessDataJson(workflowDefinition.getProcessData());
        parseAndValidateBpmn(workflowDefinition.getWorkflowId(), workflowDefinition.getConfig());

        Deployment deployment = deployBpmn(wareHouseId, workflowDefinition);
        try {
            ProcessDefinition processDefinition = queryDeployedProcessDefinition(wareHouseId, deployment, workflowDefinition.getWorkflowId());
            workflowDefinition.setDeployId(deployment.getId());
            workflowDefinition.setProcessDefId(processDefinition.getId());
            LocalDateTime now = LocalDateTime.now();
            workflowDefinition.setCreatedAt(now);
            workflowDefinition.setUpdatedAt(now);
            workflowDefinitionsService.insertDefinition(workflowDefinition);
            return workflowDefinition;
        } catch (RuntimeException ex) {
            safeDeleteDeployment(deployment.getId(), ex);
            throw ex;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition updateAndRedeploy(Long wareHouseId, Long workflowDefinitionId, WorkflowDefinition workflowDefinition) {
        if (wareHouseId == null) {
            throw new IllegalArgumentException("wareHouseId不能为空");
        }
        if (workflowDefinitionId == null) {
            throw new IllegalArgumentException("流程定义ID不能为空");
        }
        if (workflowDefinition == null) {
            throw new IllegalArgumentException("流程定义不能为空");
        }

        WorkflowDefinition existing = workflowDefinitionsService.queryById(wareHouseId, workflowDefinitionId);
        if (existing == null) {
            throw new IllegalArgumentException("流程定义不存在");
        }
        if (hasActiveTaskReferences(wareHouseId, existing.getWorkflowId())
                || hasActiveProcessInstances(wareHouseId, existing.getProcessDefId())) {
            throw new IllegalArgumentException("当前流程定义存在运行中的任务或流程实例，不允许更新");
        }

        workflowDefinition.setId(workflowDefinitionId);
        workflowDefinition.setWarehouseId(wareHouseId.intValue());
        validateUpdateRequest(wareHouseId, existing, workflowDefinition);
        validateProcessDataJson(workflowDefinition.getProcessData());
        parseAndValidateBpmn(workflowDefinition.getWorkflowId(), workflowDefinition.getConfig());
        WorkflowDefinition oldSnapshot = copyWorkflowDefinition(existing);

        Deployment deployment = deployBpmn(wareHouseId, workflowDefinition);
        try {
            ProcessDefinition processDefinition = queryDeployedProcessDefinition(wareHouseId, deployment, workflowDefinition.getWorkflowId());
            mergeUpdatableFields(existing, workflowDefinition);
            existing.setDeployId(deployment.getId());
            existing.setProcessDefId(processDefinition.getId());
            existing.setUpdatedAt(LocalDateTime.now());
            workflowDefinitionsService.updateDefinition(existing, oldSnapshot);
            return existing;
        } catch (RuntimeException ex) {
            safeDeleteDeployment(deployment.getId(), ex);
            throw ex;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePhysically(Long wareHouseId, Long workflowDefinitionId) {
        if (wareHouseId == null) {
            throw new IllegalArgumentException("wareHouseId不能为空");
        }
        if (workflowDefinitionId == null) {
            throw new IllegalArgumentException("流程定义ID不能为空");
        }

        WorkflowDefinition existing = workflowDefinitionsService.queryById(wareHouseId, workflowDefinitionId);
        if (existing == null) {
            throw new IllegalArgumentException("流程定义不存在");
        }
        if (hasActiveTaskReferences(wareHouseId, existing.getWorkflowId())
                || hasActiveProcessInstances(wareHouseId, existing.getProcessDefId())) {
            throw new IllegalArgumentException("当前流程定义存在运行中的任务或流程实例，不允许删除");
        }

        workflowDefinitionsService.deleteDefinitionPhysical(existing);
        deleteDeploymentIfPresent(existing.getDeployId());
    }

    private void validateCreateRequest(Long wareHouseId, WorkflowDefinition workflowDefinition) {
        if (!StringUtils.hasText(workflowDefinition.getBizType())) {
            throw new IllegalArgumentException("bizType不能为空");
        }
        if (!StringUtils.hasText(workflowDefinition.getName())) {
            throw new IllegalArgumentException("流程名称不能为空");
        }
        if (!StringUtils.hasText(workflowDefinition.getWorkflowId())) {
            throw new IllegalArgumentException("workflowId不能为空");
        }
        if (!StringUtils.hasText(workflowDefinition.getConfig())) {
            throw new IllegalArgumentException("BPMN XML不能为空");
        }
        if (workflowDefinitionsService.findByBizType(wareHouseId, workflowDefinition.getBizType()) != null) {
            throw new IllegalArgumentException("当前仓库下该bizType已存在流程定义");
        }
        if (workflowDefinitionsService.findByName(wareHouseId, workflowDefinition.getName()) != null) {
            throw new IllegalArgumentException("当前仓库下该名称已存在流程定义");
        }
    }

    private void validateUpdateRequest(Long wareHouseId, WorkflowDefinition existing, WorkflowDefinition workflowDefinition) {
        if (!StringUtils.hasText(workflowDefinition.getBizType())) {
            throw new IllegalArgumentException("bizType不能为空");
        }
        if (!StringUtils.hasText(workflowDefinition.getName())) {
            throw new IllegalArgumentException("流程名称不能为空");
        }
        if (!StringUtils.hasText(workflowDefinition.getWorkflowId())) {
            throw new IllegalArgumentException("workflowId不能为空");
        }
        if (!StringUtils.hasText(workflowDefinition.getConfig())) {
            throw new IllegalArgumentException("BPMN XML不能为空");
        }
        if (!existing.getBizType().equals(workflowDefinition.getBizType())
                && hasAnyTaskReferences(wareHouseId, existing.getWorkflowId())) {
            throw new IllegalArgumentException("当前流程定义已有任务数据引用，不允许修改bizType");
        }
        if (!existing.getWorkflowId().equals(workflowDefinition.getWorkflowId())
                && hasAnyTaskReferences(wareHouseId, existing.getWorkflowId())) {
            throw new IllegalArgumentException("当前流程定义已有任务数据引用，不允许修改workflowId");
        }

        WorkflowDefinition sameBizType = workflowDefinitionsService.findByBizType(wareHouseId, workflowDefinition.getBizType());
        if (sameBizType != null && !sameBizType.getId().equals(existing.getId())) {
            throw new IllegalArgumentException("当前仓库下该bizType已存在流程定义");
        }
        WorkflowDefinition sameName = workflowDefinitionsService.findByName(wareHouseId, workflowDefinition.getName());
        if (sameName != null && !sameName.getId().equals(existing.getId())) {
            throw new IllegalArgumentException("当前仓库下该名称已存在流程定义");
        }
    }

    private void validateProcessDataJson(String processData) {
        if (!StringUtils.hasText(processData)) {
            return;
        }
        try {
            JSON.parse(processData);
        } catch (Exception ex) {
            throw new IllegalArgumentException("processData不是合法的JSON格式");
        }
    }

    private void parseAndValidateBpmn(String workflowId, String bpmnXml) {
        try {
            BpmnModelInstance modelInstance = Bpmn.readModelFromStream(
                    new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));
            Bpmn.validateModel(modelInstance);
            Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);
            if (processes == null || processes.isEmpty()) {
                throw new IllegalArgumentException("BPMN XML中未找到流程定义");
            }
            boolean matched = processes.stream()
                    .map(Process::getId)
                    .anyMatch(workflowId::equals);
            if (!matched) {
                throw new IllegalArgumentException("BPMN流程ID必须与workflowId一致");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("config不是合法的BPMN XML格式");
        }
    }

    private void mergeUpdatableFields(WorkflowDefinition target, WorkflowDefinition source) {
        target.setBizType(source.getBizType());
        target.setWorkflowId(source.getWorkflowId());
        target.setConfig(source.getConfig());
        target.setProcessData(source.getProcessData());
        target.setName(source.getName());
        target.setPriority(source.getPriority());
        target.setSubtaskDefinitions(source.getSubtaskDefinitions());
        target.setIsAutoStart(source.getIsAutoStart());
        target.setFirstSubDef(source.getFirstSubDef());
        target.setStatus(source.getStatus());
    }

    private boolean hasAnyTaskReferences(Long wareHouseId, String workflowId) {
        if (!StringUtils.hasText(workflowId)) {
            return false;
        }
        return tasksService.countTasksByWarehouseAndWorkflowDefId(wareHouseId, workflowId) > 0;
    }

    private boolean hasActiveTaskReferences(Long wareHouseId, String workflowId) {
        if (!StringUtils.hasText(workflowId)) {
            return false;
        }
        return tasksService.countActiveTasksByWarehouseAndWorkflowDefId(wareHouseId, workflowId) > 0;
    }

    private boolean hasActiveProcessInstances(Long wareHouseId, String processDefId) {
        if (!StringUtils.hasText(processDefId)) {
            return false;
        }
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionId(processDefId)
                .tenantIdIn(String.valueOf(wareHouseId))
                .active()
                .count() > 0;
    }

    private Deployment deployBpmn(Long wareHouseId, WorkflowDefinition workflowDefinition) {
        return repositoryService.createDeployment()
                .name(workflowDefinition.getName())
                .source(WcsConstants.DEPLOYMENT_SOURCE)
                .tenantId(String.valueOf(wareHouseId))
                .addString(workflowDefinition.getWorkflowId() + ".bpmn", workflowDefinition.getConfig())
                .deploy();
    }

    private ProcessDefinition queryDeployedProcessDefinition(Long wareHouseId, Deployment deployment, String workflowId) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .processDefinitionKey(workflowId)
                .tenantIdIn(String.valueOf(wareHouseId))
                .latestVersion()
                .singleResult();
        if (processDefinition == null) {
            throw new IllegalStateException("流程定义部署成功，但未查询到processDefinition");
        }
        return processDefinition;
    }

    private void deleteDeploymentIfPresent(String deployId) {
        if (!StringUtils.hasText(deployId)) {
            return;
        }
        repositoryService.deleteDeployment(deployId, true);
    }

    private WorkflowDefinition copyWorkflowDefinition(WorkflowDefinition source) {
        return WorkflowDefinition.builder()
                .id(source.getId())
                .bizType(source.getBizType())
                .workflowId(source.getWorkflowId())
                .config(source.getConfig())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .processData(source.getProcessData())
                .name(source.getName())
                .priority(source.getPriority())
                .warehouseId(source.getWarehouseId())
                .subtaskDefinitions(source.getSubtaskDefinitions())
                .isAutoStart(source.getIsAutoStart())
                .firstSubDef(source.getFirstSubDef())
                .status(source.getStatus())
                .processDefId(source.getProcessDefId())
                .taskDefinition(source.getTaskDefinition())
                .workDirection(source.getWorkDirection())
                .deployId(source.getDeployId())
                .build();
    }

    private void safeDeleteDeployment(String deployId, RuntimeException originalEx) {
        try {
            repositoryService.deleteDeployment(deployId, true);
        } catch (Exception rollbackEx) {
            originalEx.addSuppressed(rollbackEx);
            log.warn("删除Camunda deployment失败, deployId={}", deployId, rollbackEx);
        }
    }
}
