package cn.aitplus.wcs.adapters.io.modbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "wcs.adapter.modbus", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ModbusAdapterProperties.class)
public class ModbusAdapterConfiguration {

    @Bean(destroyMethod = "destroy")
    public ModbusTcpDeviceTransport modbusTcpDeviceTransport(ModbusAdapterProperties properties,
                                                             ObjectMapper objectMapper) {
        return new ModbusTcpDeviceTransport(properties, objectMapper);
    }
}
