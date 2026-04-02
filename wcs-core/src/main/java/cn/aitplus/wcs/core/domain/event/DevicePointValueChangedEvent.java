package cn.aitplus.wcs.core.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 动态附加监控点值变化事件。仅在值实际变化时发布，工作流监听后推进流程。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevicePointValueChangedEvent {

    private String deviceId;
    private String pointId;
    private Object oldValue;
    private Object newValue;
    private Instant timestamp;
}
