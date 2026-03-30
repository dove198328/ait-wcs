package cn.aitplus.wcs.adapters.io.opcua.subscription;

import org.springframework.context.ApplicationEvent;

/**
 * OPC UA MonitoredItem 数据通知：每条回调发布一条事件（单 NodeId + 值）。
 * 多值时间窗合并等业务逻辑由上层（如 wcs-execution）自行实现。
 */
public class OpcUaSubscriptionNotificationEvent extends ApplicationEvent {

    private final Long warehouseId;
    private final String deviceId;
    /** 与 register 时传入的 NodeId 字符串一致 */
    private final String nodeId;
    private final Object value;

    public OpcUaSubscriptionNotificationEvent(Object source, Long warehouseId, String deviceId, String nodeId, Object value) {
        super(source);
        this.warehouseId = warehouseId;
        this.deviceId = deviceId;
        this.nodeId = nodeId;
        this.value = value;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public Object getValue() {
        return value;
    }
}
