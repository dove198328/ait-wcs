package cn.aitplus.wcs.core.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("warehouse_mode")
public class WareHouseMode implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("仓库ID")
    private Integer warehouseId;

    @ApiModelProperty("协议")
    private String protocol;

    @ApiModelProperty("地址")
    private String address;

    @ApiModelProperty("指令")
    private String cammonds;
}
