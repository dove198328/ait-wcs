package cn.aitplus.wcs.core.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 子任务实体类
 * 负责管理和跟踪子任务的执行过程
 */
@Data
@TableName("subtasks")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("子任务实体")
public class SubTask implements Serializable {
    private static final long serialVersionUID = -1345487262126114290L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty("主键")
    private Long id;

    @TableField("task_id")
    @ApiModelProperty("任务ID")
    private Long taskId;

    @TableField("subtask_def_id")
    @ApiModelProperty("子任务定义ID")
    private String subtaskDefId;

    @TableField("name")
    @ApiModelProperty("子任务名称")
    private String name;

    @TableField("priority")
    @ApiModelProperty("子任务优先级")
    private Integer priority;

    @TableField("status")
    @ApiModelProperty("子任务状态")
    private String status;

    @TableField("compensation")
    @ApiModelProperty("子任务补偿策略")
    private String compensation;

    @TableField("allow_manual")
    @ApiModelProperty("是否允许手动执行")
    private Integer allowManual;

    @TableField(value = "created_at", fill = FieldFill.INSERT,updateStrategy = FieldStrategy.NEVER)
    @ApiModelProperty("创建时间")
    private Date createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty("更新时间")
    private Date updatedAt;

    @TableField("warehouse_id")
    @ApiModelProperty("仓库ID")
    private Long warehouseId;

    @TableField("current_instruction_index")
    @ApiModelProperty("当前执行的指令索引")
    private Integer currentInstructionIndex = 0;

    @TableField("completed_at")
    @ApiModelProperty("完成时间")
    private Date completedAt;

    @TableField("remark")
    @ApiModelProperty("备注")
    private String remark;

    @TableField("selected_device_id")
    @ApiModelProperty("选中的设备ID")
    private String selectedDeviceId;

    @TableField("activity_instance_id")
    @ApiModelProperty("活动实例ID")
    private String activityInstanceId;

    @TableField("area")
    @ApiModelProperty("区域")
    private String area;

    @TableField(exist = false)
    @ApiModelProperty("指令列表")
    private List<Instruction> instructions;

    @TableField("workflow_def_id")
    @ApiModelProperty("流程定义ID")
    private String workflowDefId;

    @ApiModelProperty("是否推进下一个流程")
    private Boolean isStartNextProcess;

    @ApiModelProperty("推进流程释放的设备ID")
    private String freeDeviceId;

    @ApiModelProperty("是否巷道过滤")
    private Boolean checkAisle;
}

