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

    /** JetCache {@code name}：流程定义按主键（条目 key 为 warehouseId:id） */
    public static final String WORKFLOW_BY_ID_CACHE_NAME = "wcs:workflow:id:";
    public static final String WORKFLOW_BY_BIZ_TYPE_CACHE_NAME = "wcs:workflow:biz-type:";
    public static final String WORKFLOW_BY_NAME_CACHE_NAME = "wcs:workflow:name:";
    public static final String WORKFLOW_BY_WORKFLOW_ID_CACHE_NAME = "wcs:workflow:workflow-id:";

    /** JetCache {@code name}：仓库画像 / 责任链 */
    public static final String PROFILE_CACHE_NAME = "wcs:profile:";
    public static final String CHAIN_CACHE_NAME = "wcs:chain:";

    /** 缓存条目 key 中仓库维度与业务段之间的分隔符（与区域名里的冒号无关） */
    public static final String CACHE_KEY_SEPARATOR = ":";
}
