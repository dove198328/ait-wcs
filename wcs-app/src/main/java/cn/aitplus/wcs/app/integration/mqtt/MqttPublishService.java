package cn.aitplus.wcs.app.integration.mqtt;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;

/**
 * MQTT 出站网关：WMS（B）与设备（A）共用同一 Broker 时，通过 {@link MqttHeaders#TOPIC} 区分目标主题即可。
 * <p>
 * 入站分别走 {@code mqttWmsInboundChannel}、{@code mqttDeviceInboundChannel}（见 {@link cn.aitplus.wcs.app.config.MqttConfig}）。
 */
@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
public interface MqttPublishService {

    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, String data);

    /**
     * 使用出站默认主题（{@code mqtt.publish-default-topic}，或对 {@code mqtt.default.topic} 的兼容回落）。
     */
    void sendToDefaultTopic(String data);
}
