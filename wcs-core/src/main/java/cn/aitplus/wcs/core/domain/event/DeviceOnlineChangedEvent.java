package cn.aitplus.wcs.core.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 设备上线/离线事件。仅在 online 状态翻转时发布。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceOnlineChangedEvent {

    private String deviceId;
    private String deviceName;
    private Long warehouseId;
    private String protocolType;
    private boolean online;
    private Instant timestamp;
}
