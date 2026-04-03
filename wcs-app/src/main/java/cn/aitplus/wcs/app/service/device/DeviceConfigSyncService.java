package cn.aitplus.wcs.app.service.device;

import cn.aitplus.wcs.app.config.WmsProperties;
import cn.aitplus.wcs.app.util.DeviceListJson;
import cn.aitplus.wcs.common.constant.WcsConstants;
import cn.aitplus.wcs.core.domain.model.device.DeviceConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Pulls device list from WMS HTTP API and stores each device as JSON string under {@link WcsConstants#DEVICE_CONFIG_KEY_PREFIX}.
 */
@Service
public class DeviceConfigSyncService {

    private static final Logger log = LoggerFactory.getLogger(DeviceConfigSyncService.class);

    private final RestTemplate restTemplate;
    private final WmsProperties wmsProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public DeviceConfigSyncService(RestTemplate restTemplate,
                                   WmsProperties wmsProperties,
                                   StringRedisTemplate stringRedisTemplate,
                                   ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.wmsProperties = wmsProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * @return number of device entries written to Redis
     */
    public int syncFromWms() {
        String url = wmsProperties.resolvedSyncDeviceUrl();
        if (!StringUtils.hasText(url)) {
            throw new IllegalStateException("未配置 wms.api.sync_device 同步地址，启动同步无法执行");
        }
        log.info("【设备同步】开始请求 WMS 设备列表，URL：{}", url);
        String body;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            body = response.getBody();
            log.info("【设备同步】WMS 接口已响应，HTTP 状态：{}", response.getStatusCode());
        } catch (RestClientException ex) {
            log.error("【设备同步】调用 WMS 失败，URL：{}", url, ex);
            throw ex;
        }
        List<JsonNode> nodes;
        try {
            nodes = DeviceListJson.parseDeviceNodes(body == null ? "" : body, objectMapper);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("WMS 返回的设备列表不是合法 JSON，URL：" + url, e);
        }
        log.info("【设备同步】解析得到设备条目数：{}", nodes.size());
        int saved = 0;
        for (JsonNode node : nodes) {
            String deviceId = DeviceListJson.resolveDeviceId(node);
            if (!StringUtils.hasText(deviceId)) {
                log.warn("【设备同步】跳过一条记录（无法解析设备 ID）：{}", node);
                continue;
            }
            DeviceConfig config = objectMapper.convertValue(node, DeviceConfig.class);
            if (!StringUtils.hasText(config.getDeviceId())) {
                config.setDeviceId(deviceId);
            }
            String json;
            try {
                json = objectMapper.writeValueAsString(config);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("设备配置序列化失败，deviceId=" + deviceId, e);
            }
            stringRedisTemplate.opsForValue().set(WcsConstants.DEVICE_CONFIG_KEY_PREFIX + deviceId, json);
            saved++;
        }
        log.info("【设备同步】写入 Redis 完成，成功 {} 条，Redis 键前缀：{}", saved, WcsConstants.DEVICE_CONFIG_KEY_PREFIX);
        return saved;
    }
}
