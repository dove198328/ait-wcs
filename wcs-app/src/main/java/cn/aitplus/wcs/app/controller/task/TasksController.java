package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.common.domain.AjaxResult;
import cn.aitplus.wcs.common.domain.page.PageUtils;
import cn.aitplus.wcs.common.domain.page.TableDataInfo;
import cn.aitplus.wcs.core.domain.model.Task;
import cn.aitplus.wcs.infra.service.task.TasksService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Api(tags = "任务管理")
@RestController
@RequestMapping("/api/{wareHouseId}/tasks")
public class TasksController {

    private final TasksService tasksService;

    public TasksController(TasksService tasksService) {
        this.tasksService = tasksService;
    }

    @ApiOperation("分页查询任务列表")
    @PostMapping("/search")
    public AjaxResult<TableDataInfo<Task>> queryList(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                                     @ApiParam("页码") @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                                     @ApiParam("每页条数") @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                     @RequestBody(required = false) Task task) {
        Task query = task == null ? new Task() : task;
        query.setWarehouseId(wareHouseId);
        if (PageUtils.hasOnlyOnePageParam(pageNum, pageSize)) {
            return AjaxResult.error("pageNum和pageSize必须同时传入");
        }
        if (PageUtils.isPageQuery(pageNum, pageSize)) {
            IPage<Task> page = new Page<>(pageNum, pageSize);
            return AjaxResult.success(TableDataInfo.build(tasksService.queryByPage(wareHouseId, page, query)));
        }
        return AjaxResult.success(TableDataInfo.build(tasksService.queryList(wareHouseId, query)));
    }

    @ApiOperation("根据主键查询单条数据")
    @GetMapping("/{id}")
    public AjaxResult<Task> queryById(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                      @ApiParam("任务ID") @PathVariable("id") Long id) {
        return AjaxResult.success(tasksService.queryById(wareHouseId, id));
    }

    @ApiOperation("新增任务")
    @ApiImplicitParam(name = "task", value = "完整任务（包含子任务列表）", required = true, dataType = "Task",paramType = "body")
    @PostMapping("/complete")
    public AjaxResult insertBatchTask(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                         @RequestBody Task task) {
        if (wareHouseId == null ||task == null) {
            return AjaxResult.error("参数错误");
        }
        return AjaxResult.success("成功创建任务", tasksService.insertBatchTask(wareHouseId, task)
        );
    }

    @ApiOperation("批量新增任务")
    @PostMapping("/batch")
    public AjaxResult<List<Long>> insertBatchTasks(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                                           @RequestBody List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return AjaxResult.error("任务列表不能为空");
        }
        this.tasksService.insertBatchTasks(wareHouseId,tasks);
        return AjaxResult.success("成功创建 " + tasks.size() + " 条任务");
    }

    @ApiOperation("修改数据")
    @PutMapping("/{id}")
    public AjaxResult update(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                             @ApiParam("任务ID") @PathVariable("id") Long id,
                             @RequestBody Task task) {
        if (id == null || task == null) {
            return AjaxResult.error("参数错误");
        }
        System.out.println("wareHouseId=" + wareHouseId);
        System.out.println("id=" + id);
        System.out.println("task=" + task);
        return AjaxResult.toAjax(tasksService.updateById(wareHouseId, id, task));
    }

    @ApiOperation("删除数据")
    @ApiImplicitParam(name = "id", value = "主键", required = true, dataType = "Long",paramType = "path")
    @DeleteMapping("{id}")
    public AjaxResult deleteById(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                 @ApiParam("任务ID") @PathVariable("id") Long id) {
        if (id == null) {
            return AjaxResult.error("参数错误");
        }
        return AjaxResult.toAjax(this.tasksService.deleteById(wareHouseId,id));
    }
}