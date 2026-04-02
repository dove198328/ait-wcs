package cn.aitplus.wcs.execution.device.monitor;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 设备监控配置属性。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "wcs.monitor")
public class DeviceMonitorProperties {

    private long pollIntervalMillis = 2000;

    private int ioPoolSize = 4;

    private List<String> defaultPointIds = List.of("SBZT", "XTXH", "HCZT");

    /** 逻辑点 ID，每轮 poll 读成功后写交替 0/1；空字符串表示不写。 */
    private String heartbeatWritePointId = "";
}
