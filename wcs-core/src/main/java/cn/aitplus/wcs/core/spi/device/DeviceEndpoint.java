package cn.aitplus.wcs.core.spi.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备连接端点（按 IP/端口 + 协议参数复用 TCP/会话），与业务 {@code deviceId} 解耦。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceEndpoint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主机名或 IP */
    private String host;

    /** TCP 端口；S7 默认 102，Modbus 默认 502 */
    private int port;

    /** S7：机架号，默认 0 */
    private Integer rack;

    /** S7：槽位号，默认 1 */
    private Integer slot;

    /** Modbus：单元号，在请求级携带；此处可选默认 */
    private Integer modbusUnitId;

    /** OPC UA：endpoint URL；与 host/port 二选一由适配器解释 */
    private String opcEndpointUrl;

    /** RCS 等 HTTP：base URL（含协议），如 https://rcs.example.com/api */
    private String httpBaseUrl;
}
