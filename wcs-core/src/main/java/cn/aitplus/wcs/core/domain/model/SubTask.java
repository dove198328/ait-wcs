package cn.aitplus.wcs.core.domain.model;

import lombok.Data;

import java.util.Date;

/**
 * 子任务实体类
 * 负责管理和跟踪子任务的执行过程
 */
@Data
public class SubTask {
    // 主键
    private Long id;
    // 任务ID
    private Long taskId;
    // 子任务ID
    private String subtaskDefId;
    // 子任务名称
    private String name;
    // 子任务优先级
    private Integer priority;
    // 子任务状态
    private String status;
    // 子任务补偿策略
    private String compensation;
    // 是否允许手动执行
    private Integer allowManual;
    // 创建时间
    private Date createdAt;
    // 更新时间
    private Date updatedAt;
    // 仓库ID
    private Long warehouseId;
    // 当前执行的指令索引
    private Integer currentInstructionIndex = 0;
    // 完成时间
    private Date completedAt;
    // 备注
    private String remark;
    // 选中的设备ID
    private Integer selectedDeviceId;
    // 活动实例ID
    private String activityInstanceId;
    // 区域
    private String area;
    // 流程定义ID
    private String workflowDefId;
    // 是否推进下一个流程
    private Boolean isStartNextProcess;
    // 推进流程释放的设备ID
    private String freeDeviceId;
    // 是否巷道过滤
    private Boolean checkAisle;
}

