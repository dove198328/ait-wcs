package cn.aitplus.wcs.infra.persistence.task;

import cn.aitplus.wcs.core.domain.model.Instruction;

import java.util.List;

/**
 * (Instructions)表数据库访问层
 */
public interface InstructionsMapper{

   IPage<Instruction> queryByPage(@Param("page") IPage<Instruction> page, @Param("ew") Instruction instructions);

   List<Instruction> queryList(@Param("ew") Instruction instructions);

    void insertbatch(List<Instruction> collect);

}

