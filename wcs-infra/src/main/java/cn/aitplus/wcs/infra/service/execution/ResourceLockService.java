package cn.aitplus.wcs.infra.service.execution;

import cn.aitplus.wcs.core.domain.model.ResourceLock;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface ResourceLockService {

    IPage<ResourceLock> queryByPage(Long wareHouseId, IPage<ResourceLock> page, ResourceLock query);

    List<ResourceLock> queryList(Long wareHouseId, ResourceLock query);

    ResourceLock queryById(Long wareHouseId, Long id);

    ResourceLock queryByLockKey(Long wareHouseId, String lockKey);

    ResourceLock acquire(Long wareHouseId, ResourceLock lockRequest, Integer expireSeconds);

    void release(Long wareHouseId, String lockKey, String ownerType, String ownerId);

    ResourceLock renew(Long wareHouseId, String lockKey, String ownerType, String ownerId, Integer expireSeconds);

    void forceRelease(Long wareHouseId, String lockKey);
}
