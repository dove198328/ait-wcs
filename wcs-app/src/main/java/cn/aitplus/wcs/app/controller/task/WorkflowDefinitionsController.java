package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.common.domain.AjaxResult;
import cn.aitplus.wcs.common.domain.page.PageUtils;
import cn.aitplus.wcs.common.domain.page.TableDataInfo;
import cn.aitplus.wcs.core.domain.model.workflow.WorkflowDefinition;
import cn.aitplus.wcs.infra.service.task.WorkflowDefinitionsService;
import cn.aitplus.wcs.workflow.service.WorkflowDefinitionCommandService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "流程定义管理")
@RequestMapping("/api/{wareHouseId}/workflow-definitions")
public class WorkflowDefinitionsController {

    private final WorkflowDefinitionsService workflowDefinitionsService;
    private final WorkflowDefinitionCommandService workflowDefinitionCommandService;

    public WorkflowDefinitionsController(WorkflowDefinitionsService workflowDefinitionsService,
                                         WorkflowDefinitionCommandService workflowDefinitionCommandService) {
        this.workflowDefinitionsService = workflowDefinitionsService;
        this.workflowDefinitionCommandService = workflowDefinitionCommandService;
    }

    @ApiOperation("查询流程定义列表")
    @PostMapping("/search")
    public AjaxResult<TableDataInfo<WorkflowDefinition>> queryList(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("页码") @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @ApiParam("每页条数") @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestBody(required = false) WorkflowDefinition workflowDefinition) {
        WorkflowDefinition query = workflowDefinition == null ? new WorkflowDefinition() : workflowDefinition;
        query.setWarehouseId(wareHouseId.intValue());
        if (PageUtils.hasOnlyOnePageParam(pageNum, pageSize)) {
            return AjaxResult.error("pageNum和pageSize必须同时传入");
        }
        if (PageUtils.isPageQuery(pageNum, pageSize)) {
            IPage<WorkflowDefinition> page = new Page<>(pageNum, pageSize);
            return AjaxResult.success(TableDataInfo.build(workflowDefinitionsService.queryByPage(wareHouseId, page, query)));
        }
        return AjaxResult.success(TableDataInfo.build(workflowDefinitionsService.queryList(wareHouseId, query)));
    }

    @ApiOperation("新增流程定义并部署到工作流")
    @PostMapping
    public AjaxResult<WorkflowDefinition> create(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @RequestBody WorkflowDefinition workflowDefinition) {
        return AjaxResult.success(workflowDefinitionCommandService.createAndDeploy(wareHouseId, workflowDefinition));
    }

    @ApiOperation("更新流程定义并重新部署到工作流")
    @PutMapping("/{id}")
    public AjaxResult<WorkflowDefinition> update(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("流程定义主键ID") @PathVariable("id") Long id,
            @RequestBody WorkflowDefinition workflowDefinition) {
        return AjaxResult.success(workflowDefinitionCommandService.updateAndRedeploy(wareHouseId, id, workflowDefinition));
    }

    @ApiOperation("物理删除流程定义")
    @DeleteMapping("/{id}")
    public AjaxResult<Void> delete(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("流程定义主键ID") @PathVariable("id") Long id) {
        workflowDefinitionCommandService.deletePhysically(wareHouseId, id);
        return AjaxResult.success();
    }

    @ApiOperation("根据业务类型查询流程定义")
    @GetMapping("/biz-type/{bizType}")
    public AjaxResult<WorkflowDefinition> findByBizType(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("业务类型") @PathVariable("bizType") String bizType) {
        return AjaxResult.success(workflowDefinitionsService.findByBizType(wareHouseId, bizType));
    }

    @ApiOperation("根据流程ID查询流程定义")
    @GetMapping("/workflow/{workflowId}")
    public AjaxResult<WorkflowDefinition> queryByWorkflowId(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("流程ID") @PathVariable("workflowId") String workflowId) {
        return AjaxResult.success(workflowDefinitionsService.queryByWorkflowId(wareHouseId, workflowId));
    }

    @ApiOperation("根据流程ID查询首个子流程定义")
    @GetMapping("/workflow/{workflowId}/first-sub-def")
    public AjaxResult<String> getFirstSubDefString(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("流程ID") @PathVariable("workflowId") String workflowId) {
        return AjaxResult.success(workflowDefinitionsService.getFirstSubDefString(wareHouseId, workflowId));
    }
}
