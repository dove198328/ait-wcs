package cn.aitplus.wcs.execution.device.io.s7;

import cn.aitplus.wcs.core.domain.model.device.DevicePointDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S7 点位地址转换器。
 */
@Component
public class S7PointAddressConverter {

    private static final Pattern DB_BIT_PATTERN = Pattern.compile("^DB(\\d+)\\.DBX(\\d+)\\.(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DB_WORD_PATTERN = Pattern.compile("^DB(\\d+)\\.DBW(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DB_DWORD_PATTERN = Pattern.compile("^DB(\\d+)\\.DBD(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DB_BYTE_PATTERN = Pattern.compile("^DB(\\d+)\\.DBB(\\d+)$", Pattern.CASE_INSENSITIVE);

    public String convert(DevicePointDefinition pointDefinition) {
        if (pointDefinition == null || !StringUtils.hasText(pointDefinition.getAddress())) {
            throw new IllegalStateException("设备点位地址为空，无法转换 S7 地址");
        }
        String sourceAddress = pointDefinition.getAddress().trim();
        if (sourceAddress.startsWith("%")) {
            return sourceAddress;
        }
        String dataType = normalizeDataType(pointDefinition.getDataType());
        if (dataType.startsWith("STRING(")) {
            return convertStringAddress(sourceAddress, dataType);
        }
        Matcher bitMatcher = DB_BIT_PATTERN.matcher(sourceAddress);
        if (bitMatcher.matches()) {
            return String.format("%%DB%s.DBX%s.%s:BOOL",
                bitMatcher.group(1), bitMatcher.group(2), bitMatcher.group(3));
        }
        Matcher wordMatcher = DB_WORD_PATTERN.matcher(sourceAddress);
        if (wordMatcher.matches()) {
            return String.format("%%DB%s.DBW%s:%s",
                wordMatcher.group(1), wordMatcher.group(2), dataType);
        }
        Matcher dwordMatcher = DB_DWORD_PATTERN.matcher(sourceAddress);
        if (dwordMatcher.matches()) {
            return String.format("%%DB%s.DBD%s:%s",
                dwordMatcher.group(1), dwordMatcher.group(2), dataType);
        }
        Matcher byteMatcher = DB_BYTE_PATTERN.matcher(sourceAddress);
        if (byteMatcher.matches()) {
            return String.format("%%DB%s.DBB%s:%s",
                byteMatcher.group(1), byteMatcher.group(2), dataType);
        }
        if (sourceAddress.startsWith("DB") && sourceAddress.contains(":")) {
            return "%" + sourceAddress;
        }
        throw new IllegalStateException("暂不支持的 S7 点位地址格式：" + sourceAddress);
    }

    private String convertStringAddress(String sourceAddress, String dataType) {
        int stringLength = extractStringLength(dataType);
        Matcher bitMatcher = DB_BIT_PATTERN.matcher(sourceAddress);
        if (bitMatcher.matches()) {
            return String.format("%%DB%s:%s:STRING(%d)", bitMatcher.group(1), bitMatcher.group(2), stringLength);
        }
        Matcher byteMatcher = DB_BYTE_PATTERN.matcher(sourceAddress);
        if (byteMatcher.matches()) {
            return String.format("%%DB%s:%s:STRING(%d)", byteMatcher.group(1), byteMatcher.group(2), stringLength);
        }
        throw new IllegalStateException("字符串点位地址格式不合法：" + sourceAddress);
    }

    private int extractStringLength(String normalizedDataType) {
        int start = normalizedDataType.indexOf('(');
        int end = normalizedDataType.indexOf(')');
        if (start < 0 || end <= start + 1) {
            throw new IllegalStateException("字符串点位长度配置不合法：" + normalizedDataType);
        }
        return Integer.parseInt(normalizedDataType.substring(start + 1, end));
    }

    private String normalizeDataType(String dataType) {
        if (!StringUtils.hasText(dataType)) {
            return "STRING";
        }
        String upper = dataType.trim().toUpperCase(Locale.ROOT);
        if (upper.startsWith("STRING[")) {
            return "STRING(" + upper.substring(7, upper.length() - 1) + ")";
        }
        return switch (upper) {
            case "DINT", "DINT32" -> "DINT";
            case "INT16" -> "INT";
            case "BOOL", "INT", "UINT", "REAL", "LREAL", "BYTE", "WORD", "DWORD", "CHAR", "WCHAR" -> upper;
            default -> upper;
        };
    }
}
