package cn.aitplus.wcs.adapters.io.opcua.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OPC UA 客户端（Eclipse Milo）；默认关闭。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "wcs.adapter.opcua")
public class OpcUaAdapterProperties {

    private boolean enabled = false;

    private long defaultRequestTimeoutMillis = 5_000L;

    /** 客户端应用名（OPC UA ApplicationDescription） */
    private String applicationName = "WCS OPC UA Client";

    /** 客户端 ApplicationUri */
    private String applicationUri = "urn:aitplus:wcs:opcua-client";

    /**
     * 安全策略 URI 对应名称：None、Basic256Sha256、Basic256、Basic128Rsa15、Aes128_Sha256_RsaOaep、Aes256_Sha256_RsaPss。
     * 与服务器端点协商时优先匹配该策略。
     */
    private String securityPolicy = "None";

    /** 安全模式：None / Sign / SignAndEncrypt */
    private String messageSecurityMode = "None";

    /** 可选；空则匿名 */
    private String username = "";

    private String password = "";

    /**
     * 是否接受任意服务端证书（仅建议开发/内网）；生产应改为信任库校验并置 false。
     */
    private boolean insecureTrustServerCertificate = true;

    /**
     * 单次 Read / Write 服务调用中最多包含的 NodeId 个数；超过则自动拆成多次请求（多地址场景避免触达服务端限制）。
     */
    private int maxNodesPerServiceCall = 200;

    /**
     * 复用连接前是否读取标准节点 Server_ServerStatus_State 做应用层探活；false 则仅依赖会话 Future。
     */
    private boolean sessionProbeEnabled = true;

    /**
     * 连接空闲超过该毫秒数则下次借用前断开重连；0 表示不启用空闲淘汰。
     */
    private long idleMaxMillis = 0L;

    /**
     * {@link cn.aitplus.wcs.adapters.io.opcua.transport.OpcUaDeviceTransport#execute} 是否允许读（同步 Read）；与订阅无关。
     */
    private boolean executeReadEnabled = true;

    /**
     * {@link cn.aitplus.wcs.adapters.io.opcua.transport.OpcUaDeviceTransport#execute} 是否允许写（同步 Write）；与订阅无关。
     */
    private boolean executeWriteEnabled = true;

    /** 是否启用 MonitoredItem 订阅（监听/推送）；false 时不可 register，且不挂订阅相关监听逻辑。 */
    private boolean subscriptionEnabled = true;

    /** 创建 Subscription 时的 requestedPublishingInterval（毫秒，OPC UA 双精度） */
    private double subscriptionPublishingIntervalMillis = 1_000.0;

    /** requestedMaxKeepAliveCount */
    private long subscriptionMaxKeepAliveCount = 10L;

    /** requestedLifetimeCount，建议大于等于 3 倍 keepAliveCount */
    private long subscriptionLifetimeCount = 60L;

    /** requestedMaxNotificationsPerPublish，0 表示由服务端决定 */
    private long subscriptionMaxNotificationsPerPublish = 0L;

    /** Subscription priority */
    private int subscriptionPriority = 0;

    /**
     * 映射到 Milo {@link org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription#setTargetKeepAliveInterval(double)}：
     * 期望的订阅 keep-alive 时间间隔（毫秒）。大于 0 时由 Milo 根据 {@link #subscriptionPublishingIntervalMillis} 推导
     * {@code MaxKeepAliveCount}/{@code LifetimeCount}，此时不再使用 {@link #subscriptionMaxKeepAliveCount}、{@link #subscriptionLifetimeCount}。
     * 小于等于 0 时关闭该推导，仅使用上述 count 配置。
     */
    private long subscriptionWatchdogIntervalMillis = 0L;

    /**
     * Milo {@link org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription#setWatchdogMultiplier(double)}：
     * 客户端在修订发布间隔基础上判定「过久无 publish 活动」的倍数（与上项 OPC keep-alive 语义不同）。
     */
    private long subscriptionWatchdogSilenceMultiplier = 3L;

    /** 订阅重建初始延迟 */
    private long rebuildInitialDelayMillis = 500L;

    /** 订阅重建最大延迟 */
    private long rebuildMaxDelayMillis = 30_000L;
}
