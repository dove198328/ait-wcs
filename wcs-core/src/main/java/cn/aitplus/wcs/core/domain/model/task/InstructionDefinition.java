package cn.aitplus.wcs.core.domain.model.task;

import cn.aitplus.wcs.core.domain.model.execution.Command;
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
public class InstructionDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private String instructionDefId;

    private String subtaskDefId;

    private int sequence;

    private String protocol;

    private List<Command> commands;

    //可执行设备列表
    private String devices;

    private String params;

    private Boolean messageEvent;

    //消息事件逻辑关系（and：与，or：或，not：非）
    private String eventLogic;
}
