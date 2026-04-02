package cn.aitplus.wcs.adapters.io.s7;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S7（PLC4X）适配；默认关闭，避免无 PLC 环境启动失败。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "wcs.adapter.s7")
public class S7AdapterProperties {

    private boolean enabled = false;

    private long defaultRequestTimeoutMillis = 5_000L;

    private int maxReconnectAttempts = 8;

    private long reconnectInitialDelayMillis = 500L;

    private long reconnectMaxDelayMillis = 30_000L;

    /**
     * 每次取用连接前做轻量探测（{@link org.apache.plc4x.java.api.PlcConnection#ping()}），
     * 失败则丢弃缓存连接并在本次/下次请求重建，减轻 TCP 半开、假活。
     */
    private boolean borrowProbeEnabled = true;

    /** 借用探测超时（毫秒） */
    private long borrowProbeTimeoutMillis = 3_000L;
}
