package cn.aitplus.wcs.core.domain.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 子任务实体类
 * 负责管理和跟踪子任务的执行过程
 */
@Data
@ApiModel("子任务实体")
public class SubTask {
    @ApiModelProperty("主键")
    private Long id;
    @ApiModelProperty("任务ID")
    private Long taskId;
    @ApiModelProperty("子任务定义ID")
    private String subtaskDefId;
    @ApiModelProperty("子任务名称")
    private String name;
    @ApiModelProperty("子任务优先级")
    private Integer priority;
    @ApiModelProperty("子任务状态")
    private String status;
    @ApiModelProperty("子任务补偿策略")
    private String compensation;
    @ApiModelProperty("是否允许手动执行")
    private Integer allowManual;
    @ApiModelProperty("创建时间")
    private Date createdAt;
    @ApiModelProperty("更新时间")
    private Date updatedAt;
    @ApiModelProperty("仓库ID")
    private Long warehouseId;
    @ApiModelProperty("当前执行的指令索引")
    private Integer currentInstructionIndex = 0;
    @ApiModelProperty("完成时间")
    private Date completedAt;
    @ApiModelProperty("备注")
    private String remark;
    @ApiModelProperty("选中的设备ID")
    private Integer selectedDeviceId;
    @ApiModelProperty("活动实例ID")
    private String activityInstanceId;
    @ApiModelProperty("区域")
    private String area;
    @ApiModelProperty("流程定义ID")
    private String workflowDefId;
    @ApiModelProperty("是否推进下一个流程")
    private Boolean isStartNextProcess;
    @ApiModelProperty("推进流程释放的设备ID")
    private String freeDeviceId;
    @ApiModelProperty("是否巷道过滤")
    private Boolean checkAisle;
}

