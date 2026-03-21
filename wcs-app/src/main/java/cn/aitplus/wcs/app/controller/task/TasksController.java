package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.common.domain.AjaxResult;
import cn.aitplus.wcs.core.domain.model.Task;
import cn.aitplus.wcs.infra.service.task.TasksService;
import cn.hutool.json.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Api(tags = "任务管理")
@RequestMapping("/api/{wareHouseId}/tasks")
public class TasksController {

    private final TasksService tasksService;

    public TasksController(TasksService tasksService) {
        this.tasksService = tasksService;
    }

    @ApiOperation("查询任务列表")
    @PostMapping("/search")
    public AjaxResult<List<Task>> queryList(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                            @RequestBody(required = false) Task task) {
        Task query = task == null ? new Task() : task;
        query.setWarehouseId(wareHouseId);
        return AjaxResult.success(tasksService.queryList(wareHouseId, query));
    }

    @ApiOperation("根据ID查询任务")
    @GetMapping("/{id}")
    public AjaxResult<Task> queryById(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                      @ApiParam("任务ID") @PathVariable("id") Long id) {
        return AjaxResult.success(tasksService.queryById(wareHouseId, id));
    }

    @ApiOperation("批量新增任务")
    @PostMapping("/batch")
    public AjaxResult<List<Long>> insertBatch(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                              @RequestBody List<Task> tasks) {
        tasks.forEach(task -> task.setWarehouseId(wareHouseId));
        return AjaxResult.success(tasksService.insertBatch(tasks));
    }

    @ApiOperation("查询任务统计")
    @GetMapping("/statistics")
    public AjaxResult<JSONObject> queryTaskStatistics(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId) {
        return AjaxResult.success(tasksService.queryTaskStatistics(wareHouseId));
    }
}
