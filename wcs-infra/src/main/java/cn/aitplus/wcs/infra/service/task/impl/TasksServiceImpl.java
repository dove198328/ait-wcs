package cn.aitplus.wcs.infra.service.task.impl;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.domain.enums.InstructionStatus;
import cn.aitplus.wcs.core.domain.enums.TaskStatus;
import cn.aitplus.wcs.core.domain.model.*;
import cn.aitplus.wcs.infra.service.task.SubTasksService;
import cn.aitplus.wcs.infra.service.task.WorkflowDefinitionsService;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.hutool.json.JSONObject;
import cn.aitplus.wcs.infra.persistence.task.TasksMapper;
import cn.aitplus.wcs.infra.service.task.TasksService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;


@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class TasksServiceImpl implements TasksService {

    private final TasksMapper tasksMapper;

    @Resource
    private SubTasksService subtasksService;

    @Resource
    private WorkflowDefinitionsService workflowDefinitionsService;

    public TasksServiceImpl(TasksMapper tasksMapper) {
        this.tasksMapper = tasksMapper;
    }

    @Override
    public IPage<Task> queryByPage(Long wareHouseId, IPage<Task> page, Task tasks) {
        return tasksMapper.queryByPage(wareHouseId, page, tasks);
    }

    @Override
    public List<Task> queryList(Long wareHouseId, Task tasks) {
        return tasksMapper.queryList(wareHouseId, tasks);
    }

    @Override
    public List<Long> insertBatch(List<Task> tasks) {
        return tasksMapper.insertBatch(tasks);
    }

    @Override
    public JSONObject queryTaskStatistics(Long wareHouseId) {
        return tasksMapper.queryTaskStatistics(wareHouseId);
    }

    @Override
    public List<Task> queryListByDeviceId(Task queryTask, Set<String> deviceIds, Integer limit, List<Integer> excludedAisles) {
        return tasksMapper.queryListByDeviceId(queryTask, deviceIds, limit, excludedAisles);
    }

    @Override
    public List<JSONObject> queryTaskStatisticsByDate(String warehouseId, Date start, Date end, String groupByStr) {
        return tasksMapper.queryTaskStatisticsByDate(warehouseId, start, end, groupByStr);
    }

    @Override
    public int updateFrontEmptyByTwinsNo(String twinsNo, Boolean frontEmpty) {
        return tasksMapper.updateFrontEmptyByTwinsNo(twinsNo, frontEmpty);
    }

    @Override
    public List<JSONObject> queryEndPointSummary(String warehouseIds) {
        return tasksMapper.queryEndPointSummary(warehouseIds);
    }

    @Override
    public Date queryEarliestTaskTimeByEndPoint(String endPoint, Long warehouseId) {
        return tasksMapper.queryEarliestTaskTimeByEndPoint(endPoint, warehouseId);
    }

    @Override
    public Task queryById(Long wareHouseId, Long taskId) {
        return tasksMapper.queryById(wareHouseId, taskId);
    }

    @Override
    public String selectDevicesById(Long taskId) {
        return tasksMapper.selectDevicesById(taskId);
    }

    @Override
    public JSONObject queryEndPointSummaryOne(Integer warehouseId, String endPoint) {
        return tasksMapper.queryEndPointSummaryOne(warehouseId, endPoint);
    }

    @Override
    public Task selectOnePendingByTwinsNo(Integer warehouseId, String endPoint, String twinsNo) {
        return tasksMapper.selectOnePendingByTwinsNo(warehouseId, endPoint, twinsNo);
    }

    @Override
    public Task queryByProcessInstanceId(String processInstanceId) {
        return tasksMapper.queryByProcessInstanceId(processInstanceId);
    }

    @Override
    public Task selectOneFromWms(String taskNumber, Long wmsBizId, String taskCategory) {
        return tasksMapper.selectOneFromWms(taskNumber, wmsBizId, taskCategory);
    }

    @Override
    public int updateNonExecuteTaskLocation(String taskNumber, String location, String startPoint, String twinsNo, String depth) {
        return tasksMapper.updateNonExecuteTaskLocation(taskNumber, location, startPoint, twinsNo, depth);
    }

    @Override
    public List<String> queryCompatibleEndpointsByDeviceId(Long warehouseId, String deviceId) {
        return tasksMapper.queryCompatibleEndpointsByDeviceId(warehouseId, deviceId);
    }

    @Override
    public long countTasksByWarehouseAndWorkflowDefId(Long warehouseId, String workflowDefId) {
        return tasksMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getWarehouseId, warehouseId)
                .eq(Task::getWorkflowDefId, workflowDefId));
    }

    @Override
    public long countActiveTasksByWarehouseAndWorkflowDefId(Long warehouseId, String workflowDefId) {
        return tasksMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getWarehouseId, warehouseId)
                .eq(Task::getWorkflowDefId, workflowDefId)
                .in(Task::getStatus,
                        TaskStatus.PENDING.getValue(),
                        TaskStatus.EXECUTING.getValue(),
                        TaskStatus.SUSPENDED.getValue()));
    }

    @Override
    @Transactional(propagation = NOT_SUPPORTED)
    public Long insertTask(Long warehouseId, Task task) {
        task.setWarehouseId(warehouseId);
        // 插入task表
        task = insertTaskWithSubtasks(task);
        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertBatchTasks(Long wareHouseId, List<Task> tasks) {
        try {
            for (Task task : tasks) {
                task.setWarehouseId(wareHouseId);
            }
            List<Long> allTaskIds = insertBatchTasksWithSubtasks(tasks);
            if (CollUtil.isEmpty(allTaskIds)) {
                return 0;
            }
            return allTaskIds.size();
        } catch (Exception e) {
            log.error("批量创建完整任务失败", e);
            throw e;
        }
    }

    public Task insertTaskWithSubtasks(Task task) {
        // 如果未显式提供depth，根据location自动推导
        if (task.getDepth() == null) {
            task.setDepth(DomainEnums.DepthType.parseFromLocation(task.getLocation()).name());
        }
        // 根据仓库ID和流程定义ID查询设备流程
        WorkflowDefinition workflowDefinition = workflowDefinitionsService.queryByWorkflowId(task.getWarehouseId(), task.getWorkflowDefId());
        task.setIsAutoStart(workflowDefinition.getIsAutoStart());
        task.setProcessDefinitionId(workflowDefinition.getProcessDefId());
        task.setWorkDirection(workflowDefinition.getWorkDirection());
        // 插入主任务
        tasksMapper.insert(task);
        // 处理子任务
        if (workflowDefinition.getSubtaskDefinitions() != null) {
            // 手动转换解决缓存反序列化问题
            List<SubtaskDefinition> subtasks = JSON.parseArray(
                    JSON.toJSONString(workflowDefinition.getSubtaskDefinitions()),
                    SubtaskDefinition.class
            );
            buildSubtasksFromDefinition(task, subtasks);
            // 插入子任务
            if (task.getSubtasks() != null && !task.getSubtasks().isEmpty()) {
                subtasksService.insertBatch(task.getSubtasks());
            }
        }
        return task;
    }

    public List<Long> insertBatchTasksWithSubtasks(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        // 从 Redis 缓存获取流程定义
        Set<String> workflowDefIds = tasks.stream().map(Task::getWorkflowDefId).collect(Collectors.toSet());
        Map<String, WorkflowDefinition> workflowDefinitionMap = new HashMap<>();
        for (String wfId : workflowDefIds) {
            if (workflowDefinitionMap.containsKey(wfId)) continue;
            WorkflowDefinition workflowDefinition = workflowDefinitionsService.queryByWorkflowId(tasks.get(0).getWarehouseId(), wfId);
            if (workflowDefinition != null) workflowDefinitionMap.put(wfId, workflowDefinition);
        }

        tasks.forEach(task -> {
            if (task.getDepth() == null) {
                task.setDepth(DomainEnums.DepthType.parseFromLocation(task.getLocation()).name());
            }
            WorkflowDefinition wf = workflowDefinitionMap.get(task.getWorkflowDefId());
            if (wf != null) {
                task.setIsAutoStart(wf.getIsAutoStart());
                task.setProcessDefinitionId(wf.getProcessDefId());
                task.setWorkDirection(wf.getWorkDirection());
            }
        });

        List<Long> allTaskIds = new ArrayList<>();
        List<List<Task>> taskPartitions = Lists.partition(tasks, 60);
        for (List<Task> tasksList : taskPartitions) {
            List<Long> longs = tasksMapper.insertBatch(tasksList);
            allTaskIds.addAll(longs);
        }

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            task.setId(allTaskIds.get(i));
            WorkflowDefinition wf = workflowDefinitionMap.get(task.getWorkflowDefId());
            if (wf != null && wf.getSubtaskDefinitions() != null) {
                List<SubtaskDefinition> subtasks = JSON.parseArray(
                        JSON.toJSONString(wf.getSubtaskDefinitions()),
                        SubtaskDefinition.class);
                buildSubtasksFromDefinition(task, subtasks);
            }
        }

        List<SubTask> subtasks = tasks.stream()
                .flatMap(x -> x.getSubtasks().stream())
                .collect(Collectors.toList());
        List<List<SubTask>> partition = Lists.partition(subtasks, 100);
        for (List<SubTask> subtaskList : partition) {
            subtasksService.insertBatch(subtaskList);
        }
        return allTaskIds;
    }

    private void buildSubtasksFromDefinition(Task task, List<SubtaskDefinition> subtaskDefList){
        if (subtaskDefList == null || subtaskDefList.isEmpty()) {
            return;
        }
        String firstSubtaskDefId = workflowDefinitionsService.getFirstSubDefString(task.getWarehouseId(), task.getWorkflowDefId());
        // 仅构建首个任务，剩余子任务在运行时动态实例化
        Optional<SubtaskDefinition> firstDefOption = subtaskDefList.stream().filter(x -> x.getSubtaskDefId().equals(firstSubtaskDefId)).findFirst();
        if (firstDefOption.isEmpty()) {
            throw new RuntimeException("未找到流程定义的第一个子任务定义，子任务定义ID为" + firstSubtaskDefId);
        }

        SubtaskDefinition firstDef = firstDefOption.get();
        SubTask subtask = SubTask.builder()
                .taskId(task.getId())
                .subtaskDefId(firstDef.getSubtaskDefId())
                .name(firstDef.getName())
                .priority(firstDef.getPriority())
                .status(TaskStatus.PENDING.getValue())
                .warehouseId(task.getWarehouseId())
                .currentInstructionIndex(0)
                .area(firstDef.getArea())
                .checkAisle(firstDef.getCheckAisle())
                .workflowDefId(task.getWorkflowDefId())
                .isStartNextProcess(firstDef.getIsStartNextProcess())
                .freeDeviceId(firstDef.getFreeDeviceId())
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        // 构建指令列表（仅首个任务）
        List<Instruction> instructions = new ArrayList<>();
        firstDef.getInstructions().forEach(instructionDef -> {
            Instruction instruction = Instruction.builder()
                    .taskId(task.getId())
                    .deviceId(instructionDef.getDevices())
                    .protocol(instructionDef.getProtocol())
                    .commands(instructionDef.getCommands())
                    .params(instructionDef.getParams())
                    .sequence(instructionDef.getSequence())
                    .status(InstructionStatus.PENDING.getValue())
                    .createdAt(LocalDateTime.now())
                    .messageEvent(instructionDef.getMessageEvent())
                    .eventLogic(instructionDef.getEventLogic())
                    .build();
            instructions.add(instruction);
        });

        subtask.setInstructions(instructions);

        task.setSubtasks(java.util.Collections.singletonList(subtask));
    }

    @Override
    public int updateById(Long wareHouseId, Long id, Task task){
        task.setUpdatedAt(LocalDateTime.now());
        return tasksMapper.update(task, Wrappers.<Task>lambdaUpdate()
                .eq(Task::getId, id)
                .eq(Task::getWarehouseId, wareHouseId));
    }

    @Override
    public int deleteById(Long warehouseId, Long id) {
        subtasksService.deleteByTaskId(warehouseId,id);
        return tasksMapper.delete(Wrappers.<Task>lambdaQuery()
                .eq(Task::getId, id)
                .eq(Task::getWarehouseId, warehouseId)
        );
    }
}
