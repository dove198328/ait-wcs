package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.common.domain.AjaxResult;
import cn.aitplus.wcs.common.domain.page.PageUtils;
import cn.aitplus.wcs.common.domain.page.TableDataInfo;
import cn.aitplus.wcs.core.domain.model.SubTask;
import cn.aitplus.wcs.infra.service.task.SubTasksService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Api(tags = "子任务管理")
@RequestMapping("/api/{wareHouseId}/subtasks")
public class SubTasksController {

    private final SubTasksService subTasksService;

    public SubTasksController(SubTasksService subTasksService) {
        this.subTasksService = subTasksService;
    }

    @ApiOperation("查询子任务列表")
    @PostMapping("/search")
    public AjaxResult<TableDataInfo<SubTask>> queryList(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                                        @ApiParam("页码") @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                                        @ApiParam("每页条数") @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                        @RequestBody(required = false) SubTask subTask) {
        SubTask query = subTask == null ? new SubTask() : subTask;
        query.setWarehouseId(wareHouseId);
        if (PageUtils.hasOnlyOnePageParam(pageNum, pageSize)) {
            return AjaxResult.error("pageNum和pageSize必须同时传入");
        }
        if (PageUtils.isPageQuery(pageNum, pageSize)) {
            IPage<SubTask> page = new Page<>(pageNum, pageSize);
            return AjaxResult.success(TableDataInfo.build(subTasksService.queryByPage(wareHouseId, page, query)));
        }
        return AjaxResult.success(TableDataInfo.build(subTasksService.queryList(wareHouseId, query)));
    }

    @ApiOperation("根据ID查询子任务")
    @GetMapping("/{id}")
    public AjaxResult<SubTask> queryById(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                         @ApiParam("子任务ID") @PathVariable("id") Long id) {
        return AjaxResult.success(subTasksService.queryById(wareHouseId, id));
    }

    @ApiOperation("新增或更新子任务")
    @PostMapping("/upsert")
    public AjaxResult<SubTask> upsert(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                      @RequestBody SubTask subTask) {
        subTask.setWarehouseId(wareHouseId);
        return AjaxResult.success(subTasksService.upsertSubtask(subTask));
    }

    @ApiOperation("查询TopN子任务")
    @GetMapping("/top")
    public AjaxResult<List<SubTask>> findTopNSubtasks(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                                      @ApiParam("状态") @RequestParam("status") String status,
                                                      @ApiParam("返回条数") @RequestParam("limit") int limit) {
        return AjaxResult.success(subTasksService.findTopNSubtasks(wareHouseId, status, limit));
    }
}
