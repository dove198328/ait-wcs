package cn.aitplus.wcs.adapters.io.modbus;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "wcs.adapter.modbus")
public class ModbusAdapterProperties {

    private boolean enabled = true;

    private long defaultRequestTimeoutMillis = 5000;

    private ModbusWordOrder defaultWordOrder = ModbusWordOrder.WORD_SWAP;

    private int maxReconnectAttempts = 8;

    private long reconnectInitialDelayMillis = 500;

    private long reconnectMaxDelayMillis = 3000;
}
