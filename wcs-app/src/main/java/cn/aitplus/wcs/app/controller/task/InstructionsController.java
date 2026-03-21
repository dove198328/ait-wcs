package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.common.domain.AjaxResult;
import cn.aitplus.wcs.core.domain.model.Instruction;
import cn.aitplus.wcs.infra.service.task.InstructionsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@Api(tags = "指令管理")
@RequestMapping("/api/{wareHouseId}/instructions")
public class InstructionsController {

    private final InstructionsService instructionsService;

    public InstructionsController(InstructionsService instructionsService) {
        this.instructionsService = instructionsService;
    }

    @ApiOperation("查询指令列表")
    @PostMapping("/search")
    public AjaxResult<List<Instruction>> queryList(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                                   @RequestBody(required = false) Instruction instruction) {
        return AjaxResult.success(instructionsService.queryList(wareHouseId, instruction));
    }

    @ApiOperation("批量新增指令")
    @PostMapping("/batch")
    public AjaxResult<Void> insertBatch(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                        @RequestBody List<Instruction> instructions) {
        if (wareHouseId == null) {
            throw new IllegalArgumentException("wareHouseId不能为空");
        }
        instructionsService.insertBatch(instructions);
        return AjaxResult.success();
    }
}
