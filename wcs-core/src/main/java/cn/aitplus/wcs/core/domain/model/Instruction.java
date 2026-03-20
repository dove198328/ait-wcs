package cn.aitplus.wcs.core.domain.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Instruction {
    // 主键
    private Long id;
    // 子任务ID
    @NotNull(message = "子任务ID不能为空")
    private Long subtaskId;
    // 指令顺序
    @NotNull(message = "指令顺序不能为空")
    private Integer sequence;
    // 指令协议
    @NotEmpty(message = "指令协议不能为空")
    private String protocol;
    // 指令参数
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
    private String params;
    // 指令状态
    @NotEmpty(message = "指令状态不能为空")
    private String status;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间
    private LocalDateTime updatedAt;
    // 任务ID
    @NotNull(message = "任务ID不能为空")
    private Long taskId;
    // 备注信息
    private String remark;
    // 可执行设备，可用设备ids
    @NotEmpty(message = "设备ID不能为空")
    private String deviceId;
    /**
     * 是否采用消息事件驱动等待（true 则 Dispatcher 监听 pointIds 条件后唤醒流程）
     * 非持久化字段，可从 params 或流程定义中解析。
     */
    private Boolean messageEvent;
    /**
     * 本指令与上一条 messageEvent 指令的逻辑关系：AND / OR / NOT。
     * 第一个 messageEvent 指令可为空或忽略。
     * 非持久化字段，仅用于等待条件拼装。
     */
    //消息事件逻辑关系（and：与，or：或，not：非）
    private String eventLogic;

}
