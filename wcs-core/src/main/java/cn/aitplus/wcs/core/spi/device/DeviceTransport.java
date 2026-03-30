package cn.aitplus.wcs.core.spi.device;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;

/**
 * 设备/三方 IO 传输抽象；实现放在 wcs-adapters（包 {@code cn.aitplus.wcs.adapters.io}），
 * 由 {@code cn.aitplus.wcs.adapters.io.registry.DeviceTransportRegistry} 按域分发。
 */
public interface DeviceTransport {

    /**
     * 是否处理该协议域。
     */
    boolean supports(DomainEnums.CommandDomain domain);

    /**
     * 执行读/写；不得抛出受检异常；异常应转为 {@link DeviceIoResult#fail(String, String)}。
     */
    DeviceIoResult execute(DeviceIoRequest request);
}
