package cn.aitplus.wcs.common.constant;

/**
 * WCS 共用常量：流程部署来源、JetCache 区域名（name）与缓存条目 key 约定。
 * <p>
 * 条目 key 在注解中通过 SpEL 拼接（如 {@code warehouseId + ':' + name}），不再提供 {@code build*} 辅助方法。
 */
public final class WcsConstants {

    private WcsConstants() {
    }

    /** Flowable / 流程部署来源标识 */
    public static final String DEPLOYMENT_SOURCE = "wcs-workflow-definition";

    /**
     * JetCache {@code name}：流程定义单一区域；条目 key 须带类型前缀：
     * {@code id:warehouseId:pk}、{@code wf:warehouseId:workflowId}、{@code biz:...}、{@code name:...}。
     */
    public static final String WORKFLOW_DEFINITION_CACHE_NAME = "wcs:workflow:def:";

    /** JetCache {@code name}：仓库画像 / 责任链 */
    public static final String PROFILE_CACHE_NAME = "wcs:profile:";
    public static final String CHAIN_CACHE_NAME = "wcs:chain:";

    /** 缓存条目 key 中仓库维度与业务段之间的分隔符（与区域名里的冒号无关） */
    public static final String CACHE_KEY_SEPARATOR = ":";

    /**
     * Redis 设备配置键前缀；与 WMS 同步写入的 JSON 对象共用此前缀，便于后续设备 IO/状态读取。
     */
    public static final String DEVICE_CONFIG_KEY_PREFIX = "device:config:";
}
