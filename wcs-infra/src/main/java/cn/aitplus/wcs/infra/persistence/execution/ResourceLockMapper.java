package cn.aitplus.wcs.infra.persistence.execution;

import cn.aitplus.wcs.core.domain.model.execution.ResourceLock;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ResourceLockMapper {

    IPage<ResourceLock> queryByPage(@Param("wareHouseId") Long wareHouseId,
                                    @Param("page") IPage<ResourceLock> page,
                                    @Param("ew") ResourceLock query);

    List<ResourceLock> queryList(@Param("wareHouseId") Long wareHouseId,
                                 @Param("ew") ResourceLock query);

    ResourceLock queryById(@Param("wareHouseId") Long wareHouseId, @Param("id") Long id);

    ResourceLock queryByLockKey(@Param("wareHouseId") Long wareHouseId, @Param("lockKey") String lockKey);

    int updateForAcquire(@Param("wareHouseId") Long wareHouseId,
                         @Param("lockKey") String lockKey,
                         @Param("ownerType") String ownerType,
                         @Param("ownerId") String ownerId,
                         @Param("status") String status,
                         @Param("expireAt") LocalDateTime expireAt,
                         @Param("now") LocalDateTime now);

    int insertIfAbsent(ResourceLock resourceLock);

    int renewByOwner(@Param("wareHouseId") Long wareHouseId,
                     @Param("lockKey") String lockKey,
                     @Param("ownerType") String ownerType,
                     @Param("ownerId") String ownerId,
                     @Param("expireAt") LocalDateTime expireAt,
                     @Param("now") LocalDateTime now);

    int releaseByOwner(@Param("wareHouseId") Long wareHouseId,
                       @Param("lockKey") String lockKey,
                       @Param("ownerType") String ownerType,
                       @Param("ownerId") String ownerId);

    int forceRelease(@Param("wareHouseId") Long wareHouseId, @Param("lockKey") String lockKey);
}
