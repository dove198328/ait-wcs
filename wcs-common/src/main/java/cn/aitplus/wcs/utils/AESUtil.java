package cn.aitplus.wcs.utils;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * AES 工具类。
 */
public final class AESUtil {
    private static final Logger log = LoggerFactory.getLogger(AESUtil.class);

    /**
     * 默认密钥
     */
    public static final String DEFAULT_SECRET_KEY = "nsz3*H&I@xINg/tH";

    /**
     *  ASCII
     */
    public static final String ASCII = "ASCII";

    /**
     * 加密解密算法/加密模式/填充方式
     */
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

    /**
     * IV长度
     */
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    static {
        java.security.Security.setProperty("crypto.policy", "unlimited");
    }

    /**
     * AES加密
     */
    public static String encrypt(String content) {
        if (StrUtil.isBlank(content)) {
            log.error("加密字段为空！");
            return "";
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            SecretKey secretKey = generateMySQLAESKey(DEFAULT_SECRET_KEY, ASCII);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] byteEncode = content.getBytes(StandardCharsets.UTF_8);
            byte[] byteAes = cipher.doFinal(byteEncode);

            byte[] combined = new byte[iv.length + byteAes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(byteAes, 0, combined, iv.length, byteAes.length);

            return HexUtil.encodeHexStr(combined);
        } catch (Exception e) {
            log.error("加密失败", e);
        }
        return null;
    }

    /**
     * AES解密
     */
    public static String decrypt(String content) {
        if (StrUtil.isBlank(content)) {
            log.error("解密字段为空！");
            return "";
        }
        try {
            byte[] combined = HexUtil.decodeHex(content);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];

            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            SecretKey secretKey = generateMySQLAESKey(DEFAULT_SECRET_KEY, ASCII);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] byteDecode = cipher.doFinal(encrypted);
            return new String(byteDecode, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("aes解密失败", e);
        }
        return null;
    }

    public static SecretKeySpec generateMySQLAESKey(final String key, final String encoding) {
        try {
            final byte[] finalKey = new byte[16];
            int i = 0;
            for (byte b : key.getBytes(Charset.forName(encoding))) {
                finalKey[i++ % 16] ^= b;
            }
            return new SecretKeySpec(finalKey, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("生成AES密钥失败", e);
        }
    }
}
