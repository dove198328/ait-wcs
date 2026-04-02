package cn.aitplus.wcs.execution.device.io.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一解析设备读取结果中的 reads 节点，屏蔽 object / array 两种返回结构差异。
 */
@Component
public class DeviceIoReadResultParser {

    private final ObjectMapper objectMapper;

    public DeviceIoReadResultParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> parseReadValues(String responseJson) {
        Map<String, JsonNode> valueByAddress = parseReadValueNodes(responseJson);
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : valueByAddress.entrySet()) {
            values.put(entry.getKey(), convertJsonNode(entry.getValue()));
        }
        return values;
    }

    public Map<String, JsonNode> parseReadValueNodes(String responseJson) {
        if (!StringUtils.hasText(responseJson)) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode readsNode = root.path("reads");
            if (readsNode.isMissingNode() || readsNode.isNull()) {
                return Map.of();
            }
            Map<String, JsonNode> valueByAddress = new LinkedHashMap<>();
            if (readsNode.isObject()) {
                readsNode.fields().forEachRemaining(entry -> valueByAddress.put(entry.getKey(), entry.getValue()));
                return valueByAddress;
            }
            if (readsNode.isArray()) {
                for (JsonNode item : readsNode) {
                    String address = item.path("address").asText("");
                    if (StringUtils.hasText(address)) {
                        valueByAddress.put(address, item.get("value"));
                    }
                }
            }
            return valueByAddress;
        } catch (IOException ex) {
            throw new IllegalStateException("解析设备读取结果失败", ex);
        }
    }

    private Object convertJsonNode(JsonNode valueNode) {
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        if (valueNode.isIntegralNumber()) {
            return valueNode.longValue();
        }
        if (valueNode.isFloatingPointNumber()) {
            return valueNode.decimalValue();
        }
        if (valueNode.isBoolean()) {
            return valueNode.booleanValue();
        }
        if (valueNode.isTextual()) {
            return valueNode.textValue();
        }
        if (valueNode.isObject() || valueNode.isArray()) {
            return valueNode.toString();
        }
        return valueNode.asText();
    }
}
