package cn.aitplus.wcs.execution.device.io.runtime;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * 设备连接串解析器。
 */
@Component
public class DeviceEndpointResolver {

    public DeviceEndpoint resolveEndpoint(DomainEnums.CommandDomain domain, String connectionString) {
        if (domain == null) {
            throw new IllegalStateException("设备协议域为空，无法解析连接端点");
        }
        if (!StringUtils.hasText(connectionString)) {
            throw new IllegalStateException("设备连接串为空，无法解析连接端点");
        }
        String trimmed = connectionString.trim();
        return switch (domain) {
            case S7 -> resolveS7Endpoint(trimmed);
            case MODBUS -> resolveModbusEndpoint(trimmed);
            case RCS -> DeviceEndpoint.builder().httpBaseUrl(trimmed).build();
            case OPC -> DeviceEndpoint.builder().opcEndpointUrl(trimmed).build();
        };
    }

    private DeviceEndpoint resolveS7Endpoint(String connectionString) {
        URI uri = URI.create(connectionString);
        return DeviceEndpoint.builder()
            .host(requireHost(uri, connectionString))
            .port(uri.getPort() > 0 ? uri.getPort() : 102)
            .rack(parseIntegerQuery(uri, "remote-rack", 0))
            .slot(parseIntegerQuery(uri, "remote-slot", 1))
            .build();
    }

    private DeviceEndpoint resolveModbusEndpoint(String connectionString) {
        URI uri = URI.create(connectionString);
        return DeviceEndpoint.builder()
            .host(requireHost(uri, connectionString))
            .port(uri.getPort() > 0 ? uri.getPort() : 502)
            .modbusUnitId(resolveModbusUnitId(uri))
            .build();
    }

    private Integer resolveModbusUnitId(URI uri) {
        Integer unitId = parseOptionalIntegerQuery(uri, "unitId");
        if (unitId != null) {
            return unitId;
        }
        unitId = parseOptionalIntegerQuery(uri, "unit-id");
        if (unitId != null) {
            return unitId;
        }
        unitId = parseOptionalIntegerQuery(uri, "slave");
        if (unitId != null) {
            return unitId;
        }
        return parseOptionalIntegerQuery(uri, "slaveId");
    }

    private String requireHost(URI uri, String connectionString) {
        if (StringUtils.hasText(uri.getHost())) {
            return uri.getHost().trim();
        }
        throw new IllegalStateException("连接串缺少主机信息：" + connectionString);
    }

    private Integer parseIntegerQuery(URI uri, String name, int defaultValue) {
        Integer value = parseOptionalIntegerQuery(uri, name);
        return value != null ? value : defaultValue;
    }

    private Integer parseOptionalIntegerQuery(URI uri, String name) {
        String query = uri.getQuery();
        if (!StringUtils.hasText(query)) {
            return null;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int index = pair.indexOf('=');
            if (index <= 0) {
                continue;
            }
            String key = pair.substring(0, index);
            if (!name.equals(key)) {
                continue;
            }
            String value = pair.substring(index + 1).trim();
            if (!StringUtils.hasText(value)) {
                return null;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new IllegalStateException("连接串中的参数不是合法数字：" + name + "=" + value, ex);
            }
        }
        return null;
    }
}
