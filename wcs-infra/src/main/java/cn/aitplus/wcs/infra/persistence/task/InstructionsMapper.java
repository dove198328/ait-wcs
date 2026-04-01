package cn.aitplus.wcs.infra.persistence.task;

import cn.aitplus.wcs.core.domain.model.task.Instruction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * (Instructions)表数据库访问层
 */
public interface InstructionsMapper extends BaseMapper<Instruction> {

   IPage<Instruction> queryByPage(@Param("wareHouseId") Long wareHouseId, @Param("page") IPage<Instruction> page, @Param("ew") Instruction instructions);

   List<Instruction> queryList(@Param("wareHouseId") Long wareHouseId, @Param("ew") Instruction instructions);

    void insertbatch(List<Instruction> collect);

}

