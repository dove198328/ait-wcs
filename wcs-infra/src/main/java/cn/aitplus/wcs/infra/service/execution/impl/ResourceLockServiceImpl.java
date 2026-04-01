package cn.aitplus.wcs.infra.service.execution.impl;

import cn.aitplus.wcs.core.domain.model.execution.ResourceLock;
import cn.aitplus.wcs.infra.persistence.execution.ResourceLockMapper;
import cn.aitplus.wcs.infra.service.execution.ResourceLockService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResourceLockServiceImpl implements ResourceLockService {

    private static final Logger log = LoggerFactory.getLogger(ResourceLockServiceImpl.class);

    private final ResourceLockMapper resourceLockMapper;

    public ResourceLockServiceImpl(ResourceLockMapper resourceLockMapper) {
        this.resourceLockMapper = resourceLockMapper;
    }

    @Override
    public IPage<ResourceLock> queryByPage(Long wareHouseId, IPage<ResourceLock> page, ResourceLock query) {
        return resourceLockMapper.queryByPage(wareHouseId, page, query);
    }

    @Override
    public List<ResourceLock> queryList(Long wareHouseId, ResourceLock query) {
        return resourceLockMapper.queryList(wareHouseId, query);
    }

    @Override
    public ResourceLock queryById(Long wareHouseId, Long id) {
        return resourceLockMapper.queryById(wareHouseId, id);
    }

    @Override
    public ResourceLock queryByLockKey(Long wareHouseId, String lockKey) {
        return resourceLockMapper.queryByLockKey(wareHouseId, lockKey);
    }

    @Override
    @Transactional
    public ResourceLock acquire(Long wareHouseId, ResourceLock lockRequest, Integer expireSeconds) {
        if (lockRequest.getLockKey() == null || lockRequest.getLockKey().isEmpty()) {
            throw new IllegalArgumentException("lockKey不能为空");
        }
        if (lockRequest.getOwnerType() == null || lockRequest.getOwnerType().isEmpty()) {
            throw new IllegalArgumentException("ownerType不能为空");
        }
        if (lockRequest.getOwnerId() == null || lockRequest.getOwnerId().isEmpty()) {
            throw new IllegalArgumentException("ownerId不能为空");
        }
        int seconds = (expireSeconds == null || expireSeconds <= 0) ? 30 : expireSeconds;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusSeconds(seconds);

        int updated = resourceLockMapper.updateForAcquire(
                wareHouseId, lockRequest.getLockKey(), lockRequest.getOwnerType(), lockRequest.getOwnerId(),
                "HELD", expireAt, now);
        if (updated > 0) {
            return resourceLockMapper.queryByLockKey(wareHouseId, lockRequest.getLockKey());
        }

        lockRequest.setWarehouseId(wareHouseId);
        lockRequest.setStatus("HELD");
        lockRequest.setExpireAt(expireAt);
        try {
            int inserted = resourceLockMapper.insertIfAbsent(lockRequest);
            if (inserted > 0) {
                return resourceLockMapper.queryByLockKey(wareHouseId, lockRequest.getLockKey());
            }
        } catch (DuplicateKeyException ex) {
            log.debug("锁并发插入冲突，继续校验持有者 lockKey={}", lockRequest.getLockKey(), ex);
        }

        int renewed = resourceLockMapper.renewByOwner(
                wareHouseId, lockRequest.getLockKey(), lockRequest.getOwnerType(), lockRequest.getOwnerId(),
                expireAt, now);
        if (renewed > 0) {
            return resourceLockMapper.queryByLockKey(wareHouseId, lockRequest.getLockKey());
        }
        throw new IllegalStateException("资源已被占用，lockKey=" + lockRequest.getLockKey());
    }

    @Override
    @Transactional
    public void release(Long wareHouseId, String lockKey, String ownerType, String ownerId) {
        int updated = resourceLockMapper.releaseByOwner(wareHouseId, lockKey, ownerType, ownerId);
        if (updated == 0) {
            throw new IllegalStateException("释放锁失败：锁不存在或持有者不匹配");
        }
    }

    @Override
    @Transactional
    public ResourceLock renew(Long wareHouseId, String lockKey, String ownerType, String ownerId, Integer expireSeconds) {
        int seconds = (expireSeconds == null || expireSeconds <= 0) ? 30 : expireSeconds;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusSeconds(seconds);
        int updated = resourceLockMapper.renewByOwner(wareHouseId, lockKey, ownerType, ownerId, expireAt, now);
        if (updated == 0) {
            throw new IllegalStateException("续期失败：锁不存在、已过期或持有者不匹配");
        }
        return resourceLockMapper.queryByLockKey(wareHouseId, lockKey);
    }

    @Override
    @Transactional
    public void forceRelease(Long wareHouseId, String lockKey) {
        resourceLockMapper.forceRelease(wareHouseId, lockKey);
    }
}
