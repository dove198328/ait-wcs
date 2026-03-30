package cn.aitplus.wcs.adapters.io.opcua;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.core.spi.device.DeviceTransport;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class OpcUaStubDeviceTransport implements DeviceTransport {

    @Override
    public boolean supports(DomainEnums.CommandDomain domain) {
        return domain == DomainEnums.CommandDomain.OPC;
    }

    @Override
    public DeviceIoResult execute(DeviceIoRequest request) {
        return DeviceIoResult.fail("NOT_IMPLEMENTED", "OPC UA transport is not implemented yet.");
    }
}
