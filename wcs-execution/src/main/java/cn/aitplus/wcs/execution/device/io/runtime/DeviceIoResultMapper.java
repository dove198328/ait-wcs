package cn.aitplus.wcs.execution.device.io.runtime;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.domain.model.device.DevicePointDefinition;
import cn.aitplus.wcs.core.domain.model.device.DevicePointReadResult;
import cn.aitplus.wcs.core.domain.model.device.DevicePointValue;
import cn.aitplus.wcs.core.domain.model.device.DevicePointWriteResult;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.execution.device.io.runtime.model.DeviceIoPlan;
import cn.aitplus.wcs.execution.device.io.runtime.model.DeviceRuntimeProfile;
import cn.aitplus.wcs.execution.device.io.runtime.model.ResolvedDevicePoint;
import cn.aitplus.wcs.execution.device.io.s7.S7AlarmConditionMatcher;
import cn.aitplus.wcs.execution.device.io.s7.S7DataTypeValueConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备 IO 结果映射器。
 */
@Component
public class DeviceIoResultMapper {

    private final ObjectMapper objectMapper;
    private final S7DataTypeValueConverter s7DataTypeValueConverter;
    private final DeviceStatusEnumResolver deviceStatusEnumResolver;
    private final S7AlarmConditionMatcher s7AlarmConditionMatcher;

    public DeviceIoResultMapper(ObjectMapper objectMapper,
                                S7DataTypeValueConverter s7DataTypeValueConverter,
                                DeviceStatusEnumResolver deviceStatusEnumResolver,
                                S7AlarmConditionMatcher s7AlarmConditionMatcher) {
        this.objectMapper = objectMapper;
        this.s7DataTypeValueConverter = s7DataTypeValueConverter;
        this.deviceStatusEnumResolver = deviceStatusEnumResolver;
        this.s7AlarmConditionMatcher = s7AlarmConditionMatcher;
    }

    public DevicePointReadResult toReadResult(DeviceIoPlan ioPlan, DeviceIoResult ioResult) {
        DeviceRuntimeProfile profile = ioPlan.getRuntimeProfile();
        DevicePointReadResult.DevicePointReadResultBuilder builder = DevicePointReadResult.builder()
            .success(ioResult != null && ioResult.isSuccess())
            .errorCode(ioResult != null ? ioResult.getErrorCode() : "UNKNOWN")
            .errorMessage(ioResult != null ? ioResult.getErrorMessage() : "设备读取结果为空")
            .deviceId(profile.getDeviceConfig().getDeviceId())
            .deviceName(profile.getDeviceConfig().getDeviceName())
            .protocolType(profile.getDeviceConfig().getProtocolType())
            .rawResponseJson(ioResult != null ? ioResult.getResponseJson() : null);
        if (ioResult == null || !ioResult.isSuccess()) {
            return builder.items(new ArrayList<>()).build();
        }
        Map<String, JsonNode> valueByAddress = extractReadValueMap(ioResult.getResponseJson());
        List<DevicePointValue> items = new ArrayList<>();
        for (ResolvedDevicePoint resolvedPoint : ioPlan.getResolvedPoints()) {
            DevicePointDefinition pointDefinition = resolvedPoint.getPointDefinition();
            JsonNode valueNode = valueByAddress.get(resolvedPoint.getAdapterAddress());
            Object rawValue = convertJsonNode(valueNode);
            Object javaValue = convertJavaValue(profile.getDomain(), pointDefinition, rawValue);
            String displayValue = javaValue == null ? null : String.valueOf(javaValue);
            items.add(DevicePointValue.builder()
                .pointId(resolvedPoint.getPointId())
                .name(pointDefinition.getName())
                .sourceAddress(pointDefinition.getAddress())
                .adapterAddress(resolvedPoint.getAdapterAddress())
                .dataType(pointDefinition.getDataType())
                .access(pointDefinition.getAccess())
                .description(pointDefinition.getDescription())
                .rawValue(javaValue)
                .displayValue(displayValue)
                .status(resolveStatus(profile.getDomain(), pointDefinition, javaValue))
                .alarm(resolveAlarm(profile.getDomain(), pointDefinition, javaValue))
                .build());
        }
        return builder.items(items).build();
    }

    public DevicePointWriteResult toWriteResult(DeviceIoPlan ioPlan, DeviceIoResult ioResult) {
        DeviceRuntimeProfile profile = ioPlan.getRuntimeProfile();
        List<String> pointIds = ioPlan.getResolvedPoints().stream()
            .map(ResolvedDevicePoint::getPointId)
            .toList();
        return DevicePointWriteResult.builder()
            .success(ioResult != null && ioResult.isSuccess())
            .errorCode(ioResult != null ? ioResult.getErrorCode() : "UNKNOWN")
            .errorMessage(ioResult != null ? ioResult.getErrorMessage() : "设备写入结果为空")
            .deviceId(profile.getDeviceConfig().getDeviceId())
            .deviceName(profile.getDeviceConfig().getDeviceName())
            .protocolType(profile.getDeviceConfig().getProtocolType())
            .rawResponseJson(ioResult != null ? ioResult.getResponseJson() : null)
            .pointIds(pointIds)
            .build();
    }

    private Map<String, JsonNode> extractReadValueMap(String responseJson) {
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

    private Object convertJavaValue(DomainEnums.CommandDomain domain,
                                    DevicePointDefinition pointDefinition,
                                    Object rawValue) {
        if (domain != DomainEnums.CommandDomain.S7) {
            return rawValue;
        }
        return s7DataTypeValueConverter.convert(pointDefinition, rawValue);
    }

    private String resolveStatus(DomainEnums.CommandDomain domain,
                                 DevicePointDefinition pointDefinition,
                                 Object javaValue) {
        if (domain != DomainEnums.CommandDomain.S7) {
            return null;
        }
        if (!StringUtils.hasText(pointDefinition.getStatusEnum())) {
            return null;
        }
        return deviceStatusEnumResolver.resolveStatus(pointDefinition.getStatusEnum(), javaValue);
    }

    private Boolean resolveAlarm(DomainEnums.CommandDomain domain,
                                 DevicePointDefinition pointDefinition,
                                 Object javaValue) {
        if (domain != DomainEnums.CommandDomain.S7) {
            return Boolean.FALSE;
        }
        if (!Boolean.TRUE.equals(pointDefinition.getAlarmEnabled())) {
            return Boolean.FALSE;
        }
        if (javaValue == null || !StringUtils.hasText(pointDefinition.getAlarmCondition())) {
            return Boolean.FALSE;
        }
        return s7AlarmConditionMatcher.matches(javaValue, pointDefinition.getAlarmCondition());
    }
}
