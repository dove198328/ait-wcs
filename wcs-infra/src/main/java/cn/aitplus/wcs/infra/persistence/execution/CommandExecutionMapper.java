package cn.aitplus.wcs.infra.persistence.execution;

import cn.aitplus.wcs.core.domain.model.CommandExecution;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommandExecutionMapper {

    IPage<CommandExecution> queryByPage(@Param("wareHouseId") Long wareHouseId,
                                        @Param("page") IPage<CommandExecution> page,
                                        @Param("ew") CommandExecution query);

    List<CommandExecution> queryList(@Param("wareHouseId") Long wareHouseId,
                                     @Param("ew") CommandExecution query);

    CommandExecution queryById(@Param("wareHouseId") Long wareHouseId, @Param("id") Long id);

    CommandExecution queryByIdempotencyKey(@Param("wareHouseId") Long wareHouseId,
                                           @Param("idempotencyKey") String idempotencyKey);

    int insert(CommandExecution commandExecution);

    int updateById(CommandExecution commandExecution);

    int updateStatusById(@Param("wareHouseId") Long wareHouseId,
                         @Param("id") Long id,
                         @Param("status") String status,
                         @Param("responseJson") String responseJson,
                         @Param("errorCode") String errorCode,
                         @Param("errorMessage") String errorMessage,
                         @Param("endedAt") LocalDateTime endedAt);
}
