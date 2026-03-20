package cn.aitplus.wcs.core.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskDefinition implements Serializable {
    private static final long serialVersionUID = 1L;
    private String workflowDefId;
    private Integer warehouseId;
    private List<SubtaskDefinition> subtasks;
    private String workDirection;
}
