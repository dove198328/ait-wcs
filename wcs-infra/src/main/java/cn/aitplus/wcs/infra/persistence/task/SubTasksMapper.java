package cn.aitplus.wcs.infra.persistence.task;

import cn.aitplus.wcs.core.domain.model.SubTasks;

import java.util.List;


/**
 * (SubTasks)表数据库访问层
 */
public interface SubTasksMapper {

//    IPage<SubTasks> queryByPage(@Param("page") IPage<SubTasks> page, @Param("ew") SubTasks subtasks);
//
//    List<SubTasks> queryList(@Param("ew") SubTasks subtasks);
//
//    // 批量插入子任务及其指令
//    List<Long> insertbatch(@Param("list") List<SubTasks> subtasks);
//
//    // 批量更新子任务状态
//    int batchUpdateStatus(@Param("subtaskIds") String subtaskIds, @Param("status") String status);
//
//    // 查询指定仓库中的前N条子任务记录
//    List<SubTasks> findTopNSubtasks(@Param("warehouseId") Long warehouseId, @Param("status") String status, @Param("limit") int limit);

    // 插入子任务,如果已存在则直接返回该记录
    SubTasks upsertSubtask(SubTasks subtask);

    // 根据子任务ID获取对应的任务ID
    SubTasks queryById(Long subtaskIds);

    SubTasks queryNoCompletedByTaskId(Long taskId);
}

