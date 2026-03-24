package cn.aitplus.wcs.core.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("tasks")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Api("任务实体")
public class Task implements Serializable {
    private static final long serialVersionUID = 4274666586085043531L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty("主键")
    private Long id;

    @TableField("workflow_def_id")
    @ApiModelProperty("流程定义ID")
    @NotNull(message = "流程定义ID不能为空")
    private String workflowDefId;

    @TableField("task_name")
    @ApiModelProperty("主任务名称")
    @NotEmpty(message = "主任务名称不能为空")
    private String taskName;

    @TableField("priority")
    @ApiModelProperty("主任务优先级")
    private Integer priority;

    @TableField("status")
    @ApiModelProperty("主任务状态")
    private String status;

    @TableField("started_at")
    @ApiModelProperty("开始时间")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    @ApiModelProperty("完成时间")
    private LocalDateTime completedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;

    @TableField("warehouse_id")
    @ApiModelProperty("仓库ID")
    private Long warehouseId;

    @TableField("biz_type")
    @ApiModelProperty("业务类型主类")
    private String bizType;

    @TableField("start_point")
    @ApiModelProperty("任务起点")
    private String startPoint;

    @TableField("end_point")
    @ApiModelProperty("任务终点")
    private String endPoint;

    @TableField("task_direction")
    @ApiModelProperty("任务流向")
    private String taskDirection;

    @TableField("task_number")
    @ApiModelProperty("任务编号")
    private String taskNumber;

    @TableField("vehicle_id")
    @ApiModelProperty("载具号")
    private String vehicleId;

    @TableField("task_type")
    @ApiModelProperty("任务类型")
    private String taskType;

    @TableField("task_phase")
    @ApiModelProperty("任务阶段")
    private String taskPhase;

    @TableField("process_instance_id")
    @ApiModelProperty("流程实例ID")
    private String processInstanceId;

    @TableField(value = "is_auto_start")
    @ApiModelProperty("是否自动启动：1-是，0-否")
    private Integer isAutoStart;

    @TableField("device_ids")
    @ApiModelProperty("设备ID列表")
    private String deviceIds;

    /**
     * 货位编码，例如 "A-01-01-02-F"  (巷道-列-层-深度)
     * 深度 F = Front 前排，B = Back 后排，S = Single 单深
     */
    @TableField("location")
    @ApiModelProperty("货位编码，例如 A-01-01-02-F")
    private String location;

    /**
     * 货位深度类型：SINGLE | FRONT | BACK
     */
    @TableField("depth")
    @ApiModelProperty("货位深度类型：SINGLE/FRONT/BACK")
    private String depth;

    /**
     * 对于 BACK 货位，标识前排是否为空；
     * true  表示前排无货，可直接存取；
     * false 表示前排有货，需要合并或移库。
     * 对于 SINGLE 或 FRONT 可置 null/true
     */
    @TableField("front_empty")
    @ApiModelProperty("前排是否为空")
    private Boolean frontEmpty;

    /**
     * 作业方向 IN/OUT/MOVE_LOCAL/MOVE_CROSS
     * 参见 WorkDirection 枚举
     */
    @TableField("work_direction")
    @ApiModelProperty("作业方向：IN/OUT/MOVE_LOCAL/MOVE_CROSS")
    private String workDirection;

    @TableField("wms_biz_id")
    @ApiModelProperty("WMS业务ID")
    private Long wmsBizId;

    @TableField("order_no")
    @ApiModelProperty("WMS订单号")
    private String orderNo;

    @TableField("aisle_no")
    @ApiModelProperty("巷道号")
    private Integer aisleNo;

    @TableField("process_definition_id")
    @ApiModelProperty("流程定义ID")
    private String processDefinitionId;

    @TableField("task_category")
    @ApiModelProperty("业务类型：OUTBOUND/INBOUND/INTERNAL/INVENTORY")
    private String taskCategory;

    @TableField("twins_no")
    @ApiModelProperty("组号")
    private String twinsNo;

    @TableField(exist = false)
    @ApiModelProperty("子任务列表")
    private List<SubTask> subtasks;

    @TableField(exist = false)
    @ApiModelProperty("扫码器设备ID")
    private String scannerDeviceId;

    @TableField(exist = false)
    @ApiModelProperty("是否临时移库任务")
    private Boolean isCK;

    @TableField(exist = false)
    @ApiModelProperty("任务下发情况")
    private Integer taskDistribution;

    @TableField(exist = false)
    @ApiModelProperty("入库标志：true-库内，false-库外")
    private Boolean rkkFlag;
}

