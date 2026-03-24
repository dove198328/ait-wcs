package cn.aitplus.wcs.workflow.service;

import cn.aitplus.wcs.common.constant.WcsConstants;
import cn.aitplus.wcs.core.domain.model.TaskDefinition;
import cn.aitplus.wcs.core.domain.model.WorkflowDefinition;
import cn.aitplus.wcs.infra.service.task.TasksService;
import cn.aitplus.wcs.infra.service.task.WorkflowDefinitionsService;
import cn.aitplus.wcs.workflow.support.WarehouseTenantSupport;
import cn.aitplus.wcs.workflow.support.WorkFlowUtil;
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
            workflowDefinition.setFirstSubDef(
                    WorkFlowUtil.getFirstSubTaskDefIdByProcessDefinitionId(processDefinition.getId()));
            LocalDateTime now = LocalDateTime.now();
            workflowDefinition.setCreatedAt(now);
            workflowDefinition.setUpdatedAt(now);
            syncSubtaskDefinitionsFromTaskDefinition(workflowDefinition);
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

        WorkflowDefinition beforePatch = snapshotForUpdateValidation(existing);
        applyNonNullPatch(existing, workflowDefinition);

        validateUpdateRequest(warehouseId, beforePatch, existing);
        validateProcessDataJson(existing.getProcessData());
        parseAndValidateBpmn(existing.getWorkflowId(), existing.getConfig());

        Deployment deployment = deployBpmn(warehouseId, existing);
        try {
            ProcessDefinition processDefinition =
                    queryDeployedProcessDefinition(warehouseId, deployment, existing.getWorkflowId());
            syncSubtaskDefinitionsFromTaskDefinition(existing);
            existing.setDeployId(deployment.getId());
            existing.setProcessDefId(processDefinition.getId());
            existing.setFirstSubDef(
                    WorkFlowUtil.getFirstSubTaskDefIdByProcessDefinitionId(processDefinition.getId()));
            existing.setUpdatedAt(LocalDateTime.now());
            workflowDefinitionsService.updateDefinition(existing, beforePatch);
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

    /**
     * @param beforePatch 合并请求体之前库里的状态（用于 bizType/workflowId 变更规则）
     * @param merged      请求体中非 null 字段已覆盖到实体后的状态，校验与部署均以此为依据
     */
    private void validateUpdateRequest(Long warehouseId,
                                       WorkflowDefinition beforePatch,
                                       WorkflowDefinition merged) {
        if (!StringUtils.hasText(merged.getBizType())) {
            throw new IllegalArgumentException("bizType 不能为空");
        }
        if (!StringUtils.hasText(merged.getName())) {
            throw new IllegalArgumentException("流程名称不能为空");
        }
        if (!StringUtils.hasText(merged.getWorkflowId())) {
            throw new IllegalArgumentException("workflowId 不能为空");
        }
        if (!StringUtils.hasText(merged.getConfig())) {
            throw new IllegalArgumentException("BPMN XML 不能为空");
        }
        if (!beforePatch.getBizType().equals(merged.getBizType())
                && hasAnyTaskReferences(warehouseId, beforePatch.getWorkflowId())) {
            throw new IllegalArgumentException("当前流程定义已有任务数据引用，不允许修改 bizType");
        }
        if (!beforePatch.getWorkflowId().equals(merged.getWorkflowId())
                && hasAnyTaskReferences(warehouseId, beforePatch.getWorkflowId())) {
            throw new IllegalArgumentException("当前流程定义已有任务数据引用，不允许修改 workflowId");
        }

        WorkflowDefinition sameBizType =
                workflowDefinitionsService.findByBizType(warehouseId, merged.getBizType());
        if (sameBizType != null && !sameBizType.getId().equals(beforePatch.getId())) {
            throw new IllegalArgumentException("当前仓库下该 bizType 已存在流程定义");
        }
        WorkflowDefinition sameName =
                workflowDefinitionsService.findByName(warehouseId, merged.getName());
        if (sameName != null && !sameName.getId().equals(beforePatch.getId())) {
            throw new IllegalArgumentException("当前仓库下该名称已存在流程定义");
        }
    }

    /** 仅拷贝更新校验所需的旧值，避免与合并后的实体混淆引用。 */
    /**
     * 合并请求体之前在库中的字段：用于 bizType/workflowId 变更校验，以及缓存失效时的「更新前」键（id/wf/biz/name）。
     */
    private static WorkflowDefinition snapshotForUpdateValidation(WorkflowDefinition row) {
        WorkflowDefinition s = new WorkflowDefinition();
        s.setId(row.getId());
        s.setWarehouseId(row.getWarehouseId());
        s.setBizType(row.getBizType());
        s.setName(row.getName());
        s.setWorkflowId(row.getWorkflowId());
        return s;
    }

    /**
     * 请求体里「有传」的字段才覆盖到实体：JSON 缺省一般为 null，不覆盖库里的值。
     * {@code isAutoStart}/{@code status} 为基本类型，反序列化后无法区分缺省与 0，仍始终随请求覆盖。
     */
    private static void applyNonNullPatch(WorkflowDefinition target, WorkflowDefinition patch) {
        if (patch.getBizType() != null) {
            target.setBizType(patch.getBizType());
        }
        if (patch.getWorkflowId() != null) {
            target.setWorkflowId(patch.getWorkflowId());
        }
        if (patch.getConfig() != null) {
            target.setConfig(patch.getConfig());
        }
        if (patch.getProcessData() != null) {
            target.setProcessData(patch.getProcessData());
        }
        if (patch.getName() != null) {
            target.setName(patch.getName());
        }
        if (patch.getPriority() != null) {
            target.setPriority(patch.getPriority());
        }
        if (patch.getTaskDefinition() != null) {
            target.setTaskDefinition(patch.getTaskDefinition());
        }
        if (patch.getSubtaskDefinitions() != null) {
            target.setSubtaskDefinitions(patch.getSubtaskDefinitions());
        }
        target.setIsAutoStart(patch.getIsAutoStart());
        target.setStatus(patch.getStatus());
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

    /** 有 {@link TaskDefinition} 时用其 {@code subtasks} 写入持久化字段 {@code subtaskDefinitions}。 */
    private void syncSubtaskDefinitionsFromTaskDefinition(WorkflowDefinition def) {
        if (def == null) {
            return;
        }
        TaskDefinition taskDefinition = def.getTaskDefinition();
        if (taskDefinition == null) {
            return;
        }
        def.setSubtaskDefinitions(taskDefinition.getSubtasks());
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

    private void safeDeleteDeployment(String deployId, RuntimeException originalEx) {
        try {
            repositoryService.deleteDeployment(deployId, true);
        } catch (Exception rollbackEx) {
            originalEx.addSuppressed(rollbackEx);
            log.warn("删除 Camunda deployment 失败, deployId={}", deployId, rollbackEx);
        }
    }
}
