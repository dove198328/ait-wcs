package cn.aitplus.wcs.adapters.io.connection;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceEndpoint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Locale;

/**
 * 连接池键：仓 + 协议 + 端点，使同 IP 多逻辑设备共享连接。
 */
@Getter
@EqualsAndHashCode(of = "raw")
public final class ConnectionKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String raw;

    private ConnectionKey(String raw) {
        this.raw = raw;
    }

    public static ConnectionKey from(Long warehouseId, DomainEnums.CommandDomain domain, DeviceEndpoint endpoint) {
        if (warehouseId == null || domain == null || endpoint == null) {
            throw new IllegalArgumentException("warehouseId, domain, endpoint required");
        }
        switch (domain) {
            case S7:
                if (!StringUtils.hasText(endpoint.getHost())) {
                    throw new IllegalArgumentException("S7 endpoint.host required");
                }
                int rack = endpoint.getRack() != null ? endpoint.getRack() : 0;
                int slot = endpoint.getSlot() != null ? endpoint.getSlot() : 1;
                int s7port = endpoint.getPort() > 0 ? endpoint.getPort() : 102;
                return new ConnectionKey(String.format(Locale.ROOT, "%d|S7|%s|%d|%d|%d",
                    warehouseId, endpoint.getHost().trim().toLowerCase(Locale.ROOT), s7port, rack, slot));
            case MODBUS:
                if (!StringUtils.hasText(endpoint.getHost())) {
                    throw new IllegalArgumentException("Modbus endpoint.host required");
                }
                int p = endpoint.getPort() > 0 ? endpoint.getPort() : 502;
                return new ConnectionKey(String.format(Locale.ROOT, "%d|MODBUS|%s|%d",
                    warehouseId, endpoint.getHost().trim().toLowerCase(Locale.ROOT), p));
            case OPC:
                String opc = endpoint.getOpcEndpointUrl() != null ? endpoint.getOpcEndpointUrl().trim() : "";
                if (!StringUtils.hasText(opc)) {
                    throw new IllegalArgumentException("OPC endpoint.opcEndpointUrl required");
                }
                return new ConnectionKey(warehouseId + "|OPC|" + opc);
            case HTTP:
                String http = endpoint.getHttpBaseUrl() != null ? endpoint.getHttpBaseUrl().trim() : "";
                if (!StringUtils.hasText(http)) {
                    throw new IllegalArgumentException("HTTP endpoint.httpBaseUrl required");
                }
                return new ConnectionKey(warehouseId + "|HTTP|" + http);
            default:
                throw new IllegalArgumentException("Unsupported domain: " + domain);
        }
    }

    @Override
    public String toString() {
        return raw;
    }
}
