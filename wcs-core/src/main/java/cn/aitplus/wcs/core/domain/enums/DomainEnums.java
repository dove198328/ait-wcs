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

    public enum TaskStatus {
        CREATED,
        READY,
        RUNNING,
        SUSPENDED,
        FAILED,
        COMPLETED,
        CANCELED
    }

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
