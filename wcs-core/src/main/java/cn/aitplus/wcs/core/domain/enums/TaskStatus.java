package cn.aitplus.wcs.core.domain.enums;

import cn.aitplus.wcs.core.domain.model.task.Task;

/**
 * 主任务在库中的 {@code tasks.status} 取值约定。
 * <p>
 * 实体 {@link Task#getStatus()} 仍为 {@link String}，
 * 业务侧比较、赋值、查询条件请统一使用 {@code TaskStatus.XXX.getValue()}，避免散落字面量。
 */
public enum TaskStatus {

    PENDING("pending"),
    EXECUTING("executing"),
    COMPLETED("completed"),
    SUSPENDED("suspended"),
    FAILED("failed"),
    CANCELED("canceled");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    /**
     * 与数据库列一致的小写字符串，用于 setStatus / Wrapper 条件等。
     */
    public String getValue() {
        return value;
    }
}
