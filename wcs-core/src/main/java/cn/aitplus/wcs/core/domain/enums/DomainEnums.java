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
        HTTP,
        /** OPC UA 等长会话 + 订阅类接入 */
        OPC
    }

    public enum OwnerType {
        PLAN,
        STEP
    }

    public enum DepthType {
        SINGLE,
        FRONT,
        BACK;

        public static DepthType parseFromLocation(String location) {
            if (location == null || location.length() < 2) {
                return SINGLE;
            }
            String suffix = location.substring(location.length() - 2).toUpperCase();
            switch (suffix) {
                case "-F":
                    return FRONT;
                case "-B":
                    return BACK;
                default:
                    return SINGLE;
            }
        }
    }
}
