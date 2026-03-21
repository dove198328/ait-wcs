package cn.aitplus.wcs.infra.service.execution.impl;

import cn.aitplus.wcs.core.domain.model.CommandExecution;
import cn.aitplus.wcs.infra.persistence.execution.CommandExecutionMapper;
import cn.aitplus.wcs.infra.service.execution.CommandExecutionService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommandExecutionServiceImpl implements CommandExecutionService {

    private final CommandExecutionMapper commandExecutionMapper;

    public CommandExecutionServiceImpl(CommandExecutionMapper commandExecutionMapper) {
        this.commandExecutionMapper = commandExecutionMapper;
    }

    @Override
    public IPage<CommandExecution> queryByPage(Long wareHouseId, IPage<CommandExecution> page, CommandExecution query) {
        return commandExecutionMapper.queryByPage(wareHouseId, page, query);
    }

    @Override
    public List<CommandExecution> queryList(Long wareHouseId, CommandExecution query) {
        return commandExecutionMapper.queryList(wareHouseId, query);
    }

    @Override
    public CommandExecution queryById(Long wareHouseId, Long id) {
        return commandExecutionMapper.queryById(wareHouseId, id);
    }

    @Override
    public CommandExecution queryByIdempotencyKey(Long wareHouseId, String idempotencyKey) {
        return commandExecutionMapper.queryByIdempotencyKey(wareHouseId, idempotencyKey);
    }

    @Override
    public CommandExecution create(Long wareHouseId, CommandExecution commandExecution) {
        commandExecution.setWarehouseId(wareHouseId);
        if (commandExecution.getStatus() == null || commandExecution.getStatus().isEmpty()) {
            commandExecution.setStatus("SENT");
        }
        if (commandExecution.getStartedAt() == null) {
            commandExecution.setStartedAt(LocalDateTime.now());
        }
        CommandExecution existed = commandExecutionMapper.queryByIdempotencyKey(
                wareHouseId, commandExecution.getIdempotencyKey());
        if (existed != null) {
            return existed;
        }
        try {
            commandExecutionMapper.insert(commandExecution);
            return commandExecution;
        } catch (DuplicateKeyException ex) {
            return commandExecutionMapper.queryByIdempotencyKey(wareHouseId, commandExecution.getIdempotencyKey());
        }
    }

    @Override
    public CommandExecution update(Long wareHouseId, CommandExecution commandExecution) {
        commandExecution.setWarehouseId(wareHouseId);
        commandExecutionMapper.updateById(commandExecution);
        return commandExecutionMapper.queryById(wareHouseId, commandExecution.getId());
    }

    @Override
    public int updateStatus(Long wareHouseId, Long id, String status, String responseJson, String errorCode, String errorMessage) {
        LocalDateTime endedAt = null;
        if ("DONE".equals(status) || "ERROR".equals(status) || "TIMEOUT".equals(status) || "CANCELED".equals(status)) {
            endedAt = LocalDateTime.now();
        }
        return commandExecutionMapper.updateStatusById(
                wareHouseId, id, status, responseJson, errorCode, errorMessage, endedAt);
    }
}
