package cn.aitplus.wcs.execution.device.io.modbus;

import cn.aitplus.wcs.core.domain.model.device.DevicePointDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Modbus canonical 地址转换器。
 */
@Component
public class ModbusPointAddressConverter {

    public String convert(DevicePointDefinition def) {
        if (def == null || !StringUtils.hasText(def.getAddress())) {
            throw new IllegalStateException("Modbus 点位 address 为空");
        }
        String raw = def.getAddress().trim();
        if (looksCanonical(raw)) {
            return appendWordOrderIfNeeded(normalizeCanonical(raw), def.getModbusWordOrder());
        }
        if (raw.matches("\\d+")) {
            int offset = Integer.parseInt(raw);
            String table = defaultTableForDataType(def.getDataType());
            if ("co".equals(table)) {
                return table + ":" + offset;
            }
            return appendWordOrderIfNeeded(
                    table + ":" + offset + ":" + mapScalarSuffix(def.getDataType()),
                    def.getModbusWordOrder()
            );
        }
        throw new IllegalStateException("Modbus 点位 address 无法解析，请使用 hr:偏移:类型 或 纯数字地址，当前=" + raw);
    }

    private static boolean looksCanonical(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        return lower.startsWith("hr:") || lower.startsWith("ir:")
                || lower.startsWith("co:") || lower.startsWith("di:");
    }

    private static String normalizeCanonical(String raw) {
        String[] parts = raw.split(":");
        if (parts.length < 2) {
            throw new IllegalStateException("Modbus canonical 地址至少需要表和偏移，当前=" + raw);
        }
        if (parts.length > 4) {
            throw new IllegalStateException("Modbus canonical 地址字段过多，当前=" + raw);
        }
        String table = parts[0].trim().toLowerCase(Locale.ROOT);
        validateTable(table, raw);
        String offset = parts[1].trim();
        if (!offset.matches("\\d+")) {
            throw new IllegalStateException("Modbus 偏移必须为非负整数，当前=" + raw);
        }
        if ("co".equals(table) || "di".equals(table)) {
            if (parts.length != 2) {
                throw new IllegalStateException("Modbus 线圈/离散输入地址不支持类型或字序后缀，当前=" + raw);
            }
            return table + ":" + offset;
        }
        if (parts.length == 2) {
            return table + ":" + offset + ":u16";
        }
        String scalar = normalizeScalar(parts[2], raw);
        if (parts.length == 3) {
            return table + ":" + offset + ":" + scalar;
        }
        if (!supportsWordOrderScalar(scalar)) {
            throw new IllegalStateException("Modbus 字序只对 i32/u32/f32 有意义，当前=" + raw);
        }
        return table + ":" + offset + ":" + scalar + ":" + normalizeWordOrder(parts[3], raw);
    }

    private static String appendWordOrderIfNeeded(String canonical, String modbusWordOrderFromDef) {
        if (!StringUtils.hasText(modbusWordOrderFromDef)) {
            return canonical;
        }
        String wo = modbusWordOrderFromDef.trim().toUpperCase(Locale.ROOT);
        if (!wo.equals("WORD_SWAP") && !wo.equals("BIG_ENDIAN")) {
            throw new IllegalStateException("modbusWordOrder 仅支持 WORD_SWAP / BIG_ENDIAN，当前=" + modbusWordOrderFromDef);
        }
        String lower = canonical.toLowerCase(Locale.ROOT);
        if (lower.endsWith(":ws") || lower.endsWith(":be")) {
            return canonical;
        }
        if (!canonical.matches("(?i).+:(i32|u32|f32)$")) {
            throw new IllegalStateException("modbusWordOrder 只能用于 32 位 Modbus 点位，当前=" + canonical);
        }
        return canonical + ":" + (wo.equals("WORD_SWAP") ? "ws" : "be");
    }

    private static String defaultTableForDataType(String dataType) {
        if (!StringUtils.hasText(dataType)) {
            return "hr";
        }
        String t = dataType.trim().toUpperCase(Locale.ROOT);
        if (t.equals("BOOL") || t.equals("BOOLEAN")) {
            return "co";
        }
        return "hr";
    }

    private static String mapScalarSuffix(String dataType) {
        if (!StringUtils.hasText(dataType)) {
            return "u16";
        }
        String t = dataType.trim().toUpperCase(Locale.ROOT);
        return switch (t) {
            case "INT", "INT16" -> "i16";
            case "UINT", "UINT16", "WORD" -> "u16";
            case "DINT", "INT32" -> "i32";
            case "UDINT", "UINT32", "DWORD" -> "u32";
            case "REAL", "FLOAT", "FLOAT32" -> "f32";
            default -> "u16";
        };
    }

    private static void validateTable(String table, String raw) {
        if (!"hr".equals(table) && !"ir".equals(table)
                && !"co".equals(table) && !"di".equals(table)) {
            throw new IllegalStateException("Modbus 表前缀只支持 hr/ir/co/di，当前=" + raw);
        }
    }

    private static String normalizeScalar(String scalar, String raw) {
        String normalized = scalar.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("i16") && !normalized.equals("u16")
                && !normalized.equals("i32") && !normalized.equals("u32")
                && !normalized.equals("f32")) {
            throw new IllegalStateException("Modbus 标量类型只支持 i16/u16/i32/u32/f32，当前=" + raw);
        }
        return normalized;
    }

    private static String normalizeWordOrder(String wordOrder, String raw) {
        String normalized = wordOrder.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("ws") && !normalized.equals("be")) {
            throw new IllegalStateException("Modbus 字序只支持 ws/be，当前=" + raw);
        }
        return normalized;
    }

    private static boolean supportsWordOrderScalar(String scalar) {
        return "i32".equals(scalar) || "u32".equals(scalar) || "f32".equals(scalar);
    }
}
