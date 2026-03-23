package cn.aitplus.wcs.workflow.service;

import cn.aitplus.wcs.common.constant.WcsConstants;
import cn.aitplus.wcs.core.domain.model.WorkflowDefinition;
import cn.aitplus.wcs.infra.service.task.TasksService;
import cn.aitplus.wcs.infra.service.task.WorkflowDefinitionsService;
import cn.aitplus.wcs.workflow.support.WarehouseTenantSupport;
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
    private final WarehouseTenantSupport warehouseTenantSupport;

    public WorkflowDefinitionCommandService(TasksService tasksService,
                                            WorkflowDefinitionsService workflowDefinitionsService,
                                            RepositoryService repositoryService,
                                            RuntimeService runtimeService,
                                            WarehouseTenantSupport warehouseTenantSupport) {
        this.tasksService = tasksService;
        this.workflowDefinitionsService = workflowDefinitionsService;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.warehouseTenantSupport = warehouseTenantSupport;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition createAndDeploy(Long warehouseId, WorkflowDefinition workflowDefinition) {
        warehouseTenantSupport.assertAllowedWarehouse(warehouseId);
        if (workflowDefinition == null) {
            throw new IllegalArgumentException("流程定义不能为空");
        }

        workflowDefinition.setWarehouseId(warehouseId.intValue());
        validateCreateRequest(warehouseId, workflowDefinition);
        validateProcessDataJson(workflowDefinition.getProcessData());
        parseAndValidateBpmn(workflowDefinition.getWorkflowId(), workflowDefinition.getConfig());

        Deployment deployment = deployBpmn(warehouseId, workflowDefinition);
        try {
            ProcessDefinition processDefinition =
                    queryDeployedProcessDefinition(warehouseId, deployment, workflowDefinition.getWorkflowId());
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
    public WorkflowDefinition updateAndRedeploy(Long warehouseId,
                                                Long workflowDefinitionId,
                                                WorkflowDefinition workflowDefinition) {
        warehouseTenantSupport.assertAllowedWarehouse(warehouseId);
        if (workflowDefinitionId == null) {
            throw new IllegalArgumentException("流程定义 ID 不能为空");
        }
        if (workflowDefinition == null) {
            throw new IllegalArgumentException("流程定义不能为空");
        }

        WorkflowDefinition existing = workflowDefinitionsService.queryById(warehouseId, workflowDefinitionId);
        if (existing == null) {
            throw new IllegalArgumentException("流程定义不存在");
        }
        if (hasActiveTaskReferences(warehouseId, existing.getWorkflowId())
                || hasActiveProcessInstances(warehouseId, existing.getProcessDefId())) {
            throw new IllegalArgumentException("当前流程定义存在运行中的任务或流程实例，不允许更新");
        }

        workflowDefinition.setId(workflowDefinitionId);
        workflowDefinition.setWarehouseId(warehouseId.intValue());
        validateUpdateRequest(warehouseId, existing, workflowDefinition);
        validateProcessDataJson(workflowDefinition.getProcessData());
        parseAndValidateBpmn(workflowDefinition.getWorkflowId(), workflowDefinition.getConfig());
        WorkflowDefinition oldSnapshot = copyWorkflowDefinition(existing);

        Deployment deployment = deployBpmn(warehouseId, workflowDefinition);
        try {
            ProcessDefinition processDefinition =
                    queryDeployedProcessDefinition(warehouseId, deployment, workflowDefinition.getWorkflowId());
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
    public void deletePhysically(Long warehouseId, Long workflowDefinitionId) {
        warehouseTenantSupport.assertAllowedWarehouse(warehouseId);
        if (workflowDefinitionId == null) {
            throw new IllegalArgumentException("流程定义 ID 不能为空");
        }

        WorkflowDefinition existing = workflowDefinitionsService.queryById(warehouseId, workflowDefinitionId);
        if (existing == null) {
            throw new IllegalArgumentException("流程定义不存在");
        }
        if (hasActiveTaskReferences(warehouseId, existing.getWorkflowId())
                || hasActiveProcessInstances(warehouseId, existing.getProcessDefId())) {
            throw new IllegalArgumentException("当前流程定义存在运行中的任务或流程实例，不允许删除");
        }

        workflowDefinitionsService.deleteDefinitionPhysical(existing);
        deleteDeploymentIfPresent(existing.getDeployId());
    }

    private void validateCreateRequest(Long warehouseId, WorkflowDefinition workflowDefinition) {
        if (!StringUtils.hasText(workflowDefinition.getBizType())) {
            throw new IllegalArgumentException("bizType 不能为空");
        }
        if (!StringUtils.hasText(workflowDefinition.getName())) {
            throw new IllegalArgumentException("流程名称不能为空");
        }
        if (!StringUtils.hasText(workflowDefinition.getWorkflowId())) {
            throw new IllegalArgumentException("workflowId 不能为空");
        }
        if (!StringUtils.hasText(workflowDefinition.getConfig())) {
            throw new IllegalArgumentException("BPMN XML 不能为空");
        }
        if (workflowDefinitionsService.findByBizType(warehouseId, workflowDefinition.getBizType()) != null) {
            throw new IllegalArgumentException("当前仓库下该 bizType 已存在流程定义");
        }
        if (workflowDefinitionsService.findByName(warehouseId, workflowDefinition.getName()) != null) {
            throw new IllegalArgumentException("当前仓库下该名称已存在流程定义");
        }
    }

    private void validateUpdateRequest(Long warehouseId,
                                       WorkflowDefinition existing,
                                       WorkflowDefinition workflowDefinition) {
        if (!StringUtils.hasText(workflowDefinition.getBizType())) {
            throw new IllegalArgumentException("bizType 不能为空");
        }
        if (!StringUtils.hasText(workflowDefinition.getName())) {
            throw new IllegalArgumentException("流程名称不能为空");
        }
        if (!StringUtils.hasText(workflowDefinition.getWorkflowId())) {
            throw new IllegalArgumentException("workflowId 不能为空");
        }
        if (!StringUtils.hasText(workflowDefinition.getConfig())) {
            throw new IllegalArgumentException("BPMN XML 不能为空");
        }
        if (!existing.getBizType().equals(workflowDefinition.getBizType())
                && hasAnyTaskReferences(warehouseId, existing.getWorkflowId())) {
            throw new IllegalArgumentException("当前流程定义已有任务数据引用，不允许修改 bizType");
        }
        if (!existing.getWorkflowId().equals(workflowDefinition.getWorkflowId())
                && hasAnyTaskReferences(warehouseId, existing.getWorkflowId())) {
            throw new IllegalArgumentException("当前流程定义已有任务数据引用，不允许修改 workflowId");
        }

        WorkflowDefinition sameBizType =
                workflowDefinitionsService.findByBizType(warehouseId, workflowDefinition.getBizType());
        if (sameBizType != null && !sameBizType.getId().equals(existing.getId())) {
            throw new IllegalArgumentException("当前仓库下该 bizType 已存在流程定义");
        }
        WorkflowDefinition sameName =
                workflowDefinitionsService.findByName(warehouseId, workflowDefinition.getName());
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
            throw new IllegalArgumentException("processData 不是合法的 JSON 格式");
        }
    }

    private void parseAndValidateBpmn(String workflowId, String bpmnXml) {
        try {
            BpmnModelInstance modelInstance = Bpmn.readModelFromStream(
                    new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));
            Bpmn.validateModel(modelInstance);
            Collection<Process> processes = modelInstance.getModelElementsByType(Process.class);
            if (processes == null || processes.isEmpty()) {
                throw new IllegalArgumentException("BPMN XML 中未找到流程定义");
            }
            boolean matched = processes.stream()
                    .map(Process::getId)
                    .anyMatch(workflowId::equals);
            if (!matched) {
                throw new IllegalArgumentException("BPMN 流程 ID 必须与 workflowId 一致");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("config 不是合法的 BPMN XML 格式");
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

    private boolean hasAnyTaskReferences(Long warehouseId, String workflowId) {
        if (!StringUtils.hasText(workflowId)) {
            return false;
        }
        return tasksService.countTasksByWarehouseAndWorkflowDefId(warehouseId, workflowId) > 0;
    }

    private boolean hasActiveTaskReferences(Long warehouseId, String workflowId) {
        if (!StringUtils.hasText(workflowId)) {
            return false;
        }
        return tasksService.countActiveTasksByWarehouseAndWorkflowDefId(warehouseId, workflowId) > 0;
    }

    private boolean hasActiveProcessInstances(Long warehouseId, String processDefId) {
        if (!StringUtils.hasText(processDefId)) {
            return false;
        }
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionId(processDefId)
                .tenantIdIn(warehouseTenantSupport.tenantIdOf(warehouseId))
                .active()
                .count() > 0;
    }

    private Deployment deployBpmn(Long warehouseId, WorkflowDefinition workflowDefinition) {
        return repositoryService.createDeployment()
                .name(workflowDefinition.getName())
                .source(WcsConstants.DEPLOYMENT_SOURCE)
                .tenantId(warehouseTenantSupport.tenantIdOf(warehouseId))
                .addString(workflowDefinition.getWorkflowId() + ".bpmn", workflowDefinition.getConfig())
                .deploy();
    }

    private ProcessDefinition queryDeployedProcessDefinition(Long warehouseId,
                                                             Deployment deployment,
                                                             String workflowId) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .processDefinitionKey(workflowId)
                .tenantIdIn(warehouseTenantSupport.tenantIdOf(warehouseId))
                .latestVersion()
                .singleResult();
        if (processDefinition == null) {
            throw new IllegalStateException("流程定义部署成功，但未查询到 processDefinition");
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
            log.warn("删除 Camunda deployment 失败, deployId={}", deployId, rollbackEx);
        }
    }
}
