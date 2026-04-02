package cn.aitplus.wcs.app.integration.mqtt;

import cn.aitplus.wcs.core.domain.event.DeviceAlarmChangedEvent;
import cn.aitplus.wcs.core.domain.event.DeviceOnlineChangedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 监听设备状态事件，通过 MQTT 推送状态和告警通知。
 */
@Component
public class DeviceStatusMqttPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeviceStatusMqttPublisher.class);

    private final MqttPublishService mqttPublishService;
    private final ObjectMapper objectMapper;
    private final String statusTopicPrefix;
    private final String alarmTopicPrefix;

    public DeviceStatusMqttPublisher(
        MqttPublishService mqttPublishService,
        ObjectMapper objectMapper,
        @Value("${mqtt.sendTopic.status:wcs/device/status}") String statusTopicPrefix,
        @Value("${mqtt.sendTopic.alarm:wcs/device/alarm}") String alarmTopicPrefix) {
        this.mqttPublishService = mqttPublishService;
        this.objectMapper = objectMapper;
        this.statusTopicPrefix = statusTopicPrefix;
        this.alarmTopicPrefix = alarmTopicPrefix;
    }

    @EventListener
    public void onOnlineChanged(DeviceOnlineChangedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            mqttPublishService.sendToMqtt(buildTopic(statusTopicPrefix, event.getWarehouseId()), json);
        } catch (Exception ex) {
            log.warn("MQTT 推送设备状态失败，deviceId={}", event.getDeviceId(), ex);
        }
    }

    @EventListener
    public void onAlarmChanged(DeviceAlarmChangedEvent event) {
        if (!event.isAlarm()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(event);
            mqttPublishService.sendToMqtt(buildTopic(alarmTopicPrefix, event.getWarehouseId()), json);
        } catch (Exception ex) {
            log.warn("MQTT 推送设备告警失败，deviceId={}", event.getDeviceId(), ex);
        }
    }

    private String buildTopic(String topicPrefix, Long warehouseId) {
        if (warehouseId == null) {
            return topicPrefix;
        }
        String normalizedPrefix = StringUtils.trimTrailingCharacter(topicPrefix, '/');
        return normalizedPrefix + "/" + warehouseId;
    }
}
