package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.common.domain.AjaxResult;
import cn.aitplus.wcs.common.domain.page.PageUtils;
import cn.aitplus.wcs.common.domain.page.TableDataInfo;
import cn.aitplus.wcs.core.domain.model.Instruction;
import cn.aitplus.wcs.infra.service.task.InstructionsService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public AjaxResult<TableDataInfo<Instruction>> queryList(@ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
                                                            @ApiParam("页码") @RequestParam(value = "pageNum", required = false) Integer pageNum,
                                                            @ApiParam("每页条数") @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                            @RequestBody(required = false) Instruction instruction) {
        if (PageUtils.hasOnlyOnePageParam(pageNum, pageSize)) {
            return AjaxResult.error("pageNum和pageSize必须同时传入");
        }
        if (PageUtils.isPageQuery(pageNum, pageSize)) {
            IPage<Instruction> page = new Page<>(pageNum, pageSize);
            return AjaxResult.success(TableDataInfo.build(instructionsService.queryByPage(wareHouseId, page, instruction)));
        }
        return AjaxResult.success(TableDataInfo.build(instructionsService.queryList(wareHouseId, instruction)));
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
