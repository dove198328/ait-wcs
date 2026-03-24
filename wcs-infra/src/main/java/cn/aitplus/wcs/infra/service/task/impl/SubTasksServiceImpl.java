package cn.aitplus.wcs.infra.service.task.impl;

import cn.aitplus.wcs.core.domain.model.Instruction;
import cn.aitplus.wcs.infra.service.task.InstructionsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.aitplus.wcs.core.domain.model.SubTask;
import cn.aitplus.wcs.infra.persistence.task.SubTasksMapper;
import cn.aitplus.wcs.infra.service.task.SubTasksService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class SubTasksServiceImpl implements SubTasksService {

    private final SubTasksMapper subTasksMapper;

    @Resource
    private InstructionsService instructionsService;

    public SubTasksServiceImpl(SubTasksMapper subTasksMapper) {
        this.subTasksMapper = subTasksMapper;
    }

    @Override
    public IPage<SubTask> queryByPage(Long wareHouseId, IPage<SubTask> page, SubTask subtasks) {
        return subTasksMapper.queryByPage(wareHouseId, page, subtasks);
    }

    @Override
    public List<SubTask> queryList(Long wareHouseId, SubTask subtasks) {
        return subTasksMapper.queryList(wareHouseId, subtasks);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void insertBatch(List<SubTask> subtasks) {

        Map<String, List<SubTask>> collect = subtasks.stream()
                .collect(Collectors.groupingBy(x -> x.getTaskId() + "_" + x.getSubtaskDefId()));

        for (Map.Entry<String, List<SubTask>> entry : collect.entrySet()) {

                List<SubTask> subtasksForInsert = entry.getValue();

                // 1. 批量插入 SubTask
                List<Long> batchIds = subTasksMapper.insertbatch(subtasksForInsert);

                for (int i = 0; i < subtasksForInsert.size(); i++) {
                    subtasksForInsert.get(i).setId(batchIds.get(i));
                }

                // 2. 收集所有 Instruction
                List<Instruction> allInstructions = new ArrayList<>();
                for (SubTask subtask : subtasksForInsert) {
                    if (subtask.getInstructions() != null) {
                        for (Instruction instruction : subtask.getInstructions()) {
                            instruction.setSubtaskId(subtask.getId());
                            allInstructions.add(instruction);
                        }
                    }
                }
                if (!allInstructions.isEmpty()) {
                    instructionsService.insertBatch(allInstructions);
                }
        }
    }

    @Override
    public int batchUpdateStatus(String subtaskIds, String status) {
        return subTasksMapper.batchUpdateStatus(subtaskIds, status);
    }

    @Override
    public List<SubTask> findTopNSubtasks(Long warehouseId, String status, int limit) {
        return subTasksMapper.findTopNSubtasks(warehouseId, status, limit);
    }

    @Override
    public SubTask upsertSubtask(SubTask subtask) {
        return subTasksMapper.upsertSubtask(subtask);
    }

    @Override
    public SubTask queryById(Long wareHouseId, Long subtaskIds) {
        return subTasksMapper.queryById(wareHouseId, subtaskIds);
    }

    @Override
    public SubTask queryNoCompletedByTaskId(Long wareHouseId, Long taskId) {
        return subTasksMapper.queryNoCompletedByTaskId(wareHouseId, taskId);
    }

    @Override
    public int deleteByTaskId(Long warehouseId, Long taskId) {
        if (taskId == null) {
            return 0;
        }
        try {
            //删除指令表
            instructionsService.deleteByTaskId(warehouseId,taskId);
            // 使用LambdaQueryWrapper构建删除条件
            LambdaQueryWrapper<SubTask> wrapper = Wrappers.lambdaQuery(SubTask.class)
                    .eq(SubTask::getWarehouseId, warehouseId)
                    .eq(SubTask::getTaskId, taskId);
            // 执行删除操作
            int count = subTasksMapper.delete(wrapper);
            log.info("已删除任务{}的{}个子任务", taskId, count);
            return count;
        } catch (Exception e) {
            log.error("删除任务{}的子任务时发生异常", taskId, e);
            return 0;
        }
    }
}
