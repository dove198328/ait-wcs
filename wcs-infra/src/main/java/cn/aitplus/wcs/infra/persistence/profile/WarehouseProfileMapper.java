package cn.aitplus.wcs.infra.persistence.profile;

import cn.aitplus.wcs.core.domain.model.WarehouseProfile;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WarehouseProfileMapper {

    WarehouseProfile selectByWarehouseId(@Param("warehouseId") Long warehouseId);

    List<WarehouseProfile> selectAll();

    int insert(WarehouseProfile profile);

    int update(WarehouseProfile profile);

    int deleteByWarehouseId(@Param("warehouseId") Long warehouseId);
}
