package cn.aitplus.wcs.adapters.io.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RCS（HTTP）适配：无连接池，{@link org.springframework.http.client.SimpleClientHttpRequestFactory}。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "wcs.adapter.http")
public class HttpAdapterProperties {

    private boolean enabled = false;

    /** 连接与读取超时（毫秒），当 {@link cn.aitplus.wcs.core.spi.device.DeviceIoRequest#getTimeoutMillis()} 未指定时使用 */
    private long defaultRequestTimeoutMillis = 5_000L;
}
