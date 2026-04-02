package cn.aitplus.wcs.execution.device.monitor;

import cn.aitplus.wcs.adapters.io.connection.ConnectionKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 启动时从 Redis 配置构建的监控索引。
 */
@Getter
@Builder
@AllArgsConstructor
public class MonitorIndex {

    /** deviceId → 定义 */
    private final Map<String, DeviceMonitorDefinition> definitionsByDeviceId;

    /** ConnectionKey → 同连接下的设备定义（S7 / Modbus） */
    private final Map<ConnectionKey, List<DeviceMonitorDefinition>> groups;
}
