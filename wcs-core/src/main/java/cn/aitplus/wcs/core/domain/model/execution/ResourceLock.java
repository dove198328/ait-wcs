package cn.aitplus.wcs.core.domain.model.execution;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel("资源锁")
public class ResourceLock implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("仓库ID")
    private Long warehouseId;

    @ApiModelProperty("资源锁键，例如 AISLE:3/STATION:IN-01/LOCATION:A-01-01-02-F")
    private String lockKey;

    @ApiModelProperty("持有者类型：TASK/SUBTASK/INSTRUCTION")
    private String ownerType;

    @ApiModelProperty("持有者ID")
    private String ownerId;

    @ApiModelProperty("锁状态：HELD/RELEASED/EXPIRED")
    private String status;

    @ApiModelProperty("过期时间")
    private LocalDateTime expireAt;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
