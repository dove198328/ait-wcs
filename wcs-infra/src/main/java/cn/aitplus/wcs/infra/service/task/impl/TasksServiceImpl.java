package cn.aitplus.wcs.infra.service.task.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.aitplus.wcs.core.domain.model.Task;
import cn.hutool.json.JSONObject;
import cn.aitplus.wcs.infra.persistence.task.TasksMapper;
import cn.aitplus.wcs.infra.service.task.TasksService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class TasksServiceImpl implements TasksService {

    private final TasksMapper tasksMapper;

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
}
