package cn.aitplus.wcs.infra.persistence.task;

import cn.aitplus.wcs.core.domain.model.SubTask;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * (SubTasks)表数据库访问层
 */
public interface SubTasksMapper {

    IPage<SubTask> queryByPage(@Param("wareHouseId") Long wareHouseId, @Param("page") IPage<SubTask> page, @Param("ew") SubTask subtasks);

    List<SubTask> queryList(@Param("wareHouseId") Long wareHouseId, @Param("ew") SubTask subtasks);

    // 批量插入子任务及其指令
    List<Long> insertbatch(@Param("list") List<SubTask> subtasks);

    // 批量更新子任务状态
    int batchUpdateStatus(@Param("subtaskIds") String subtaskIds, @Param("status") String status);

    // 查询指定仓库中的前N条子任务记录
    List<SubTask> findTopNSubtasks(@Param("warehouseId") Long warehouseId, @Param("status") String status, @Param("limit") int limit);

    // 插入子任务,如果已存在则直接返回该记录
    SubTask upsertSubtask(SubTask subtask);

    // 根据子任务ID获取对应的任务ID
    SubTask queryById(@Param("wareHouseId") Long wareHouseId, @Param("id") Long subtaskIds);

    SubTask queryNoCompletedByTaskId(@Param("wareHouseId") Long wareHouseId, @Param("taskId") Long taskId);
}

