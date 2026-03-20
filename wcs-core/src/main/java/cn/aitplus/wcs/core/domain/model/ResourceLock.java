package cn.aitplus.wcs.core.domain.model;

import cn.aitplus.wcs.core.domain.enums.DomainEnums.OwnerType;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResourceLock {

    @NotBlank
    private String warehouseId;
    @NotBlank
    private String lockKey;
    @NotNull
    private OwnerType ownerType;
    @NotBlank
    private String ownerId;
    @NotBlank
    private String status;
    private LocalDateTime expireAt;
}

