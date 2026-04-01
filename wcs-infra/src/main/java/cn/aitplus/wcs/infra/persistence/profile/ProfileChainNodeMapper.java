package cn.aitplus.wcs.infra.persistence.profile;

import cn.aitplus.wcs.core.domain.model.profile.ProfileChainNode;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProfileChainNodeMapper {

    List<ProfileChainNode> selectByChain(@Param("warehouseId") Long warehouseId,
                                         @Param("chainName") String chainName);

    List<ProfileChainNode> selectByWarehouseId(@Param("warehouseId") Long warehouseId);

    List<String> selectChainNamesByWarehouseId(@Param("warehouseId") Long warehouseId);

    int insert(ProfileChainNode node);

    int batchInsert(@Param("list") List<ProfileChainNode> nodes);

    int update(ProfileChainNode node);

    int deleteByWarehouseIdAndChain(@Param("warehouseId") Long warehouseId,
                                    @Param("chainName") String chainName);

    int deleteByWarehouseId(@Param("warehouseId") Long warehouseId);
}
