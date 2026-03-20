package cn.aitplus.wcs.core.domain.model;

import cn.aitplus.wcs.core.domain.enums.DomainEnums.StepStatus;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Step {

    private Long id;
    @NotBlank
    private String warehouseId;
    @NotNull
    private Long planId;
    @NotNull
    private Integer seq;
    @NotBlank
    private String type;
    @NotNull
    private StepStatus status;
    private String payloadJson;
    private String resourceLocksJson;
}

