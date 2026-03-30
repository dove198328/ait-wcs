package cn.aitplus.wcs.adapters.io.http;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.core.spi.device.DeviceTransport;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * RCS（HTTP）域占位实现；后续可替换为连接池 + 实际 API 调用。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class HttpStubTransport implements DeviceTransport {

    @Override
    public boolean supports(DomainEnums.CommandDomain domain) {
        return domain == DomainEnums.CommandDomain.RCS;
    }

    @Override
    public DeviceIoResult execute(DeviceIoRequest request) {
        return DeviceIoResult.fail("NOT_IMPLEMENTED", "RCS HTTP transport is not implemented yet.");
    }
}
