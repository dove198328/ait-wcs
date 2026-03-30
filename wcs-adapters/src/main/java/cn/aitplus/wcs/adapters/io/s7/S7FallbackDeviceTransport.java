package cn.aitplus.wcs.adapters.io.s7;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.core.spi.device.DeviceTransport;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * S7 未启用 PLC 时的占位实现；与 {@link S7Plc4xDeviceTransport} 并存时排序在后。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class S7FallbackDeviceTransport implements DeviceTransport {

    @Override
    public boolean supports(DomainEnums.CommandDomain domain) {
        return domain == DomainEnums.CommandDomain.S7;
    }

    @Override
    public DeviceIoResult execute(DeviceIoRequest request) {
        return DeviceIoResult.fail("S7_DISABLED",
            "Set wcs.adapter.s7.enabled=true and configure endpoint to use PLC4X S7.");
    }
}
