package cn.aitplus.wcs.core.spi.device;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 协议无关设备 IO 请求；由 execution 组装，适配器执行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceIoRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long warehouseId;

    private DomainEnums.CommandDomain domain;

    private DeviceEndpoint endpoint;

    @Builder.Default
    private List<DeviceIoItem> items = new ArrayList<>();

    /** I/O 超时（毫秒） */
    private long timeoutMillis;

    private String idempotencyKey;

    private String traceId;

    private String correlationId;

    /** 业务设备 ID，仅用于日志/映射，不参与连接池键 */
    private String deviceId;
}
