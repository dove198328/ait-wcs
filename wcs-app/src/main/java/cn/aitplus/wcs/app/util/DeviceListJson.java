package cn.aitplus.wcs.app.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses WMS device list JSON: root array, or object with {@code data} / {@code list} array.
 */
public final class DeviceListJson {

    private DeviceListJson() {
    }

    public static List<JsonNode> parseDeviceNodes(String body, ObjectMapper mapper) throws JsonProcessingException {
        if (body == null || body.trim().isEmpty()) {
            return Collections.emptyList();
        }
        JsonNode root = mapper.readTree(body);
        if (root.isArray()) {
            return toList(root);
        }
        if (root.isObject()) {
            JsonNode data = root.get("data");
            if (data != null && data.isArray()) {
                return toList(data);
            }
            JsonNode list = root.get("list");
            if (list != null && list.isArray()) {
                return toList(list);
            }
        }
        throw new IllegalArgumentException("WMS 设备列表 JSON 结构不支持（需为数组或含 data/list 数组的对象）");
    }

    private static List<JsonNode> toList(JsonNode arrayNode) {
        List<JsonNode> out = new ArrayList<>(arrayNode.size());
        for (JsonNode n : arrayNode) {
            out.add(n);
        }
        return out;
    }

    /**
     * Resolves stable device id from a device object node.
     */
    public static String resolveDeviceId(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        if (node.hasNonNull("deviceId")) {
            return textOrNumber(node.get("deviceId"));
        }
        if (node.hasNonNull("device_id")) {
            return textOrNumber(node.get("device_id"));
        }
        if (node.has("id") && !node.get("id").isNull()) {
            return textOrNumber(node.get("id"));
        }
        return null;
    }

    private static String textOrNumber(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isTextual()) {
            return n.asText();
        }
        if (n.isNumber()) {
            return n.asText();
        }
        return n.asText(null);
    }
}
