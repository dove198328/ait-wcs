package cn.aitplus.wcs.core.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * 设备告警变化事件。alarm 翻转或告警点集合变化时发布。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAlarmChangedEvent {

    private String deviceId;
    private String deviceName;
    private Long warehouseId;
    private String protocolType;
    private boolean alarm;
    private Set<String> alarmPointIds;
    private Instant timestamp;
}
