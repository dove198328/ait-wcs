package cn.aitplus.wcs.infra.service.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.aitplus.wcs.core.domain.model.Task;
import cn.hutool.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Set;

public interface TasksService {

    IPage<Task> queryByPage(Long wareHouseId, IPage<Task> page, Task tasks);

    List<Task> queryList(Long wareHouseId, Task tasks);

    List<Long> insertBatch(List<Task> tasks);

    JSONObject queryTaskStatistics(Long wareHouseId);

    List<Task> queryListByDeviceId(Task queryTask, Set<String> deviceIds, Integer limit, List<Integer> excludedAisles);

    List<JSONObject> queryTaskStatisticsByDate(String warehouseId, Date start, Date end, String groupByStr);

    int updateFrontEmptyByTwinsNo(String twinsNo, Boolean frontEmpty);

    List<JSONObject> queryEndPointSummary(String warehouseIds);

    Date queryEarliestTaskTimeByEndPoint(String endPoint, Long warehouseId);

    Task queryById(Long wareHouseId, Long taskId);

    String selectDevicesById(Long taskId);

    JSONObject queryEndPointSummaryOne(Integer warehouseId, String endPoint);

    Task selectOnePendingByTwinsNo(Integer warehouseId, String endPoint, String twinsNo);

    Task queryByProcessInstanceId(String processInstanceId);

    Task selectOneFromWms(String taskNumber, Long wmsBizId, String taskCategory);

    int updateNonExecuteTaskLocation(String taskNumber, String location, String startPoint, String twinsNo, String depth);

    List<String> queryCompatibleEndpointsByDeviceId(Long warehouseId, String deviceId);
}
