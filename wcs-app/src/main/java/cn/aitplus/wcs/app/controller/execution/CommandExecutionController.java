package cn.aitplus.wcs.app.controller.execution;

import cn.aitplus.wcs.common.domain.AjaxResult;
import cn.aitplus.wcs.common.domain.page.PageUtils;
import cn.aitplus.wcs.common.domain.page.TableDataInfo;
import cn.aitplus.wcs.core.domain.model.execution.CommandExecution;
import cn.aitplus.wcs.infra.service.execution.CommandExecutionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "指令执行审计")
@RequestMapping("/api/{wareHouseId}/command-executions")
public class CommandExecutionController {

    private final CommandExecutionService commandExecutionService;

    public CommandExecutionController(CommandExecutionService commandExecutionService) {
        this.commandExecutionService = commandExecutionService;
    }

    @ApiOperation("查询执行审计列表")
    @PostMapping("/search")
    public AjaxResult<TableDataInfo<CommandExecution>> queryList(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("页码") @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @ApiParam("每页条数") @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestBody(required = false) CommandExecution commandExecution) {
        CommandExecution query = commandExecution == null ? new CommandExecution() : commandExecution;
        query.setWarehouseId(wareHouseId);
        if (PageUtils.hasOnlyOnePageParam(pageNum, pageSize)) {
            return AjaxResult.error("pageNum和pageSize必须同时传入");
        }
        if (PageUtils.isPageQuery(pageNum, pageSize)) {
            IPage<CommandExecution> page = new Page<>(pageNum, pageSize);
            return AjaxResult.success(TableDataInfo.build(commandExecutionService.queryByPage(wareHouseId, page, query)));
        }
        return AjaxResult.success(TableDataInfo.build(commandExecutionService.queryList(wareHouseId, query)));
    }

    @ApiOperation("根据ID查询执行审计")
    @GetMapping("/{id}")
    public AjaxResult<CommandExecution> queryById(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("执行审计ID") @PathVariable("id") Long id) {
        return AjaxResult.success(commandExecutionService.queryById(wareHouseId, id));
    }

    @ApiOperation("根据幂等键查询执行审计")
    @GetMapping("/idempotency/{idempotencyKey}")
    public AjaxResult<CommandExecution> queryByIdempotencyKey(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("幂等键") @PathVariable("idempotencyKey") String idempotencyKey) {
        return AjaxResult.success(commandExecutionService.queryByIdempotencyKey(wareHouseId, idempotencyKey));
    }

    @ApiOperation("新增执行审计（幂等）")
    @PostMapping
    public AjaxResult<CommandExecution> create(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @RequestBody CommandExecution commandExecution) {
        return AjaxResult.success(commandExecutionService.create(wareHouseId, commandExecution));
    }

    @ApiOperation("更新执行审计")
    @PutMapping
    public AjaxResult<CommandExecution> update(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @RequestBody CommandExecution commandExecution) {
        return AjaxResult.success(commandExecutionService.update(wareHouseId, commandExecution));
    }

    @ApiOperation("推进执行状态")
    @PutMapping("/{id}/status")
    public AjaxResult<Void> updateStatus(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("执行审计ID") @PathVariable("id") Long id,
            @ApiParam("状态") @RequestParam("status") String status,
            @ApiParam("响应报文JSON") @RequestParam(value = "responseJson", required = false) String responseJson,
            @ApiParam("错误码") @RequestParam(value = "errorCode", required = false) String errorCode,
            @ApiParam("错误信息") @RequestParam(value = "errorMessage", required = false) String errorMessage) {
        commandExecutionService.updateStatus(wareHouseId, id, status, responseJson, errorCode, errorMessage);
        return AjaxResult.success();
    }
}
