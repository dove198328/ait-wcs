package cn.aitplus.wcs.core.domain.model;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkflowDefinition {
    // 主键
    private Long id;
    // 业务类型
    @NotEmpty(message = "业务类型不能为空")
    private String bizType;
    // 流程定义ID
    private String workflowId;
    // 配置
    @NotEmpty(message = "配置不能为空")
    private String config; // JSON 或 BPMN XML
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间
    private LocalDateTime updatedAt;
    // 流程数据
    private String processData;
    // 流程名称
    private String name;
    // 优先级
    private Integer priority;
    // 仓库ID
    private Integer warehouseId;

    private List<SubtaskDefinition> subtaskDefinitions;

    private int isAutoStart;

    private String firstSubDef;
    // 状态 0 无效， 1有效
    private int status = 1;
    // 流程ID
    private String processDefId;
    // 任务设备明细定义
    private TaskDefinition taskDefinition;
    // 作业流向
    private String workDirection;
    // 部署ID
    private String deployId;
}
