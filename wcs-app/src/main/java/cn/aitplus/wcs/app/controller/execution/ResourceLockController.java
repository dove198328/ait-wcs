package cn.aitplus.wcs.app.controller.execution;

import cn.aitplus.wcs.common.domain.AjaxResult;
import cn.aitplus.wcs.common.domain.page.PageUtils;
import cn.aitplus.wcs.common.domain.page.TableDataInfo;
import cn.aitplus.wcs.core.domain.model.ResourceLock;
import cn.aitplus.wcs.infra.service.execution.ResourceLockService;
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

@RestController
@Api(tags = "资源锁管理")
@RequestMapping("/api/{wareHouseId}/resource-locks")
public class ResourceLockController {

    private final ResourceLockService resourceLockService;

    public ResourceLockController(ResourceLockService resourceLockService) {
        this.resourceLockService = resourceLockService;
    }

    @ApiOperation("查询资源锁列表")
    @PostMapping("/search")
    public AjaxResult<TableDataInfo<ResourceLock>> queryList(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("页码") @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @ApiParam("每页条数") @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestBody(required = false) ResourceLock resourceLock) {
        ResourceLock query = resourceLock == null ? new ResourceLock() : resourceLock;
        query.setWarehouseId(wareHouseId);
        if (PageUtils.hasOnlyOnePageParam(pageNum, pageSize)) {
            return AjaxResult.error("pageNum和pageSize必须同时传入");
        }
        if (PageUtils.isPageQuery(pageNum, pageSize)) {
            IPage<ResourceLock> page = new Page<>(pageNum, pageSize);
            return AjaxResult.success(TableDataInfo.build(resourceLockService.queryByPage(wareHouseId, page, query)));
        }
        return AjaxResult.success(TableDataInfo.build(resourceLockService.queryList(wareHouseId, query)));
    }

    @ApiOperation("根据ID查询资源锁")
    @GetMapping("/{id}")
    public AjaxResult<ResourceLock> queryById(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("锁ID") @PathVariable("id") Long id) {
        return AjaxResult.success(resourceLockService.queryById(wareHouseId, id));
    }

    @ApiOperation("根据锁键查询资源锁")
    @GetMapping("/lock/{lockKey}")
    public AjaxResult<ResourceLock> queryByLockKey(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("锁键") @PathVariable("lockKey") String lockKey) {
        return AjaxResult.success(resourceLockService.queryByLockKey(wareHouseId, lockKey));
    }

    @ApiOperation("获取资源锁")
    @PostMapping("/acquire")
    public AjaxResult<ResourceLock> acquire(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("过期秒数，默认30") @RequestParam(value = "expireSeconds", required = false) Integer expireSeconds,
            @RequestBody ResourceLock lockRequest) {
        return AjaxResult.success(resourceLockService.acquire(wareHouseId, lockRequest, expireSeconds));
    }

    @ApiOperation("释放资源锁（仅持有者）")
    @PostMapping("/release")
    public AjaxResult<Void> release(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("锁键") @RequestParam("lockKey") String lockKey,
            @ApiParam("持有者类型") @RequestParam("ownerType") String ownerType,
            @ApiParam("持有者ID") @RequestParam("ownerId") String ownerId) {
        resourceLockService.release(wareHouseId, lockKey, ownerType, ownerId);
        return AjaxResult.success();
    }

    @ApiOperation("续期资源锁（仅持有者）")
    @PostMapping("/renew")
    public AjaxResult<ResourceLock> renew(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("锁键") @RequestParam("lockKey") String lockKey,
            @ApiParam("持有者类型") @RequestParam("ownerType") String ownerType,
            @ApiParam("持有者ID") @RequestParam("ownerId") String ownerId,
            @ApiParam("过期秒数，默认30") @RequestParam(value = "expireSeconds", required = false) Integer expireSeconds) {
        return AjaxResult.success(resourceLockService.renew(wareHouseId, lockKey, ownerType, ownerId, expireSeconds));
    }

    @ApiOperation("强制释放资源锁（运维接口）")
    @PostMapping("/force-release/{lockKey}")
    public AjaxResult<Void> forceRelease(
            @ApiParam("仓库ID") @PathVariable("wareHouseId") Long wareHouseId,
            @ApiParam("锁键") @PathVariable("lockKey") String lockKey) {
        resourceLockService.forceRelease(wareHouseId, lockKey);
        return AjaxResult.success();
    }
}
