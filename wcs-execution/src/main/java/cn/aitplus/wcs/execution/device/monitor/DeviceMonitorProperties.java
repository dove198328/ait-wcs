package cn.aitplus.wcs.execution.device.monitor;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Monitor scheduler settings plus protocol-specific polling defaults.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "wcs.monitor")
public class DeviceMonitorProperties {

    private long pollIntervalMillis = 2000;

    private int ioPoolSize = 4;

    private Map<String, ProtocolMonitorProperties> protocols = new LinkedHashMap<>();

    public List<String> getDefaultPointIds(DomainEnums.CommandDomain domain) {
        return getProtocolProperties(domain).getDefaultPointIds();
    }

    public String getHeartbeatWritePointId(DomainEnums.CommandDomain domain) {
        return getProtocolProperties(domain).getHeartbeatWritePointId();
    }

    private ProtocolMonitorProperties getProtocolProperties(DomainEnums.CommandDomain domain) {
        if (domain == null || protocols == null || protocols.isEmpty()) {
            return new ProtocolMonitorProperties();
        }
        String key = domain.name().toLowerCase(Locale.ROOT);
        ProtocolMonitorProperties props = protocols.get(key);
        return props != null ? props : new ProtocolMonitorProperties();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProtocolMonitorProperties {

        private List<String> defaultPointIds = List.of();

        private String heartbeatWritePointId = "";

        public ProtocolMonitorProperties(List<String> defaultPointIds, String heartbeatWritePointId) {
            this.defaultPointIds = defaultPointIds;
            this.heartbeatWritePointId = heartbeatWritePointId;
        }
    }
}
