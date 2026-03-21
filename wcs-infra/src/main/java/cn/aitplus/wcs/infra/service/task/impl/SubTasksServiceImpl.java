package cn.aitplus.wcs.infra.service.task.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.aitplus.wcs.core.domain.model.SubTask;
import cn.aitplus.wcs.infra.persistence.task.SubTasksMapper;
import cn.aitplus.wcs.infra.service.task.SubTasksService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubTasksServiceImpl implements SubTasksService {

    private final SubTasksMapper subTasksMapper;

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
    public List<Long> insertBatch(List<SubTask> subtasks) {
        return subTasksMapper.insertbatch(subtasks);
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
}
