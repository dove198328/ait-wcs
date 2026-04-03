package cn.aitplus.wcs.execution.device.io.runtime.protocol;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 设备协议类型解析器。
 */
@Component
public class DeviceProtocolResolver {

    public DomainEnums.CommandDomain resolveDomain(String protocolType) {
        if (!StringUtils.hasText(protocolType)) {
            throw new IllegalStateException("设备协议类型为空，无法解析适配器类型");
        }
        String normalized = protocolType.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("s7")) {
            return DomainEnums.CommandDomain.S7;
        }
        if (normalized.contains("modbus")) {
            return DomainEnums.CommandDomain.MODBUS;
        }
        if (normalized.contains("opc")) {
            return DomainEnums.CommandDomain.OPC;
        }
        if (normalized.contains("http") || normalized.contains("https")) {
            return DomainEnums.CommandDomain.HTTP;
        }
        throw new IllegalStateException("暂不支持的设备协议类型：" + protocolType);
    }
}