package cn.aitplus.wcs.plc4x;

import cn.aitplus.wcs.core.domain.enums.device.DeviceConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * PLC连接工厂
 * 负责创建和管理PLC连接对象
 */
@Slf4j
public class Plc4xConnectionStringUtil {
    /**
     * 构建PLC连接字符串
     * 基于设备配置的protocol和connectionString生成PLC4X连接串
     */
    public static String buildConnectionString(DeviceConfig config, int modbusUnitId, int eipBackplane, int eipSlot) {
        // 获取协议类型和连接字符串
        String protocolType = config.getProtocolType().getProtocol();
        String connectionString = config.getConnectionString();
        
        // 如果配置的connectionString已经是完整的连接字符串，直接使用
        if (connectionString != null && !connectionString.isEmpty()) {
            String normalized = protocolType == null ? "" : protocolType.toLowerCase();
            if (normalized.startsWith("plc4x-")) {
                normalized = normalized.substring(6);
            }
            // 根据协议类型附加不同的参数
            if ("s7".equals(normalized) || "plc4x-s7".equalsIgnoreCase(protocolType)) {
                // 创建S7连接
                if (!connectionString.contains("?")) {
                    connectionString = String.format("%s?remote-rack=0&remote-slot=1", connectionString);
                } else if (!connectionString.contains("field-optimization")) {
                    connectionString = String.format("%s&field-optimization=true", connectionString);
                }
            } else if (normalized.startsWith("modbus")) {
                // 处理Modbus不同变种
                if ("modbus-tcp".equals(normalized)) {
                    // Modbus TCP连接 - 只需要添加unit-identifier参数
                    if (!connectionString.contains("?")) {
                        connectionString = String.format("%s?unit-identifier=%d&requestTimeout=2000", connectionString, modbusUnitId);
                    } else if (!connectionString.contains("unit-identifier")) {
                        connectionString = String.format("%s&unit-identifier=%d&requestTimeout=2000", connectionString, modbusUnitId);
                    } else if (!connectionString.contains("requestTimeout")) {
                        connectionString = String.format("%s&requestTimeout=2000", connectionString);
                    }
                } else if ("modbus-rtu".equals(normalized) || "modbus-ascii".equals(normalized)) {
                    // Modbus RTU或ASCII连接
                    if (!connectionString.contains("?")) {
                        // 添加基本的unit-identifier参数与请求超时
                        connectionString = String.format("%s?unit-identifier=%d&requestTimeout=2000", connectionString, modbusUnitId);
                    } else if (!connectionString.contains("unit-identifier")) {
                        connectionString = String.format("%s&unit-identifier=%d&requestTimeout=2000", connectionString, modbusUnitId);
                    } else if (!connectionString.contains("requestTimeout")) {
                        connectionString = String.format("%s&requestTimeout=2000", connectionString);
                    }
                }
            } else if ("eip".equals(normalized) || "plc4x-eip".equalsIgnoreCase(protocolType)) {
                // 创建EtherNet/IP连接
                if (!connectionString.contains("?")) {
                    connectionString = String.format("%s?backplane=%d&slot=%d", connectionString, eipBackplane, eipSlot);
                } else if (!connectionString.contains("backplane") || !connectionString.contains("slot")) {
                    connectionString = String.format("%s&backplane=%d&slot=%d", connectionString, eipBackplane, eipSlot);
                }
            }
            
            return connectionString;
        } else {
            return "";
        }
    }
} 