package cn.aitplus.wcs.core.domain.model;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.*;
import com.alibaba.fastjson2.JSONObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "workflow_definitions", autoResultMap = true)
@ApiModel("流程定义实体类")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowDefinition {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("biz_type")
    @ApiModelProperty("二级分类，因为业务类型只在二级分类")
    @NotEmpty(message = "业务类型不能为空")
    private String bizType;

    @TableField("workflow_id")
    @ApiModelProperty("流程id")
    private String workflowId;

    @TableField("config")
    @ApiModelProperty("配置")
    @NotEmpty(message = "配置不能为空")
    private String config; // JSON 或 BPMN XML

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;

    @TableField("process_data")
    @ApiModelProperty("流程数据")
    private String processData;

    @TableField("name")
    @ApiModelProperty("流程名称")
    private String name;

    @TableField("priority")
    @ApiModelProperty("优先级,数值越大越优先")
    private Integer priority;

    @TableField("warehouse_id")
    @ApiModelProperty("仓库id")
    private Integer warehouseId;

    @TableField(value = "process_data_with_device",typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<SubtaskDefinition> subtaskDefinitions;

    @TableField(value = "is_auto_start")
    private int isAutoStart;

    @TableField(value = "first_sub_def")
    private String firstSubDef;

    @TableField(value = "status")
    @ApiModelProperty("状态 0 无效， 1有效")
    @Builder.Default
    private int status = 1;

    @TableField(value = "process_def_id")
    @ApiModelProperty("流程ID")
    private String processDefId;

    @TableField(exist = false)
    @ApiModelProperty("任务设备明细定义")
    private TaskDefinition taskDefinition;

    @TableField(exist = false)
    @ApiModelProperty("作业方向")
    private String workDirection;

    @TableField(value = "deploy_id")
    @ApiModelProperty("部署ID")
    private String deployId;

    public String getWorkDirection(){
        if(StrUtil.isNotEmpty(processData)){
            JSONObject jsonObject = JSONObject.parseObject(processData);
            return jsonObject.getString("workDirection");
        }
        return null;
    }
}
