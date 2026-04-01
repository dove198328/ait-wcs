package cn.aitplus.wcs.core.domain.model.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备点位写入结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevicePointWriteResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String errorCode;
    private String errorMessage;
    private String deviceId;
    private String deviceName;
    private String protocolType;
    private String rawResponseJson;

    @Builder.Default
    private List<String> pointIds = new ArrayList<>();
}
