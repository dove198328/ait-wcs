package cn.aitplus.wcs.adapters.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * 设备侧 MQTT 入站（A）<strong>示例</strong>，归属 {@code wcs-adapters}。
 * <p>
 * 入站通道 {@code mqttDeviceInboundChannel} 由 {@code wcs-app} 的 {@link cn.aitplus.wcs.app.config.MqttConfig} 声明；
 * 运行时由 Spring 按名称装配，本类与 {@code MqttConfig} 同属一个 ApplicationContext 即可（当前由 {@code WcsApplication} 扫描 {@code cn.aitplus.wcs} 子包）。
 * <p>
 * 生产请改写为真实协议解析并对接 execution/领域服务，载荷格式需在文档中约定。
 */
@Component
@Conditional(MqttIntegrationEnabledCondition.class)
public class DeviceMqttInboundExampleHandler {

    private static final Logger log = LoggerFactory.getLogger(DeviceMqttInboundExampleHandler.class);

    private final ObjectMapper objectMapper;

    public DeviceMqttInboundExampleHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ServiceActivator(inputChannel = "mqttDeviceInboundChannel")
    public void onMessage(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String body = toUtf8String(message.getPayload());

        log.info("[MQTT device 示例/adapters] topic={} payload={}", topic, abbrev(body));

        if (!StringUtils.hasText(body)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.hasNonNull("warehouseId")) {
                log.info("[MQTT device 示例/adapters] warehouseId={}", root.get("warehouseId").asLong());
            }
            if (root.has("status")) {
                log.info("[MQTT device 示例/adapters] status={}", root.get("status").asText());
            }
        } catch (Exception e) {
            log.debug("[MQTT device 示例/adapters] 非 JSON 或解析失败: {}", e.toString());
        }
    }

    private static String toUtf8String(Object payload) {
        if (payload instanceof byte[]) {
            return new String((byte[]) payload, StandardCharsets.UTF_8);
        }
        if (payload instanceof String) {
            return (String) payload;
        }
        return payload == null ? "" : String.valueOf(payload);
    }

    private static String abbrev(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 400 ? s.substring(0, 400) + "..." : s;
    }
}
