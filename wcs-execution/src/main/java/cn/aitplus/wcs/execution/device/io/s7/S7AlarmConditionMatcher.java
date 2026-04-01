package cn.aitplus.wcs.execution.device.io.s7;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * S7 报警条件匹配器。
 * <p>
 * 当前支持以下写法：
 * <p>
 * 1. 固定值列表：{@code 1000,1001,1002}
 * <p>
 * 2. 闭区间：{@code range:[1000,2000]}
 * <p>
 * 3. 区间加排除：{@code range:[1000,2000];exclude:1041,1087}
 * <p>
 * 4. 比较条件：{@code >=1000;<=2000;exclude:1041,1087}
 * <p>
 * 5. 多段规则（或关系）：{@code range:[1000,2000];exclude:1041|3000,3001}
 */
@Component
public class S7AlarmConditionMatcher {

    public boolean matches(Object javaValue, String alarmCondition) {
        if (javaValue == null || !StringUtils.hasText(alarmCondition)) {
            return false;
        }
        for (String segment : splitByPipe(alarmCondition)) {
            if (matchSingleSegment(javaValue, segment)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchSingleSegment(Object javaValue, String segmentText) {
        String segment = normalizeText(segmentText);
        if (!StringUtils.hasText(segment)) {
            return false;
        }
        if (!containsStructuredRule(segment)) {
            return matchValueList(javaValue, splitValueList(segment));
        }

        SegmentRule segmentRule = parseSegmentRule(segment);
        if (!segmentRule.requiredValues.isEmpty() && !matchValueList(javaValue, segmentRule.requiredValues)) {
            return false;
        }
        if (segmentRule.minValue != null) {
            int compared = compareNumeric(javaValue, segmentRule.minValue);
            if (compared < 0 || (segmentRule.minExclusive && compared == 0)) {
                return false;
            }
        }
        if (segmentRule.maxValue != null) {
            int compared = compareNumeric(javaValue, segmentRule.maxValue);
            if (compared > 0 || (segmentRule.maxExclusive && compared == 0)) {
                return false;
            }
        }
        return !matchValueList(javaValue, segmentRule.excludeValues);
    }

    private SegmentRule parseSegmentRule(String segment) {
        SegmentRule segmentRule = new SegmentRule();
        for (String rawToken : splitBySemicolon(segment)) {
            String token = normalizeText(rawToken);
            if (!StringUtils.hasText(token)) {
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            if (lower.startsWith("range:")) {
                parseRangeRule(segmentRule, token.substring(token.indexOf(':') + 1).trim());
                continue;
            }
            if (lower.startsWith("exclude:")) {
                segmentRule.excludeValues.addAll(splitValueList(token.substring(token.indexOf(':') + 1)));
                continue;
            }
            if (isCompareToken(token)) {
                parseCompareRule(segmentRule, token);
                continue;
            }
            segmentRule.requiredValues.addAll(splitValueList(token));
        }
        return segmentRule;
    }

    private void parseRangeRule(SegmentRule segmentRule, String rangeText) {
        String normalized = normalizeText(rangeText);
        if (!normalized.startsWith("[") || !normalized.endsWith("]")) {
            throw new IllegalStateException(
                "\u62A5\u8B66\u533A\u95F4\u683C\u5F0F\u4E0D\u5408\u6CD5,\u5E94\u4E3A range:[\u6700\u5C0F\u503C,\u6700\u5927\u503C],\u5F53\u524D\u503C:" + rangeText
            );
        }
        String content = normalized.substring(1, normalized.length() - 1);
        List<String> values = splitValueList(content);
        if (values.size() != 2) {
            throw new IllegalStateException(
                "\u62A5\u8B66\u533A\u95F4\u683C\u5F0F\u4E0D\u5408\u6CD5,\u5E94\u540C\u65F6\u63D0\u4F9B\u6700\u5C0F\u503C\u548C\u6700\u5927\u503C,\u5F53\u524D\u503C:" + rangeText
            );
        }
        segmentRule.minValue = toBigDecimal(values.get(0));
        segmentRule.maxValue = toBigDecimal(values.get(1));
    }

    private void parseCompareRule(SegmentRule segmentRule, String token) {
        if (token.startsWith(">=")) {
            segmentRule.minValue = max(segmentRule.minValue, toBigDecimal(token.substring(2)));
            segmentRule.minExclusive = false;
            return;
        }
        if (token.startsWith(">")) {
            BigDecimal value = toBigDecimal(token.substring(1));
            segmentRule.minValue = max(segmentRule.minValue, value);
            segmentRule.minExclusive = true;
            return;
        }
        if (token.startsWith("<=")) {
            segmentRule.maxValue = min(segmentRule.maxValue, toBigDecimal(token.substring(2)));
            segmentRule.maxExclusive = false;
            return;
        }
        if (token.startsWith("<")) {
            BigDecimal value = toBigDecimal(token.substring(1));
            segmentRule.maxValue = min(segmentRule.maxValue, value);
            segmentRule.maxExclusive = true;
            return;
        }
        if (token.startsWith("==")) {
            segmentRule.requiredValues.add(token.substring(2).trim());
            return;
        }
        throw new IllegalStateException("\u4E0D\u652F\u6301\u7684\u62A5\u8B66\u6BD4\u8F83\u89C4\u5219:" + token);
    }

    private boolean matchValueList(Object javaValue, List<String> values) {
        for (String candidate : values) {
            if (isSameValue(javaValue, candidate)) {
                return true;
            }
        }
        return false;
    }

    private int compareNumeric(Object javaValue, BigDecimal right) {
        BigDecimal left = toBigDecimal(String.valueOf(javaValue));
        return left.compareTo(right);
    }

    private boolean isSameValue(Object javaValue, String candidate) {
        if (!StringUtils.hasText(candidate) || javaValue == null) {
            return false;
        }
        if (javaValue instanceof Number) {
            try {
                return compareNumeric(javaValue, toBigDecimal(candidate)) == 0;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        if (javaValue instanceof Boolean booleanValue) {
            return ("1".equals(candidate) && booleanValue)
                || ("0".equals(candidate) && !booleanValue)
                || String.valueOf(booleanValue).equalsIgnoreCase(candidate);
        }
        return String.valueOf(javaValue).equals(candidate);
    }

    private boolean containsStructuredRule(String text) {
        String normalized = normalizeText(text).toLowerCase(Locale.ROOT);
        return normalized.contains("range:")
            || normalized.contains("exclude:")
            || normalized.contains(">=")
            || normalized.contains("<=")
            || normalized.contains(">")
            || normalized.contains("<")
            || normalized.contains("==")
            || normalized.contains(";");
    }

    private boolean isCompareToken(String token) {
        return token.startsWith(">=")
            || token.startsWith("<=")
            || token.startsWith(">")
            || token.startsWith("<")
            || token.startsWith("==");
    }

    private List<String> splitByPipe(String text) {
        return trimToList(normalizeText(text).split("\\|"));
    }

    private List<String> splitBySemicolon(String text) {
        return trimToList(normalizeText(text).split(";"));
    }

    private List<String> splitValueList(String text) {
        return trimToList(normalizeText(text).split(","));
    }

    private List<String> trimToList(String[] segments) {
        List<String> values = new ArrayList<>();
        for (String segment : segments) {
            String value = segment == null ? "" : segment.trim();
            if (StringUtils.hasText(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace('\uFF0C', ',')
            .replace('\uFF1B', ';')
            .replace('\uFF5C', '|')
            .replace('\u3010', '[')
            .replace('\u3011', ']')
            .trim();
    }

    private BigDecimal toBigDecimal(String text) {
        return new BigDecimal(text.trim());
    }

    private BigDecimal max(BigDecimal left, BigDecimal right) {
        if (left == null) {
            return right;
        }
        return left.compareTo(right) >= 0 ? left : right;
    }

    private BigDecimal min(BigDecimal left, BigDecimal right) {
        if (left == null) {
            return right;
        }
        return left.compareTo(right) <= 0 ? left : right;
    }

    private static final class SegmentRule {
        private final List<String> requiredValues = new ArrayList<>();
        private final List<String> excludeValues = new ArrayList<>();
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private boolean minExclusive;
        private boolean maxExclusive;
    }
}
