package cn.aitplus.wcs.infra.service.task.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.aitplus.wcs.core.domain.model.Instruction;
import cn.aitplus.wcs.infra.persistence.task.InstructionsMapper;
import cn.aitplus.wcs.infra.service.task.InstructionsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstructionsServiceImpl implements InstructionsService {

    private final InstructionsMapper instructionsMapper;

    public InstructionsServiceImpl(InstructionsMapper instructionsMapper) {
        this.instructionsMapper = instructionsMapper;
    }

    @Override
    public IPage<Instruction> queryByPage(Long wareHouseId, IPage<Instruction> page, Instruction instructions) {
        return instructionsMapper.queryByPage(wareHouseId, page, instructions);
    }

    @Override
    public List<Instruction> queryList(Long wareHouseId, Instruction instructions) {
        return instructionsMapper.queryList(wareHouseId, instructions);
    }

    @Override
    public void insertBatch(List<Instruction> collect) {
        instructionsMapper.insertbatch(collect);
    }
}
