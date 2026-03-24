package cn.aitplus.wcs.app.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * MQTT：Broker 与出站共用；入站分 WMS（B）与设备（A）两路通道。
 * <p>
 * 总开关：{@code mqtt.enabled=false} 关闭本配置。
 */
@Configuration
@EnableIntegration
@IntegrationComponentScan(basePackages = "cn.aitplus.wcs.app.integration.mqtt")
@EnableConfigurationProperties(MqttProperties.class)
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqttConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttConfig.class);

    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttProperties props) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(Arrays.stream(props.getBroker().split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toArray(String[]::new));
        if (StringUtils.hasText(props.getUsername())) {
            options.setUserName(props.getUsername());
            if (StringUtils.hasText(props.getPassword())) {
                options.setPassword(props.getPassword().toCharArray());
            }
        }
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutboundHandler(
        MqttProperties props,
        MqttPahoClientFactory mqttClientFactory,
        @Value("${mqtt.default.topic:}") String legacyDefaultTopic) {
        String defaultTopic = props.getPublishDefaultTopic();
        if (!StringUtils.hasText(defaultTopic)) {
            defaultTopic = firstCsvSegment(legacyDefaultTopic);
        }
        if (!StringUtils.hasText(defaultTopic)) {
            defaultTopic = "wcs/command/default";
        }
        MqttPahoMessageHandler handler =
            new MqttPahoMessageHandler(props.getClientId() + "-out", mqttClientFactory);
        handler.setDefaultTopic(defaultTopic);
        handler.setAsync(false);
        handler.setDefaultQos(1);
        return handler;
    }

    private static String firstCsvSegment(String csv) {
        if (!StringUtils.hasText(csv)) {
            return "";
        }
        String[] parts = csv.split(",");
        return parts[0].trim();
    }

    @Bean
    public MessageChannel mqttWmsInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttDeviceInboundChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttWmsInboundChannel")
    public MessageHandler wmsMqttInboundLogger() {
        return message -> log.info("[MQTT WMS] topic={} payload={}",
            message.getHeaders().get(org.springframework.integration.mqtt.support.MqttHeaders.RECEIVED_TOPIC),
            message.getPayload());
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttDeviceInboundChannel")
    public MessageHandler deviceMqttInboundLogger() {
        return message -> log.info("[MQTT device] topic={} payload={}",
            message.getHeaders().get(org.springframework.integration.mqtt.support.MqttHeaders.RECEIVED_TOPIC),
            message.getPayload());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "mqtt", name = "wms-inbound-enabled", havingValue = "true")
    static class WmsMqttInboundConfig {

        @Bean
        public MessageProducer wmsMqttInboundAdapter(
            MqttProperties props,
            MqttPahoClientFactory mqttClientFactory,
            MessageChannel mqttWmsInboundChannel) {
            if (!StringUtils.hasText(props.getWmsInboundTopics())) {
                throw new IllegalStateException(
                    "mqtt.wms-inbound-enabled=true 时必须配置 mqtt.wms-inbound-topics（逗号分隔）");
            }
            return buildInboundAdapter(
                props.getClientId() + "-wms-in",
                mqttClientFactory,
                props.getWmsInboundTopics(),
                props.getCompletionTimeout(),
                mqttWmsInboundChannel);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "mqtt", name = "device-inbound-enabled", havingValue = "true", matchIfMissing = true)
    static class DeviceMqttInboundConfig {

        @Bean
        public MessageProducer deviceMqttInboundAdapter(
            MqttProperties props,
            MqttPahoClientFactory mqttClientFactory,
            MessageChannel mqttDeviceInboundChannel,
            @Value("${mqtt.default.topic:}") String legacyDefaultTopic) {
            String topics = resolveDeviceInboundTopics(props, legacyDefaultTopic);
            return buildInboundAdapter(
                props.getClientId() + "-device-in",
                mqttClientFactory,
                topics,
                props.getCompletionTimeout(),
                mqttDeviceInboundChannel);
        }

        private static String resolveDeviceInboundTopics(MqttProperties props, String legacyDefaultTopic) {
            if (StringUtils.hasText(props.getDeviceInboundTopics())) {
                return props.getDeviceInboundTopics();
            }
            if (StringUtils.hasText(legacyDefaultTopic)) {
                return legacyDefaultTopic;
            }
            return props.getPublishDefaultTopic();
        }
    }

    private static MessageProducer buildInboundAdapter(
        String clientId,
        MqttPahoClientFactory factory,
        String topicsCsv,
        int completionTimeout,
        MessageChannel output) {
        String[] topics = Arrays.stream(topicsCsv.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toArray(String[]::new);
        if (topics.length == 0) {
            throw new IllegalStateException("MQTT 入站 topic 解析后为空");
        }
        MqttPahoMessageDrivenChannelAdapter adapter =
            new MqttPahoMessageDrivenChannelAdapter(clientId, factory, topics);
        adapter.setCompletionTimeout(completionTimeout);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(output);
        return adapter;
    }
}
