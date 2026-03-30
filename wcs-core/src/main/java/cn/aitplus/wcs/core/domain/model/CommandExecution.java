package cn.aitplus.wcs.core.domain.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel("指令执行审计")
public class CommandExecution implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("仓库ID")
    private Long warehouseId;

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("子任务ID")
    private Long subtaskId;

    @ApiModelProperty("指令ID")
    private Long instructionId;

    @ApiModelProperty("协议域：S7/MODBUS/RCS/OPC")
    private String domain;

    @ApiModelProperty("设备ID")
    private String deviceId;

    @ApiModelProperty("命令类型")
    private String commandType;

    @ApiModelProperty("幂等键")
    private String idempotencyKey;

    @ApiModelProperty("执行状态：SENT/ACK/RUNNING/DONE/ERROR/TIMEOUT/CANCELED")
    private String status;

    @ApiModelProperty("请求报文JSON")
    private String requestJson;

    @ApiModelProperty("响应报文JSON")
    private String responseJson;

    @ApiModelProperty("错误码")
    private String errorCode;

    @ApiModelProperty("错误信息")
    private String errorMessage;

    @ApiModelProperty("链路追踪ID")
    private String traceId;

    @ApiModelProperty("关联ID")
    private String correlationId;

    @ApiModelProperty("开始时间")
    private LocalDateTime startedAt;

    @ApiModelProperty("结束时间")
    private LocalDateTime endedAt;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
