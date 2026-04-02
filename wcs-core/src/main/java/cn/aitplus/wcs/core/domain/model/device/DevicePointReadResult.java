package cn.aitplus.wcs.core.domain.model.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备点位读取结果。
 * 一次性业务读取与监控内存快照统一复用该模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevicePointReadResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String errorCode;
    private String errorMessage;
    private String deviceId;
    private String deviceName;
    private Long warehouseId;
    private String protocolType;
    private String rawResponseJson;
    private Instant updatedAt;

    @Builder.Default
    private List<DevicePointValue> items = new ArrayList<>();
}
