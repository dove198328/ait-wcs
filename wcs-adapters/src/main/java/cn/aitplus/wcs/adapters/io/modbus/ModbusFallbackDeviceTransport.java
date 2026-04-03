package cn.aitplus.wcs.adapters.io.modbus;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.core.spi.device.DeviceTransport;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 未启用真实 Modbus Bean 时的占位；与 {@link ModbusTcpDeviceTransport} 并存时排序在后。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ModbusFallbackDeviceTransport implements DeviceTransport {

    @Override
    public boolean supports(DomainEnums.CommandDomain domain) {
        return domain == DomainEnums.CommandDomain.MODBUS;
    }

    @Override
    public DeviceIoResult execute(DeviceIoRequest request) {
        return DeviceIoResult.fail("MODBUS_DISABLED",
                "Set wcs.adapter.modbus.enabled=true to use Modbus TCP (j2mod).");
    }
}
