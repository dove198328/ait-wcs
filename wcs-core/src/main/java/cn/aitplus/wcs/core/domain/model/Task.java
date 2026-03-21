package cn.aitplus.wcs.core.domain.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel("任务实体")
public class Task {
    @ApiModelProperty("主键")
    private Long id;
    @ApiModelProperty("流程定义ID")
    @NotNull(message = "流程定义ID不能为空")
    private String workflowDefId;
    @ApiModelProperty("主任务名称")
    @NotEmpty(message = "主任务名称不能为空")
    private String taskName;
    @ApiModelProperty("主任务优先级")
    private Integer priority;
    @ApiModelProperty("主任务状态")
    private String status;
    @ApiModelProperty("开始时间")
    private LocalDateTime startedAt;
    @ApiModelProperty("完成时间")
    private LocalDateTime completedAt;
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
    @ApiModelProperty("仓库ID")
    private Long warehouseId;
    @ApiModelProperty("业务类型主类")
    private String bizType;
    @ApiModelProperty("任务起点")
    private String startPoint;
    @ApiModelProperty("任务终点")
    private String endPoint;
    @ApiModelProperty("任务流向")
    private String taskDirection;
    @ApiModelProperty("任务编号")
    private String taskNumber;
    @ApiModelProperty("载具号")
    private String vehicleId;
    @ApiModelProperty("任务类型")
    private String taskType;
    @ApiModelProperty("任务阶段")
    private String taskPhase;
    @ApiModelProperty("流程实例ID")
    private String processInstanceId;
    @ApiModelProperty("是否自动启动：1-是，0-否")
    private Integer isAutoStart;
    @ApiModelProperty("设备ID列表")
    private String deviceIds;
    /**
     * 货位编码，例如 "A-01-01-02-F"  (巷道-列-层-深度)
     * 深度 F = Front 前排，B = Back 后排，S = Single 单深
     */
    @ApiModelProperty("货位编码，例如 A-01-01-02-F")
    private String location;
    /**
     * 货位深度类型：SINGLE | FRONT | BACK
     */
    @ApiModelProperty("货位深度类型：SINGLE/FRONT/BACK")
    private String depth;
    /**
     * 对于 BACK 货位，标识前排是否为空；
     * true  表示前排无货，可直接存取；
     * false 表示前排有货，需要合并或移库。
     * 对于 SINGLE 或 FRONT 可置 null/true
     */
    @ApiModelProperty("前排是否为空")
    private Boolean frontEmpty;
    /**
     * 作业方向 IN/OUT/MOVE_LOCAL/MOVE_CROSS
     * 参见 WorkDirection 枚举
     */
    @ApiModelProperty("作业方向：IN/OUT/MOVE_LOCAL/MOVE_CROSS")
    private String workDirection;
    @ApiModelProperty("WMS业务ID")
    private Long wmsBizId;
    @ApiModelProperty("WMS订单号")
    private String orderNo;
    @ApiModelProperty("巷道号")
    private Integer aisleNo;
    @ApiModelProperty("流程定义ID")
    private String processDefinitionId;
    @ApiModelProperty("业务类型：OUTBOUND/INBOUND/INTERNAL/INVENTORY")
    private String taskCategory;
    @ApiModelProperty("组号")
    private String twinsNo;
    @ApiModelProperty("子任务列表")
    private List<SubTask> subtasks;
    @ApiModelProperty("扫码器设备ID")
    private String scannerDeviceId;
    @ApiModelProperty("是否临时移库任务")
    private Boolean isCK;
    @ApiModelProperty("任务下发情况")
    private Integer taskDistribution;
    @ApiModelProperty("入库标志：true-库内，false-库外")
    private Boolean rkkFlag;
}

