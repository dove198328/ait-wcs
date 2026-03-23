package cn.aitplus.wcs.app.controller.task;

import cn.aitplus.wcs.common.domain.AjaxResult;
import cn.aitplus.wcs.core.domain.model.WcsChangeFlow;
import cn.aitplus.wcs.infra.service.task.WareHouseModeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(tags = "仓库模式")
@RestController
@RequestMapping("warehouseMode")
public class WareHouseModeController {
    @Resource
    private WareHouseModeService wareHouseModeService;

    @GetMapping("getCurrentMode")
    @ApiOperation("获取当前模式")
    public AjaxResult<String> getCurrentMode(Integer warehouseId) {
        return AjaxResult.success(wareHouseModeService.getCurrentMode(warehouseId));
    }

    @PostMapping("changeMode")
    @ApiOperation("切换模式")
    public AjaxResult<String> changeMode(@RequestBody WcsChangeFlow changeFlow) {
        return AjaxResult.success(wareHouseModeService.changeMode(changeFlow.getWarehouseId(), changeFlow.getMode()));
    }
}
