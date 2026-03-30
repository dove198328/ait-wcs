package cn.aitplus.wcs.adapters.io.opcua.transport;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.core.spi.device.DeviceTransport;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * OPC UA 未启用 Milo 客户端时的占位；与 {@link OpcUaDeviceTransport} 并存时排序在后。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class OpcUaFallbackDeviceTransport implements DeviceTransport {

    @Override
    public boolean supports(DomainEnums.CommandDomain domain) {
        return domain == DomainEnums.CommandDomain.OPC;
    }

    @Override
    public DeviceIoResult execute(DeviceIoRequest request) {
        return DeviceIoResult.fail("OPC_UA_DISABLED",
            "Set wcs.adapter.opcua.enabled=true and configure endpoint.opcEndpointUrl.");
    }
}
