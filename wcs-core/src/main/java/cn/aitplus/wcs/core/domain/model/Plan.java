package cn.aitplus.wcs.core.domain.model;

import cn.aitplus.wcs.core.domain.enums.DomainEnums.PlanStatus;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Plan {

    private Long id;
    @NotBlank
    private String warehouseId;
    @NotNull
    private Long taskId;
    @NotNull
    private PlanStatus status;
}

