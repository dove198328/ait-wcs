package cn.aitplus.wcs.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * WMS 集成相关配置，与 {@code application*.yml} 中 {@code wms.*} 对齐。
 */
@Data
@Component
@ConfigurationProperties(prefix = "wms")
public class WmsProperties {

    private String url = "";
    private String upload = "";
    private List<String> syncPointsDir = new ArrayList<>();
    private Api api = new Api();

    /** 已解析占位符后的设备列表同步完整 URL（含 query）。 */
    public String resolvedSyncDeviceUrl() {
        String u = getApi() != null ? getApi().getSyncDevice() : null;
        return StringUtils.hasText(u) ? u.trim() : "";
    }

    @Data
    public static class Api {
        /** 对应 YAML {@code sync_device} */
        private String syncDevice = "";
    }
}
