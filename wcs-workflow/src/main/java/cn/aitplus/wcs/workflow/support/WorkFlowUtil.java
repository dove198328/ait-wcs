package cn.aitplus.wcs.workflow.support;

import cn.aitplus.wcs.utils.SpringUtils;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

import java.util.Iterator;

public class WorkFlowUtil {

    /**
     * 仅持有 deploymentId 时使用；若已查询到 {@link ProcessDefinition#getId()}，请用
     * {@link #getFirstSubTaskDefIdByProcessDefinitionId(String)}，避免重复查库。
     */
    public static String getFirstSubTaskDefId(String deployId) {
        return getFirstSubTaskDefIdByProcessDefinitionId(getProcessDefId(deployId));
    }

    public static String getFirstSubTaskDefIdByProcessDefinitionId(String processDefinitionId) {
        RepositoryService repositoryService = SpringUtils.getBean(RepositoryService.class);
        BpmnModelInstance model = repositoryService.getBpmnModelInstance(processDefinitionId);
        return getFirstSubtaskDefIdByModel(model);
    }

    public static String getProcessDefId(String deployId){
        RepositoryService repositoryService = SpringUtils.getBean(RepositoryService.class);
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery().deploymentId(deployId)
                .singleResult();
        return processDefinition.getId();
    }

    public static String getFirstSubtaskDefIdByModel(BpmnModelInstance model){
        // 3. 获取StartEvent
        StartEvent startEvent = model
                .getModelElementsByType(StartEvent.class)
                .iterator()
                .next();
        //该方法用于处理不存在outgoing时获取第一个子任务ID
        Iterator<SequenceFlow> iterator = model.getModelElementsByType(SequenceFlow.class).iterator();
        String firstSubtaskDefId = null;
        while (iterator.hasNext()) {
            SequenceFlow sequenceFlow = iterator.next();
            if (sequenceFlow.getSource().equals(startEvent)) {
                firstSubtaskDefId = sequenceFlow.getTarget().getId();
            }
        }
        return firstSubtaskDefId;
    }
}
