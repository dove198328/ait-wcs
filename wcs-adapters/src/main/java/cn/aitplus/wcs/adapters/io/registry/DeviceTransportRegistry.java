package cn.aitplus.wcs.adapters.io.registry;

import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.core.spi.device.DeviceTransport;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 按 {@link DeviceTransport#supports} 选择首个实现；实现类可用 {@code @Order} 或 {@link org.springframework.core.Ordered} 控制优先级。
 */
@Component
public class DeviceTransportRegistry {

    private final List<DeviceTransport> transports;

    public DeviceTransportRegistry(List<DeviceTransport> transports) {
        List<DeviceTransport> list = transports == null ? new ArrayList<>() : new ArrayList<>(transports);
        AnnotationAwareOrderComparator.sort(list);
        this.transports = list;
    }

    public DeviceIoResult execute(DeviceIoRequest request) {
        return dispatch(request, transport -> transport.execute(request));
    }

    public DeviceIoResult executeWithNewConnection(DeviceIoRequest request) {
        return dispatch(request, transport -> transport.executeWithNewConnection(request));
    }

    private DeviceIoResult dispatch(DeviceIoRequest request,
                                    Function<DeviceTransport, DeviceIoResult> executor) {
        if (request == null || request.getDomain() == null) {
            return DeviceIoResult.fail("INVALID_REQUEST", "request or domain is null");
        }
        for (DeviceTransport transport : transports) {
            if (transport.supports(request.getDomain())) {
                try {
                    return executor.apply(transport);
                } catch (Exception e) {
                    return DeviceIoResult.fail("TRANSPORT_ERROR",
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                }
            }
        }
        return DeviceIoResult.fail("NO_TRANSPORT", "No DeviceTransport for domain " + request.getDomain());
    }
}
