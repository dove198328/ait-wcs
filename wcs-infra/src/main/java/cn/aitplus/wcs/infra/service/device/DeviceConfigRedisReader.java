package cn.aitplus.wcs.infra.service.device;

import cn.aitplus.wcs.common.constant.WcsConstants;
import cn.aitplus.wcs.core.domain.model.device.DeviceConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 从 Redis 读取设备基础配置。
 */
@Service
public class DeviceConfigRedisReader {

    private static final Logger log = LoggerFactory.getLogger(DeviceConfigRedisReader.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public DeviceConfigRedisReader(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<DeviceConfig> findByDeviceId(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return Optional.empty();
        }
        String redisKey = WcsConstants.DEVICE_CONFIG_KEY_PREFIX + deviceId.trim();
        String jsonText = stringRedisTemplate.opsForValue().get(redisKey);
        if (!StringUtils.hasText(jsonText)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(jsonText, DeviceConfig.class));
        } catch (JsonProcessingException ex) {
            log.warn("从 Redis 反序列化设备配置失败，key={}", redisKey, ex);
            return Optional.empty();
        }
    }
}
