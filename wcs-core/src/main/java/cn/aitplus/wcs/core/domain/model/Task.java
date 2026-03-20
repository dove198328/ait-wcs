package cn.aitplus.wcs.core.domain.model;

import cn.aitplus.wcs.core.domain.enums.DomainEnums.TaskStatus;
import cn.aitplus.wcs.core.domain.enums.DomainEnums.TaskType;
import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Task {

    private Long id;
    @NotBlank
    private String warehouseId;
    @NotBlank
    private String upstreamTaskId;
    @NotNull
    private TaskType type;
    @NotNull
    private TaskStatus status;
    private Integer priority;
    private String payloadJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

