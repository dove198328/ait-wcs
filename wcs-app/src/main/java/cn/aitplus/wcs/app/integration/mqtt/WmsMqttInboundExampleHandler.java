package cn.aitplus.wcs.app.integration.mqtt;

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
 * WMS 侧 MQTT 入站（B）<strong>示例</strong>，归属 {@code wcs-app}（上游入口与 REST 同级）。
 * <p>
 * 订阅通道 {@code mqttWmsInboundChannel}，由 {@link cn.aitplus.wcs.app.config.MqttConfig} 在
 * {@code mqtt.wms-inbound-enabled=true} 且配置了 {@code mqtt.wms-inbound-topics} 时接入 Broker。
 * <p>
 * 示例载荷（可与 REST 批量任务对齐，字段以实际契约为准）：
 * <pre>
 * {"warehouseId":1,"tasks":[{"workflowDefId":"x","taskName":"demo"}]}
 * </pre>
 * 生产请改为调用 {@code TasksService} 等与 REST 相同的应用服务，并在文档中固定 topic 与 JSON schema。
 */
@Component
@Conditional(WmsMqttInboundExampleEnabledCondition.class)
public class WmsMqttInboundExampleHandler {

    private static final Logger log = LoggerFactory.getLogger(WmsMqttInboundExampleHandler.class);

    private final ObjectMapper objectMapper;

    public WmsMqttInboundExampleHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ServiceActivator(inputChannel = "mqttWmsInboundChannel")
    public void onMessage(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String body = toUtf8String(message.getPayload());

        log.info("[MQTT WMS 示例] topic={} payload={}", topic, abbrev(body));

        if (!StringUtils.hasText(body)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!root.hasNonNull("warehouseId")) {
                log.warn("[MQTT WMS 示例] 缺少 warehouseId，跳过解析示例");
                return;
            }
            long warehouseId = root.get("warehouseId").asLong();
            log.info("[MQTT WMS 示例] warehouseId={}", warehouseId);
            if (root.has("tasks") && root.get("tasks").isArray()) {
                int n = root.get("tasks").size();
                log.info("[MQTT WMS 示例] tasks 数组长度={}（此处仅演示，未调用 TasksService）", n);
            }
            // 示例扩展点（与 TasksController.insertBatch 对齐）：
            // tasks.forEach(t -> t.setWarehouseId(warehouseId));
            // tasksService.insertBatch(tasks);
        } catch (Exception e) {
            log.debug("[MQTT WMS 示例] 非 JSON 或解析失败: {}", e.toString());
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
