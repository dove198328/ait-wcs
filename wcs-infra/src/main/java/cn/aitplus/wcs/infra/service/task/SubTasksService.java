package cn.aitplus.wcs.infra.service.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.aitplus.wcs.core.domain.model.SubTask;

import java.util.List;

public interface SubTasksService {

    IPage<SubTask> queryByPage(Long wareHouseId, IPage<SubTask> page, SubTask subtasks);

    List<SubTask> queryList(Long wareHouseId, SubTask subtasks);

    List<Long> insertBatch(List<SubTask> subtasks);

    int batchUpdateStatus(String subtaskIds, String status);

    List<SubTask> findTopNSubtasks(Long warehouseId, String status, int limit);

    SubTask upsertSubtask(SubTask subtask);

    SubTask queryById(Long wareHouseId, Long subtaskIds);

    SubTask queryNoCompletedByTaskId(Long wareHouseId, Long taskId);
}
