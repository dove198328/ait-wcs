package cn.aitplus.wcs.core.domain.model.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备点位值。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevicePointValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private String pointId;
    private String name;
    private String sourceAddress;
    private String adapterAddress;
    private String dataType;
    private String access;
    private String description;
    private Object rawValue;
    private String displayValue;
    private String status;
    private Boolean alarm;
}
