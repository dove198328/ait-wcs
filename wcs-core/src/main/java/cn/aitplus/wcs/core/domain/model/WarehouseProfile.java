package cn.aitplus.wcs.core.domain.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel("仓库配置")
public class WarehouseProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("仓库ID")
    private Long warehouseId;

    @ApiModelProperty("仓库类型：ASRS/SHUTTLE/MIXED")
    private String warehouseType;

    @ApiModelProperty("启用的插件列表（JSON）")
    private String enabledPluginsJson;

    @ApiModelProperty("参数配置（JSON）")
    private String paramsJson;

    @ApiModelProperty("版本号")
    private Integer version;

    @ApiModelProperty("是否激活")
    private Boolean active;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
