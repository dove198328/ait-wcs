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

    /**
     * 单次尝试执行：有活连接直接用，没有则做一次建连尝试（不循环重试）。
     * <p>监控服务使用此方法保证 2s 轮询周期不被重连循环阻塞。
     * 默认实现直接委托 {@link #execute(DeviceIoRequest)}。
     */
    default DeviceIoResult executeOnce(DeviceIoRequest request) {
        return execute(request);
    }
}
