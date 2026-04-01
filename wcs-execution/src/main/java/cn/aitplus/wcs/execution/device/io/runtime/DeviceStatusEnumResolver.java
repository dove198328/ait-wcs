package cn.aitplus.wcs.execution.device.io.runtime;

import cn.aitplus.wcs.core.domain.model.device.DeviceStatusValueEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 设备状态枚举解析器。
 */
@Component
public class DeviceStatusEnumResolver {

    private static final String[] SEARCH_PACKAGES = {
        "cn.aitplus.wcs.core.domain.enums",
        "cn.aitplus.wcs.core.domain.enums.device",
        "cn.aitplus.wcs.core.domain.model.device"
    };

    public String resolveStatus(String statusEnum, Object rawValue) {
        if (!StringUtils.hasText(statusEnum) || rawValue == null) {
            return null;
        }
        Class<?> enumClass = loadEnumClass(statusEnum.trim());
        if (!enumClass.isEnum()) {
            throw new IllegalStateException("状态枚举配置不是枚举类型：" + statusEnum);
        }
        if (!DeviceStatusValueEnum.class.isAssignableFrom(enumClass)) {
            throw new IllegalStateException("状态枚举必须实现 DeviceStatusValueEnum：" + statusEnum);
        }
        String rawCode = String.valueOf(rawValue);
        Object[] constants = enumClass.getEnumConstants();
        for (Object constant : constants) {
            DeviceStatusValueEnum statusValueEnum = (DeviceStatusValueEnum) constant;
            if (rawCode.equals(statusValueEnum.getCode())) {
                return statusValueEnum.getStatus();
            }
        }
        return null;
    }

    private Class<?> loadEnumClass(String statusEnum) {
        if (statusEnum.contains(".")) {
            return tryLoad(statusEnum);
        }
        for (String searchPackage : SEARCH_PACKAGES) {
            try {
                return tryLoad(searchPackage + "." + statusEnum);
            } catch (IllegalStateException ignored) {
                // 继续尝试下一个包
            }
        }
        throw new IllegalStateException("未找到状态枚举类型：" + statusEnum);
    }

    private Class<?> tryLoad(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("未找到状态枚举类型：" + className, ex);
        }
    }
}
