package cn.aitplus.wcs.core.domain.enums;

public final class DomainEnums {

    private DomainEnums() {
    }

    public enum TaskType {
        OUTBOUND,
        INBOUND,
        INVENTORY,
        RELOCATE
    }

    // 主任务持久化状态见 cn.aitplus.wcs.core.domain.enums.TaskStatus（实体仍为 String，用 getValue() 与库对齐）

    public enum PlanStatus {
        CREATED,
        RUNNING,
        COMPLETED,
        FAILED
    }

    public enum StepStatus {
        PENDING,
        RUNNING,
        DONE,
        FAILED,
        SKIPPED
    }

    public enum CommandStatus {
        SENT,
        ACK,
        RUNNING,
        DONE,
        ERROR,
        TIMEOUT,
        CANCELED
    }

    public enum CommandDomain {
        S7,
        MODBUS,
        RCS
    }

    public enum OwnerType {
        PLAN,
        STEP
    }
}
