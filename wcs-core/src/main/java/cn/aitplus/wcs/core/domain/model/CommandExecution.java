package cn.aitplus.wcs.core.domain.model;

import cn.aitplus.wcs.core.domain.enums.DomainEnums.CommandDomain;
import cn.aitplus.wcs.core.domain.enums.DomainEnums.CommandStatus;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommandExecution {

    private Long id;
    @NotBlank
    private String warehouseId;
    private Long taskId;
    private Long planId;
    private Long stepId;
    @NotNull
    private CommandDomain domain;
    @NotBlank
    private String deviceId;
    @NotBlank
    private String commandType;
    @NotBlank
    private String idempotencyKey;
    @NotNull
    private CommandStatus status;
    private String requestJson;
    private String responseJson;
    private String traceId;
    private String correlationId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}

