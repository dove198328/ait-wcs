package cn.aitplus.wcs.core.domain.model.device;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 设备点位配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DevicePointsConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String deviceId;
    private String description;
    private String deviceType;

    @Builder.Default
    private Map<String, DevicePointDefinition> pointsConfig = new LinkedHashMap<>();
}
