package cn.aitplus.wcs.infra.service.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.aitplus.wcs.core.domain.model.Instruction;

import java.util.List;

public interface InstructionsService {

    IPage<Instruction> queryByPage(Long wareHouseId, IPage<Instruction> page, Instruction instructions);

    List<Instruction> queryList(Long wareHouseId, Instruction instructions);

    void insertBatch(List<Instruction> collect);

    int deleteByTaskId(Long warehouseId, Long taskId);
}
