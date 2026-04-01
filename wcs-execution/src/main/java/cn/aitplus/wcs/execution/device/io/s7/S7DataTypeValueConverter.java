package cn.aitplus.wcs.execution.device.io.s7;

import cn.aitplus.wcs.core.domain.model.device.DevicePointDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * S7 点位值转 Java 类型转换器。
 */
@Component
public class S7DataTypeValueConverter {

    public Object convert(DevicePointDefinition pointDefinition, Object rawValue) {
        if (rawValue == null || pointDefinition == null || !StringUtils.hasText(pointDefinition.getDataType())) {
            return rawValue;
        }
        String dataType = normalizeDataType(pointDefinition.getDataType());
        return switch (dataType) {
            case "BOOL" -> toBoolean(rawValue);
            case "INT", "UINT", "WORD", "BYTE" -> toInteger(rawValue);
            case "DINT", "DWORD" -> toLong(rawValue);
            case "REAL", "LREAL" -> toBigDecimal(rawValue);
            default -> String.valueOf(rawValue);
        };
    }

    private String normalizeDataType(String dataType) {
        String upper = dataType.trim().toUpperCase(Locale.ROOT);
        if (upper.startsWith("STRING[")) {
            return "STRING";
        }
        return switch (upper) {
            case "DINT32" -> "DINT";
            case "INT16" -> "INT";
            default -> upper;
        };
    }

    private Boolean toBoolean(Object rawValue) {
        if (rawValue instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (rawValue instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = String.valueOf(rawValue).trim();
        return "1".equals(text) || Boolean.parseBoolean(text);
    }

    private Integer toInteger(Object rawValue) {
        if (rawValue instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(String.valueOf(rawValue).trim());
    }

    private Long toLong(Object rawValue) {
        if (rawValue instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(rawValue).trim());
    }

    private BigDecimal toBigDecimal(Object rawValue) {
        if (rawValue instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (rawValue instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        return new BigDecimal(String.valueOf(rawValue).trim());
    }
}
