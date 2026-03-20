package cn.aitplus.wcs.core.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Task {
    // 主键
    private Long id;
    // 流程定义ID
    @NotNull(message = "流程定义ID不能为空")
    private String workflowDefId;
    // 主任务名称
    @NotEmpty(message = "主任务名称不能为空")
    private String taskName;
    // 主任务优先级
    private Integer priority;
    // 主任务状态
    private String status;
    // 开始时间
    private LocalDateTime startedAt;
    // 完成时间
    private LocalDateTime completedAt;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间
    private LocalDateTime updatedAt;
    // 仓库ID
    private Long warehouseId;
    // 业务类型主类
    private String bizType;
    // 任务起点
    private String startPoint;
    // 任务终点
    private String endPoint;
    // 任务流向
    private String taskDirection;
    // 任务编号
    private String taskNumber;
    // 载具号
    private String vehicleId;
    // 任务类型
    private String taskType;
    // 任务阶段
    private String taskPhase;
    // 流程实例ID
    private String processInstanceId;
    // 是否自动启动 1.是 0.否
    private Integer isAutoStart;
    // 设备ID列表
    private String deviceIds;
    /**
     * 货位编码，例如 "A-01-01-02-F"  (巷道-列-层-深度)
     * 深度 F = Front 前排，B = Back 后排，S = Single 单深
     */
    private String location;
    /**
     * 货位深度类型：SINGLE | FRONT | BACK
     */
    private String depth;
    /**
     * 对于 BACK 货位，标识前排是否为空；
     * true  表示前排无货，可直接存取；
     * false 表示前排有货，需要合并或移库。
     * 对于 SINGLE 或 FRONT 可置 null/true
     */
    private Boolean frontEmpty;
    /**
     * 作业方向 IN/OUT/MOVE_LOCAL/MOVE_CROSS
     * 参见 WorkDirection 枚举
     */
    private String workDirection;
    // WMS业务ID
    private Long wmsBizId;
    // WMS订单号
    private String orderNo;
    // 巷道号
    private Integer aisleNo;
    // 流程定义ID
    private String processDefinitionId;
    // 业务类型：出库 OUTBOUND入库 INBOUND移库 INTERNAL盘点 INVENTORY
    private String taskCategory;
    // 组号
    private String twinsNo;
    // 子任务列表
    private List<SubTasks> subtasks;
    // 扫码器设备ID
    private String scannerDeviceId;
    // 是否临时移库任务
    private Boolean isCK;
    // 任务下发情况
    private Integer taskDistribution;
    // 入库标志，true-库内，false-库外
    private Boolean rkkFlag;
}

