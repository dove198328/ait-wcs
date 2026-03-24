package cn.aitplus.wcs.core.domain.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 指令命令项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel("指令命令项")
public class Command implements Serializable {
    private static final long serialVersionUID = -704686100311407963L;

    @ApiModelProperty("命令名称")
    private String command;

    @ApiModelProperty("命令值")
    private Object value;

    @ApiModelProperty("是否写入命令")
    private Boolean isWrite;
}
