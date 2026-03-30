package cn.aitplus.wcs.adapters.io.s7;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "wcs.adapter.s7", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(S7AdapterProperties.class)
public class S7AdapterConfiguration {

    @Bean(destroyMethod = "destroy")
    public S7Plc4xDeviceTransport s7Plc4xDeviceTransport(S7AdapterProperties properties, ObjectMapper objectMapper) {
        return new S7Plc4xDeviceTransport(properties, objectMapper);
    }
}
