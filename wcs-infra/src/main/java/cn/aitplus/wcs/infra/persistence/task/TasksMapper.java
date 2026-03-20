package cn.aitplus.wcs.infra.persistence.task;

import cn.aitplus.wcs.core.domain.model.Task;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * (Tasks)表数据库访问层
 */
public interface TasksMapper {

    IPage<Task> queryByPage(@Param("page") IPage<Task> page, @Param("ew") Task tasks);

    List<Task> queryList(@Param("ew") Task tasks);

    List<Long> insertBatch(@Param("list") List<Task> tasks);

    JSONObject queryTaskStatistics(@Param("warehouseId") String warehouseId);

    /**
     * 查询设备可执行的任务列表（支持排除被占用的巷道）
     * @param queryTask 查询条件
     * @param deviceIds 设备ID集合
     * @param limit 限制数量
     * @param excludedAisles 被占用的巷道号列表（排除这些巷道），为 null 或空时不排除
     * @return 任务列表
     */
    List<Task> queryListByDeviceId(@Param("task") Task queryTask, @Param("collection") Set<String> deviceIds, @Param("limit") Integer limit, @Param("excludedAisles") List<Integer> excludedAisles);

    List<JSONObject> queryTaskStatisticsByDate(@Param("warehouseId") String warehouseId, @Param("start") Date start, @Param("end")Date end, @Param("groupByStr") String groupByStr);

    /**
     * 根据组号更新 front_empty 状态
     * @param twinsNo 组号
     * @param frontEmpty 前排是否为空 (true=空, false=占用)
     * @return 更新的记录数
     */
    int updateFrontEmptyByTwinsNo(@Param("twinsNo") String twinsNo, @Param("frontEmpty") Boolean frontEmpty);

    /**
     * 根据终点选出自动启动的任务的数量和最早的时间,参数写死了是自动启动和pending
     * @return
     */
    List<JSONObject> queryEndPointSummary(@Param("warehouseIds") String warehouseIds);

    Date queryEarliestTaskTimeByEndPoint(@Param("endPoint") String endPoint, @Param("warehouseId") Long warehouseId);

    Task queryById(Long taskId);

    String selectDevicesById(Long taskId);

    JSONObject queryEndPointSummaryOne(@Param("warehouseId") Integer warehouseId, @Param("endPoint") String endPoint);

    /**
     * 按组号与端点选择一条待处理任务（自启动）
     */
    Task selectOnePendingByTwinsNo(@org.apache.ibatis.annotations.Param("warehouseId") Integer warehouseId,
                                   @org.apache.ibatis.annotations.Param("endPoint") String endPoint,
                                   @org.apache.ibatis.annotations.Param("twinsNo") String twinsNo);

    Task queryByProcessInstanceId(String processInstanceId);

    Task selectOneFromWms(@Param("taskNumber") String taskNumber, @Param("wmsBizId") Long wmsBizId, @Param("taskCategory") String taskCategory);

    int updateNonExecuteTaskLocation(@Param("taskNumber") String taskNumber, @Param("location") String location, @Param("startPoint") String startPoint, @Param("twinsNo") String twinsNo, @Param("depth") String depth);

    /**
     * 查询设备可执行的所有endpoint列表（去重）
     * 用于优化批量查询，避免返回大量任务数据
     *
     * @param warehouseId 仓库ID
     * @param deviceId 设备ID
     * @return endpoint列表
     */
    List<String> queryCompatibleEndpointsByDeviceId(@Param("warehouseId") Long warehouseId, @Param("deviceId") String deviceId);
}

