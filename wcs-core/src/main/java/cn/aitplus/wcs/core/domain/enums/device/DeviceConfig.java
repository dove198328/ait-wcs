package cn.aitplus.wcs.core.domain.enums.device;

import cn.aitplus.wcs.core.domain.enums.ProtocolType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备配置实体类
 * 用于保存设备的基本配置信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Api(tags = "设备配置实体类")
public class DeviceConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    @ApiModelProperty("设备ID")
    private String deviceId;

    /**
     * 设备名称
     */
    @ApiModelProperty("设备名称")
    private String deviceName;

    /**
     * 协议类型，例如：s7, modbus, ads, eip等
     */
    @ApiModelProperty("协议类型")
    private ProtocolType protocolType;

    /**
     * 连接地址，例如：s7:127.0.0.1，modbus:tcp://192.168.1.10:502
     */
    @ApiModelProperty("连接地址")
    private String connectionString;

    @ApiModelProperty("设备类型")
    private String deviceType;

    @ApiModelProperty("操作类型")
    private String action;

    @ApiModelProperty("设备点位路径")
    private String pointPath;

    @ApiModelProperty("仓库ID")
    private String warehouseIds;

    @ApiModelProperty("货道号")
    private Long aisleNo;

    @ApiModelProperty("排/列/层")
    private String deviceArrange;

    @ApiModelProperty("绑定的设备id，主要用于摄像头")
    private String relatedDeviceId;

    @ApiModelProperty("排，逗号分割，用于摄像头判断左右")
    private String rowNums;

    public String getProtocolTypeString() {
        return protocolType != null ? protocolType.getProtocol() : null;
    }

    public void setProtocolTypeString(String protocolType) {
        this.protocolType = ProtocolType.valueOf(protocolType.toUpperCase().replace('-', '_'));
    }

    public void setProtocolType(String protocolType){
        this.protocolType = ProtocolType.fromString(protocolType);
    }
} 