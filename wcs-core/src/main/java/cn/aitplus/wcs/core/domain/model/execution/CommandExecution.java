package cn.aitplus.wcs.core.domain.model.execution;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel("CommandExecution")
public class CommandExecution implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("Primary key")
    private Long id;

    @ApiModelProperty("Warehouse id")
    private Long warehouseId;

    @ApiModelProperty("Task id")
    private Long taskId;

    @ApiModelProperty("Subtask id")
    private Long subtaskId;

    @ApiModelProperty("Instruction id")
    private Long instructionId;

    @ApiModelProperty("Protocol domain: S7/MODBUS/RCS/OPC")
    private String domain;

    @ApiModelProperty("Device id")
    private String deviceId;

    @ApiModelProperty("Command type")
    private String commandType;

    @ApiModelProperty("Idempotency key")
    private String idempotencyKey;

    @ApiModelProperty("Status: SENT/ACK/RUNNING/DONE/ERROR/TIMEOUT/CANCELED")
    private String status;

    @ApiModelProperty("Request JSON")
    private String requestJson;

    @ApiModelProperty("Response JSON")
    private String responseJson;

    @ApiModelProperty("Error code")
    private String errorCode;

    @ApiModelProperty("Error message")
    private String errorMessage;

    @ApiModelProperty("Trace id")
    private String traceId;

    @ApiModelProperty("Correlation id")
    private String correlationId;

    @ApiModelProperty("Started at")
    private LocalDateTime startedAt;

    @ApiModelProperty("Ended at")
    private LocalDateTime endedAt;

    @ApiModelProperty("Created at")
    private LocalDateTime createdAt;

    @ApiModelProperty("Updated at")
    private LocalDateTime updatedAt;
}
