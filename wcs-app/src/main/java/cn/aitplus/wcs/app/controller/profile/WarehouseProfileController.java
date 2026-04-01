package cn.aitplus.wcs.app.controller.profile;

import cn.aitplus.wcs.common.domain.AjaxResult;
import cn.aitplus.wcs.core.domain.model.profile.ProfileChainNode;
import cn.aitplus.wcs.core.domain.model.profile.WarehouseProfile;
import cn.aitplus.wcs.infra.service.profile.WarehouseProfileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Api(tags = "仓库配置管理")
@RequestMapping("/api/{wareHouseId}/profile")
public class WarehouseProfileController {

    private final WarehouseProfileService profileService;

    public WarehouseProfileController(WarehouseProfileService profileService) {
        this.profileService = profileService;
    }

    @ApiOperation("查询当前仓库配置")
    @GetMapping
    public AjaxResult<WarehouseProfile> getProfile(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId) {
        return AjaxResult.success(profileService.getProfile(wareHouseId));
    }

    @ApiOperation("查询所有仓库配置")
    @GetMapping("/all")
    public AjaxResult<List<WarehouseProfile>> listAll(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId) {
        return AjaxResult.success(profileService.listAll());
    }

    @ApiOperation("创建仓库配置")
    @PostMapping
    public AjaxResult<WarehouseProfile> createProfile(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @RequestBody WarehouseProfile profile) {
        profile.setWarehouseId(wareHouseId);
        return AjaxResult.success(profileService.createProfile(profile));
    }

    @ApiOperation("更新仓库配置")
    @PutMapping
    public AjaxResult<WarehouseProfile> updateProfile(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @RequestBody WarehouseProfile profile) {
        profile.setWarehouseId(wareHouseId);
        return AjaxResult.success(profileService.updateProfile(profile));
    }

    @ApiOperation("删除仓库配置（含所有责任链节点）")
    @DeleteMapping
    public AjaxResult<Void> deleteProfile(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId) {
        profileService.deleteProfile(wareHouseId);
        return AjaxResult.success();
    }

    // ========== 责任链节点 ==========

    @ApiOperation("查询指定责任链的节点列表")
    @GetMapping("/chains/{chainName}")
    public AjaxResult<List<ProfileChainNode>> getChainNodes(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("链名称") @PathVariable("chainName") String chainName) {
        return AjaxResult.success(profileService.getChainNodes(wareHouseId, chainName));
    }

    @ApiOperation("查询当前仓库所有责任链节点")
    @GetMapping("/chains")
    public AjaxResult<List<ProfileChainNode>> getAllChainNodes(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId) {
        return AjaxResult.success(profileService.getAllChainNodes(wareHouseId));
    }

    @ApiOperation("替换指定责任链的节点列表（全量替换）")
    @PutMapping("/chains/{chainName}")
    public AjaxResult<Void> replaceChainNodes(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("链名称") @PathVariable("chainName") String chainName,
            @RequestBody List<ProfileChainNode> nodes) {
        profileService.replaceChainNodes(wareHouseId, chainName, nodes);
        return AjaxResult.success();
    }

    @ApiOperation("删除指定责任链的所有节点")
    @DeleteMapping("/chains/{chainName}")
    public AjaxResult<Void> deleteChainNodes(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("链名称") @PathVariable("chainName") String chainName) {
        profileService.deleteChainNodes(wareHouseId, chainName);
        return AjaxResult.success();
    }

    @ApiOperation("手动刷新缓存")
    @PostMapping("/cache/evict")
    public AjaxResult<Void> evictCache(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId) {
        profileService.evictCache(wareHouseId);
        return AjaxResult.success();
    }
}
