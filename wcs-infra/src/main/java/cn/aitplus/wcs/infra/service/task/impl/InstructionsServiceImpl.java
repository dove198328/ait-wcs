package cn.aitplus.wcs.infra.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import cn.aitplus.wcs.core.domain.model.Instruction;
import cn.aitplus.wcs.infra.persistence.task.InstructionsMapper;
import cn.aitplus.wcs.infra.service.task.InstructionsService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
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

    @Override
    public int deleteByTaskId(Long warehouseId, Long taskId) {
        if (taskId == null) {
            return 0;
        }
        try {
            // 使用LambdaQueryWrapper构建删除条件
            LambdaQueryWrapper<Instruction> wrapper = Wrappers.lambdaQuery(Instruction.class)
                    .eq(Instruction::getTaskId, taskId);
            // 执行删除操作
            int count = instructionsMapper.delete(wrapper);
            return count;
        } catch (Exception e) {
            log.error("删除子任务{}的指令时发生异常", taskId, e);
            return 0;
        }
    }
}
