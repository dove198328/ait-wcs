package cn.aitplus.wcs.execution.device.io.runtime.point;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.domain.model.device.DevicePointDefinition;
import cn.aitplus.wcs.core.domain.model.device.DevicePointValue;
import cn.aitplus.wcs.execution.device.io.runtime.model.ResolvedDevicePoint;
import cn.aitplus.wcs.execution.device.io.s7.S7AlarmConditionMatcher;
import cn.aitplus.wcs.execution.device.io.s7.S7DataTypeValueConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 统一完成点位值的类型转换、状态解析、告警判断和结果对象映射。
 */
@Component
public class DevicePointValueMapper {

    private final S7DataTypeValueConverter dataTypeValueConverter;
    private final DeviceStatusEnumResolver deviceStatusEnumResolver;
    private final S7AlarmConditionMatcher alarmConditionMatcher;

    public DevicePointValueMapper(S7DataTypeValueConverter dataTypeValueConverter,
                                  DeviceStatusEnumResolver deviceStatusEnumResolver,
                                  S7AlarmConditionMatcher alarmConditionMatcher) {
        this.dataTypeValueConverter = dataTypeValueConverter;
        this.deviceStatusEnumResolver = deviceStatusEnumResolver;
        this.alarmConditionMatcher = alarmConditionMatcher;
    }

    public DevicePointValue toPointValue(DomainEnums.CommandDomain domain,
                                         ResolvedDevicePoint resolvedPoint,
                                         Object rawValue) {
        DevicePointDefinition pointDefinition = resolvedPoint.getPointDefinition();
        Object javaValue = convertJavaValue(domain, pointDefinition, rawValue);
        String displayValue = javaValue == null ? null : String.valueOf(javaValue);
        return DevicePointValue.builder()
                .pointId(resolvedPoint.getPointId())
                .name(pointDefinition.getName())
                .sourceAddress(pointDefinition.getAddress())
                .adapterAddress(resolvedPoint.getAdapterAddress())
                .dataType(pointDefinition.getDataType())
                .access(pointDefinition.getAccess())
                .description(pointDefinition.getDescription())
                .rawValue(javaValue)
                .displayValue(displayValue)
                .status(resolveStatus(domain, pointDefinition, javaValue))
                .alarm(resolveAlarm(domain, pointDefinition, javaValue))
                .build();
    }

    private Object convertJavaValue(DomainEnums.CommandDomain domain,
                                    DevicePointDefinition pointDefinition,
                                    Object rawValue) {
        if (domain != DomainEnums.CommandDomain.S7) {
            return rawValue;
        }
        return dataTypeValueConverter.convert(pointDefinition, rawValue);
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
        return alarmConditionMatcher.matches(javaValue, pointDefinition.getAlarmCondition());
    }
}
