package cn.aitplus.wcs.adapters.io.http;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.core.spi.device.DeviceTransport;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * HTTP 未启用时的占位；与 {@link HttpDeviceTransport} 并存时排序在后。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class HttpFallbackDeviceTransport implements DeviceTransport {

    @Override
    public boolean supports(DomainEnums.CommandDomain domain) {
        return domain == DomainEnums.CommandDomain.HTTP;
    }

    @Override
    public DeviceIoResult execute(DeviceIoRequest request) {
        return DeviceIoResult.fail("RCS_HTTP_DISABLED",
            "Set wcs.adapter.http.enabled=true and configure DeviceEndpoint.httpBaseUrl (or host/port).");
    }
}
