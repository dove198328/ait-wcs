package cn.aitplus.wcs.core.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProtocolType {
    MQTT("mqtt", false, true),
    TCP("tcp", false, true),
    HTTP("http", false,  true),
    PLC4X_S7("plc4x-s7", true, false),
    PLC4X_MODBUS_TCP("plc4x-modbus-tcp", false, false),
    PLC4X_MODBUS_RTU("plc4x-modbus-rtu", false, true),
    PLC4X_MODBUS_ASCII("plc4x-modbus-ascii", false, true),
    JMS("jms", false, true),
    PLC4X("plc4x",true, false),
    OPCUA("opcua", false, true),
    HK("hk", false, true);



    private final String protocol;
    private final boolean isHeartbeat;
    private final boolean canSubscribe;

    ProtocolType(String protocol, boolean isHeartbeat, boolean canSubscribe) {
        this.protocol = protocol;
        this.isHeartbeat = isHeartbeat;
        this.canSubscribe = canSubscribe;
    }

    @JsonValue
    public String getProtocol() {
        return protocol;
    }
    
    public boolean isHeartbeat() {
        return isHeartbeat;
    }

    public boolean canSubscribe() {
        return canSubscribe;
    }

    @Override
    public String toString() {
        return protocol;
    }
    
    @JsonCreator
    public static ProtocolType fromString(String value) {
        if (value == null) {
            return null;
        }
        
        for (ProtocolType type : ProtocolType.values()) {
            if (type.protocol.equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        // 尝试转换特殊格式（例如将"-"替换为"_"）
        String formattedValue = value.toUpperCase().replace('-', '_');
        try {
            return ProtocolType.valueOf(formattedValue);
        } catch (IllegalArgumentException e) {
            // 如果无法匹配，返回默认值或抛出异常
            return null;
        }
    }
} 