package cn.aitplus.wcs.execution.device.io.runtime.model;

import cn.aitplus.wcs.core.domain.model.device.DevicePointDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已完成适配器地址解析的点位。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolvedDevicePoint {

    private String pointId;
    private String adapterAddress;
    private DevicePointDefinition pointDefinition;
    private Object writeValue;

    public String getPhysicalAddress() {
        return adapterAddress;
    }

    public void setPhysicalAddress(String physicalAddress) {
        this.adapterAddress = physicalAddress;
    }
}
