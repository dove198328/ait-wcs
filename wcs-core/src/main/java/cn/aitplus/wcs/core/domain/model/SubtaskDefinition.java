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
public class SubtaskDefinition implements Serializable {
    private static final long serialVersionUID = 1L;
    private String subtaskDefId;
    private String workflowDefId;
    private String name;
    private String area;
    private int priority;
    private String dependson;
    private Boolean isStartNextProcess;
    private String freeDeviceId;
    private String freeAreaId;
    private Boolean checkAisle;
    private String bindDDJ;
    List<InstructionDefinition> instructions;
}
