package cn.aitplus.wcs.core.domain.model.profile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@ApiModel("责任链节点")
public class ProfileChainNode implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("仓库ID")
    private Long warehouseId;

    @ApiModelProperty("链名称：startGateChain/routingChain/planningExpandChain/commandPipelineChain/exceptionHandlingChain")
    private String chainName;

    @ApiModelProperty("节点顺序")
    private Integer nodeOrder;

    @ApiModelProperty("Spring Bean 名称")
    private String beanName;

    @ApiModelProperty("是否启用")
    private Boolean enabled;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
