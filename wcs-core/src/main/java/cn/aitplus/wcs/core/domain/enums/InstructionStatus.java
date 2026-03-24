package cn.aitplus.wcs.core.domain.enums;

/**
 * 指令状态枚举
 */
public enum InstructionStatus {
    PENDING("pending"),
    WAITING("waiting"),
    EXECUTING("executing"),
    COMPLETED("completed"),
    FAILED("failed");
    
    private final String value;
    
    InstructionStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }

}