package cn.aitplus.wcs.plc4x;

import org.springframework.data.redis.core.RedisTemplate;
import cn.aitplus.wcs.core.domain.enums.ProtocolType;
import cn.aitplus.wcs.core.domain.enums.device.DeviceConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.aitplus.wcs.common.constant.Constants.*;
/**
 * PLC配置工具类
 * 封装了从Redis获取和解析PLC配置的通用方法
 *
 * Redis存储格式:
 *
 * 1. device:config:{deviceId} 设备基本配置
 * {
 *     "deviceId": "16",
 *     "deviceName": "1号堆垛机",
 *     "protocolType": "plc4x-s7",
 *     "connectionString": "s7://192.168.1.192",
 *     "deviceType": "7",
 *     "action": null,
 *     "pointPath": null,
 *     "warehouseIds": "1",
 *     "protocolTypeString": "plc4x-s7"
 * }
 *
 * 2. device:points:config:{deviceId} 设备点位配置
 * {
 *     "deviceId": "16",
 *     "description": "堆垛机设备点位配置",
 *     "deviceType": "STACKER_CRANE",
 *     "pointsConfig": {
 *         "XTXH": {
 *             "pointId": "XTXH",
 *             "name": "心跳信号",
 *             "address": "DB4.DBW68",
 *             "dataType": "INT",
 *             "access": "READ_ONLY",
 *             "description": "心跳信号"
 *         },
 *         "WXTXH": {
 *             "pointId": "WXTXH",
 *             "name": "写入心跳信息",
 *             "address": "DB5.DBW52",
 *             "dataType": "INT",
 *             "access": "WRITE_ONLY",
 *             "description": "写入心跳信息"
 *         }
 *     }
 * }
 */
@Slf4j
public class PlcConfigUtils {

    // 本地缓存：deviceId -> pointsConfig（即 Redis key device:points:config:{deviceId} 下的 pointsConfig 节点）
    private static final Cache<String, Map<String, Object>> POINTS_CFG_CACHE =
            Caffeine.newBuilder()
                    .expireAfterWrite(600, java.util.concurrent.TimeUnit.SECONDS) // TTL 60s
                    .maximumSize(1000)
                    .build();

    /** 点位配置更新后可调用使其失效 */
    public static void invalidatePointsConfig(String deviceId) {
        if (deviceId != null) {
            POINTS_CFG_CACHE.invalidate(deviceId);
        }
    }

    /**
     * 从Redis获取设备配置
     * @param deviceId 设备ID
     * @param redisTemplate Redis模板
     * @param objectMapper JSON解析器
     * @return 设备配置对象，如果找不到则返回null
     */
    @SuppressWarnings("unchecked")
    public static DeviceConfig getDeviceConfig(String deviceId, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        try {
            String redisKey = DEVICE_CONFIG_KEY_PREFIX + deviceId;
            Object redisValue = redisTemplate.opsForValue().get(redisKey);

            if (redisValue == null) {
                log.error("未找到设备 {} 的基本配置", deviceId);
                return null;
            }

            if (redisValue instanceof DeviceConfig) {
                return (DeviceConfig) redisValue;
            } else if (redisValue instanceof String) {
                String configJson = (String) redisValue;
                if (configJson.isEmpty()) {
                    log.error("设备 {} 的基本配置为空", deviceId);
                    return null;
                }
                return objectMapper.readValue(configJson, DeviceConfig.class);
            } else {
                log.error("设备 {} 的基本配置类型异常: {}", deviceId, redisValue.getClass().getName());
                return null;
            }
        } catch (Exception e) {
            log.error("获取设备 {} 基本配置失败: {}", deviceId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取点位配置Map（pointsConfig，带本地缓存）
     * @param deviceId 设备ID
     * @param redisTemplate Redis模板
     * @param objectMapper JSON解析器
     * @return 点位配置Map，其中键是pointId，值是点位配置对象
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getPointsConfig(String deviceId, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        try {
            // 1) 先查本地缓存
            Map<String, Object> cached = POINTS_CFG_CACHE.getIfPresent(deviceId);
            if (cached != null) {
                return cached;
            }

            // 2) miss 再读 Redis
            String redisKey = DEVICE_POINTS_CONFIG_KEY_PREFIX + deviceId;
            Object redisValue = redisTemplate.opsForValue().get(redisKey);

            if (redisValue == null) {
                log.error("未找到设备 {} 的点位配置", deviceId);
                return Collections.emptyMap();
            }

            Map<String, Object> config;
            if (redisValue instanceof Map) {
                config = (Map<String, Object>) redisValue;
            } else if (redisValue instanceof String) {
                String configJson = (String) redisValue;
                if (configJson.isEmpty()) {
                    return Collections.emptyMap();
                }
                config = objectMapper.readValue(configJson, Map.class);
            } else {
                log.error("设备 {} 的点位配置类型异常: {}", deviceId, redisValue.getClass().getName());
                return Collections.emptyMap();
            }

            Map<String, Object> points = config.containsKey("pointsConfig")
                    ? (Map<String, Object>) config.get("pointsConfig")
                    : Collections.emptyMap();

            // 3) 回填本地缓存
            POINTS_CFG_CACHE.put(deviceId, points);
            return points;
        } catch (Exception e) {
            log.error("获取设备 {} 点位配置失败: {}", deviceId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取点位配置Map（pointsConfig，带本地缓存）
     * @param deviceId 设备ID
     * @param redisTemplate Redis模板
     * @param objectMapper JSON解析器
     * @return 点位配置Map，其中键是pointId，值是点位配置对象
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getFullPointsConfig(String deviceId, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        try {
            // 2) miss 再读 Redis
            String redisKey = DEVICE_POINTS_CONFIG_KEY_PREFIX + deviceId;
            Object redisValue = redisTemplate.opsForValue().get(redisKey);

            if (redisValue == null) {
                log.error("未找到设备 {} 的点位配置", deviceId);
                return Collections.emptyMap();
            }

            Map<String, Object> config;
            if (redisValue instanceof Map) {
                config = (Map<String, Object>) redisValue;
            } else if (redisValue instanceof String) {
                String configJson = (String) redisValue;
                if (configJson.isEmpty()) {
                    return Collections.emptyMap();
                }
                config = objectMapper.readValue(configJson, Map.class);
            } else {
                log.error("设备 {} 的点位配置类型异常: {}", deviceId, redisValue.getClass().getName());
                return Collections.emptyMap();
            }

            return config;
        } catch (Exception e) {
            log.error("获取设备 {} 点位配置失败: {}", deviceId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取多个点位的详细配置（按点位ID过滤后的Map）
     * @param pointIds 点位ID列表
     * @param deviceId 设备ID
     * @param redisTemplate Redis模板
     * @param objectMapper JSON解析器
     * @return 点位ID到详细配置的映射
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, Object>> getPointsConfigMap(List<String> pointIds, String deviceId, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        if (pointIds == null || pointIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> pointsConfig = getPointsConfig(deviceId, redisTemplate, objectMapper);
        if (pointsConfig.isEmpty()) {
            return Collections.emptyMap();
        }

        // 使用LinkedHashMap保持pointIds的顺序
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (String pointId : pointIds) {
            if (pointsConfig.containsKey(pointId)) {
                result.put(pointId, (Map<String, Object>) pointsConfig.get(pointId));
            } else if (pointsConfig.containsKey(pointId.toUpperCase())) {
                // 兼容上游把点位ID存成大写的情况
                result.put(pointId, (Map<String, Object>) pointsConfig.get(pointId.toUpperCase()));
            }
        }

        return result;
    }

    /**
     * 格式化PLC地址，根据不同协议添加前缀
     * @param protocolType 协议类型
     * @param address 地址
     * @param dataType 数据类型
     * @return 格式化后的地址
     */
    public static String formatPlcAddress(ProtocolType protocolType, String address, String dataType) {
        // ------- 1. 归一化 dataType --------
        if (dataType == null) {
            dataType = "INT"; // 默认类型
        }

        String normalized;

        // 支持写成 String[8] / string[20]
        Pattern arrayPattern = Pattern.compile("(?i)string\\s*\\[(\\d+)\\]");
        Matcher m = arrayPattern.matcher(dataType.trim());
        if (m.matches()) {
            // 转为 STRING(8)
            normalized = "STRING(" + m.group(1) + ")";
        } else {
            normalized = dataType.trim().toUpperCase(Locale.ROOT);
        }

        // ------- 2. 拼装地址 --------
        if (protocolType == ProtocolType.PLC4X_S7) {
            return address.contains(":") ? "%" + address : "%" + address + ":" + normalized;
        } else if (protocolType == ProtocolType.PLC4X_MODBUS_TCP ||
                protocolType == ProtocolType.PLC4X_MODBUS_RTU ||
                protocolType == ProtocolType.PLC4X_MODBUS_ASCII) {
            // Modbus格式: register-type:address:dataType?byteOrder=...&wordOrder=...
            // return address.contains(":") ? address + ":" + normalized : "holding-register:" + address + ":" + normalized;
            return address.contains(":") ? address + ":" + normalized : "holding-register:" + address;
        } else {
            // 其他协议
            return address + ":" + normalized;
        }
    }
}