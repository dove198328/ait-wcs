package cn.aitplus.wcs.core.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("instructions")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("指令实体")
public class Instruction {

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty("主键")
    private Long id;

    @TableField("warehouse_id")
    @ApiModelProperty("仓库ID")
    private Long warehouseId;

    @TableField("subtask_id")
    @ApiModelProperty("子任务ID")
    @NotNull(message = "子任务ID不能为空")
    private Long subtaskId;

    @TableField("sequence")
    @ApiModelProperty("指令顺序")
    @NotNull(message = "指令顺序不能为空")
    private Integer sequence;

    @TableField("protocol")
    @ApiModelProperty("指令协议")
    @NotEmpty(message = "指令协议不能为空")
    private String protocol;

    @ApiModelProperty("指令参数列表")
    private List<Command> commands;

    /**
     * 指令额外参数(json字符串)
     *   - 调度元数据（timeoutMs、retryCount、delayMs、backoffFactor、dependsOn）
     *   - 业务参数对象（params）
     *   params{
     *     "commands": [
     *       {"pointId": "A", "value": 1},
     *       {"pointId": "B", "value": 2}
     *     ]
     *   }
     */
    @TableField("params")
    @ApiModelProperty("指令额外参数（JSON字符串）")
    private String params;

    @TableField("status")
    @ApiModelProperty("指令状态")
    @NotEmpty(message = "指令状态不能为空")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;

    @TableField("task_id")
    @ApiModelProperty("任务ID")
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @TableField("remark")
    @ApiModelProperty("备注信息")
    private String remark;

    @TableField(value = "device_id")
    @ApiModelProperty("可执行设备ID")
    @NotEmpty(message = "设备ID不能为空")
    private String deviceId;

    /**
     * 是否采用消息事件驱动等待（true 则 Dispatcher 监听 pointIds 条件后唤醒流程）
     * 非持久化字段，可从 params 或流程定义中解析。
     */
    @TableField("message_event")
    @ApiModelProperty("是否采用消息事件驱动等待")
    private Boolean messageEvent;

    /**
     * 本指令与上一条 messageEvent 指令的逻辑关系：AND / OR / NOT。
     * 第一个 messageEvent 指令可为空或忽略。
     * 非持久化字段，仅用于等待条件拼装。
     */
    @ApiModelProperty("消息事件逻辑关系（and/or/not）")
    private String eventLogic;

}
