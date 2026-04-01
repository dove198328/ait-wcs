package cn.aitplus.wcs.infra.persistence.task;

import cn.aitplus.wcs.core.domain.model.profile.WareHouseMode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WareHouseModeMapper extends BaseMapper<WareHouseMode> {
}
