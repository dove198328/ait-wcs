package cn.aitplus.wcs.infra.service.device;

import cn.aitplus.wcs.common.constant.WcsConstants;
import cn.aitplus.wcs.core.domain.model.device.DeviceConfig;
import cn.aitplus.wcs.core.domain.model.device.DevicePointsConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量扫描 Redis 中的设备配置与点位配置，供监控服务启动时构建索引。
 */
@Service
public class DeviceConfigRedisScanner {

    private static final Logger log = LoggerFactory.getLogger(DeviceConfigRedisScanner.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public DeviceConfigRedisScanner(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 以 Redis 键名 {@code device:config:{deviceId}} 的后缀为设备 ID，
     * 写回 {@link DeviceConfig#setDeviceId}；同一 deviceId 多次出现时保留先扫描到的项。
     */
    public List<DeviceConfig> findAllDeviceConfigs() {
        String pattern = WcsConstants.DEVICE_CONFIG_KEY_PREFIX + "*";
        Map<String, DeviceConfig> byId = new LinkedHashMap<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String deviceIdFromKey = deviceIdSuffix(key, WcsConstants.DEVICE_CONFIG_KEY_PREFIX);
                if (!StringUtils.hasText(deviceIdFromKey)) {
                    log.warn("设备配置 Redis 键无法解析设备 ID，已跳过 key={}", key);
                    continue;
                }
                String json = stringRedisTemplate.opsForValue().get(key);
                if (!StringUtils.hasText(json)) {
                    continue;
                }
                try {
                    DeviceConfig cfg = objectMapper.readValue(json, DeviceConfig.class);
                    cfg.setDeviceId(deviceIdFromKey);
                    if (byId.putIfAbsent(deviceIdFromKey, cfg) != null) {
                        log.warn("设备配置 Redis 键重复设备 ID，已忽略后出现的键 deviceId={} ignoredKey={}",
                                deviceIdFromKey, key);
                    }
                } catch (JsonProcessingException ex) {
                    log.warn("反序列化失败 key={} type=DeviceConfig", key, ex);
                }
            }
        }
        log.info("Redis SCAN {} 完成，共加载 {} 条（按键索引）", pattern, byId.size());
        return new ArrayList<>(byId.values());
    }

    /**
     * 以 Redis 键名 {@code device:points:config:{deviceId}} 的后缀为设备 ID 建索引，
     * 写回 {@link DevicePointsConfig#setDeviceId} 供下游使用。
     */
    public Map<String, DevicePointsConfig> findAllDevicePointsConfigs() {
        String pattern = WcsConstants.DEVICE_POINTS_CONFIG_KEY_PREFIX + "*";
        Map<String, DevicePointsConfig> map = new LinkedHashMap<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String deviceIdFromKey = deviceIdSuffix(key, WcsConstants.DEVICE_POINTS_CONFIG_KEY_PREFIX);
                if (!StringUtils.hasText(deviceIdFromKey)) {
                    log.warn("点位配置 Redis 键无法解析设备 ID，已跳过 key={}", key);
                    continue;
                }
                String json = stringRedisTemplate.opsForValue().get(key);
                if (!StringUtils.hasText(json)) {
                    continue;
                }
                try {
                    DevicePointsConfig pc = objectMapper.readValue(json, DevicePointsConfig.class);
                    pc.setDeviceId(deviceIdFromKey);
                    if (map.putIfAbsent(deviceIdFromKey, pc) != null) {
                        log.warn("点位配置 Redis 键重复设备 ID，已忽略后出现的键 deviceId={} ignoredKey={}",
                                deviceIdFromKey, key);
                    }
                } catch (JsonProcessingException ex) {
                    log.warn("反序列化失败 key={} type=DevicePointsConfig", key, ex);
                }
            }
        }
        log.info("Redis SCAN {} 完成，共加载 {} 条（按键索引）", pattern, map.size());
        return map;
    }

    private static String deviceIdSuffix(String redisKey, String keyPrefix) {
        if (!StringUtils.hasText(redisKey) || !StringUtils.hasText(keyPrefix) || !redisKey.startsWith(keyPrefix)) {
            return null;
        }
        String id = redisKey.substring(keyPrefix.length()).trim();
        return StringUtils.hasText(id) ? id : null;
    }
}
