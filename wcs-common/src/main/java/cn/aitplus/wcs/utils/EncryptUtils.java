package cn.aitplus.wcs.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * 返回加密开关与密钥获取工具。
 */
public final class EncryptUtils {
    /**
     * 配置键：是否启用加密。
     */
    private static final String ENCRYPT_ENABLED_PROPERTY = "wcs.encrypt.enabled";
    private static final boolean DEFAULT_ENCRYPT_ENABLED = false;

    private EncryptUtils() {
    }

    /**
     * 判断当前是否需要加密数据。
     * 配置 true 表示加密，false 表示不加密（默认 false）。
     */
    public static boolean needEncrypt() {
        ApplicationContext context = SpringUtils.getApplicationContext();
        if (context == null) {
            return DEFAULT_ENCRYPT_ENABLED;
        }
        Environment environment = context.getEnvironment();
        return environment.getProperty(ENCRYPT_ENABLED_PROPERTY, Boolean.class, DEFAULT_ENCRYPT_ENABLED);
    }
}
