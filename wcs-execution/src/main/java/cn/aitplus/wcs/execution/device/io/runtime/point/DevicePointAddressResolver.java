package cn.aitplus.wcs.execution.device.io.runtime.point;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.domain.model.device.DevicePointDefinition;
import cn.aitplus.wcs.execution.device.io.s7.S7PointAddressConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 点位地址解析器。
 */
@Component
public class DevicePointAddressResolver {

    private final S7PointAddressConverter s7PointAddressConverter;

    public DevicePointAddressResolver(S7PointAddressConverter s7PointAddressConverter) {
        this.s7PointAddressConverter = s7PointAddressConverter;
    }

    public String resolve(DomainEnums.CommandDomain domain, DevicePointDefinition pointDefinition) {
        if (domain == null) {
            throw new IllegalStateException("设备协议域为空，无法解析点位地址");
        }
        if (pointDefinition == null || !StringUtils.hasText(pointDefinition.getAddress())) {
            throw new IllegalStateException("设备点位地址为空，无法解析点位地址");
        }
        return switch (domain) {
            case S7 -> s7PointAddressConverter.convert(pointDefinition);
            case MODBUS, HTTP, OPC -> pointDefinition.getAddress().trim();
        };
    }
}
