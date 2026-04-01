package cn.aitplus.wcs.infra.service.execution;

import cn.aitplus.wcs.core.domain.model.execution.CommandExecution;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface CommandExecutionService {

    IPage<CommandExecution> queryByPage(Long wareHouseId, IPage<CommandExecution> page, CommandExecution query);

    List<CommandExecution> queryList(Long wareHouseId, CommandExecution query);

    CommandExecution queryById(Long wareHouseId, Long id);

    CommandExecution queryByIdempotencyKey(Long wareHouseId, String idempotencyKey);

    CommandExecution create(Long wareHouseId, CommandExecution commandExecution);

    CommandExecution update(Long wareHouseId, CommandExecution commandExecution);

    int updateStatus(Long wareHouseId, Long id, String status, String responseJson, String errorCode, String errorMessage);
}
