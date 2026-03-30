package cn.aitplus.wcs.adapters.io.opcua.config;

import cn.aitplus.wcs.adapters.io.opcua.session.OpcUaClientRegistry;
import cn.aitplus.wcs.adapters.io.opcua.subscription.OpcUaSubscriptionService;
import cn.aitplus.wcs.adapters.io.opcua.transport.OpcUaDeviceTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ConditionalOnProperty(prefix = "wcs.adapter.opcua", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OpcUaAdapterProperties.class)
public class OpcUaAdapterConfiguration {

    @Bean(name = "opcUaTaskScheduler")
    public ThreadPoolTaskScheduler opcUaTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("opc-ua-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }

    @Bean(destroyMethod = "destroy")
    public OpcUaClientRegistry opcUaClientRegistry(OpcUaAdapterProperties properties) {
        return new OpcUaClientRegistry(properties);
    }

    @Bean(destroyMethod = "destroy")
    public OpcUaSubscriptionService opcUaSubscriptionService(
        OpcUaAdapterProperties properties,
        OpcUaClientRegistry opcUaClientRegistry,
        ApplicationEventPublisher applicationEventPublisher,
        @Qualifier("opcUaTaskScheduler") TaskScheduler opcUaTaskScheduler) {
        OpcUaSubscriptionService service = new OpcUaSubscriptionService(
            properties, opcUaClientRegistry, applicationEventPublisher, opcUaTaskScheduler);
        opcUaClientRegistry.addListener(service);
        return service;
    }

    @Bean
    public OpcUaDeviceTransport opcUaDeviceTransport(
        OpcUaClientRegistry opcUaClientRegistry,
        ObjectMapper objectMapper,
        OpcUaAdapterProperties properties) {
        return new OpcUaDeviceTransport(opcUaClientRegistry, objectMapper, properties);
    }
}
