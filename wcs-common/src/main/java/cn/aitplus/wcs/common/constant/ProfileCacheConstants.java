package cn.aitplus.wcs.common.constant;

/**
 * Warehouse profile/cache shared constants.
 */
public final class ProfileCacheConstants {

    private ProfileCacheConstants() {
    }

    public static final String PROFILE_CACHE_NAME = "wcs:profile:";
    public static final String CHAIN_CACHE_NAME = "wcs:chain:";
    public static final String CACHE_KEY_SEPARATOR = ":";

    public static String buildChainCacheKey(Long warehouseId, String chainName) {
        return warehouseId + CACHE_KEY_SEPARATOR + chainName;
    }
}
