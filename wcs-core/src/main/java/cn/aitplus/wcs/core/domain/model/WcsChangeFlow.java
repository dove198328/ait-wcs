package cn.aitplus.wcs.core.domain.model;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Api("更改仓库运行模式")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class WcsChangeFlow {

    @ApiModelProperty("仓库ID")
    private Integer warehouseId;

    @ApiModelProperty("模式")
    private Integer mode;
}