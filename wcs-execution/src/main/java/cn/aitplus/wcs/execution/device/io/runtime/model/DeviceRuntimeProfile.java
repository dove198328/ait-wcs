package cn.aitplus.wcs.execution.device.io.runtime.model;

import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.domain.model.device.DeviceConfig;
import cn.aitplus.wcs.core.domain.model.device.DevicePointsConfig;
import cn.aitplus.wcs.core.spi.device.DeviceEndpoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备运行时画像。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRuntimeProfile {

    private Long warehouseId;
    private DeviceConfig deviceConfig;
    private DevicePointsConfig devicePointsConfig;
    private DomainEnums.CommandDomain domain;
    private DeviceEndpoint endpoint;
}
