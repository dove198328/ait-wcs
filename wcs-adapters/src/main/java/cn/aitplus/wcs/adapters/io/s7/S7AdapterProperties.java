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

    /** 心跳周期（毫秒）；≤0 表示关闭 */
    private long heartbeatIntervalMillis = 0L;

    /** 心跳周期读地址（PLC4X 地址串） */
    private String heartbeatReadAddress = "";

    /**
     * 每次取用连接前做轻量探测（优先 {@link org.apache.plc4x.java.api.PlcConnection#ping()}，不支持则用 heartbeat 地址读），
     * 失败则丢弃缓存连接并在本次/下次请求重建，减轻 TCP 半开、假活。
     */
    private boolean borrowProbeEnabled = true;

    /** 借用探测超时（毫秒） */
    private long borrowProbeTimeoutMillis = 3_000L;

    /**
     * 定时巡检间隔（毫秒）：对池中连接做 isConnected + 借用探测；≤0 关闭。
     * 与心跳互补，避免长期无流量时假活不被发现。
     */
    private long staleCheckIntervalMillis = 60_000L;
}
