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

import java.util.Collections;

/**
 * 监听设备状态事件，通过 MQTT 推送状态和告警通知。
 * 掉线时：状态 topic 与告警 topic 均推送；上线时：仅状态 topic。
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
            String topic = buildTopic(statusTopicPrefix, event.getWarehouseId());
            log.debug("MQTT 发送设备状态 topic={} deviceId={} online={} payload={}",
                    topic, event.getDeviceId(), event.isOnline(), json);
            mqttPublishService.sendToMqtt(topic, json);
            if (!event.isOnline()) {
                DeviceAlarmChangedEvent offlineAlarm = DeviceAlarmChangedEvent.builder()
                        .deviceId(event.getDeviceId())
                        .deviceName(event.getDeviceName())
                        .warehouseId(event.getWarehouseId())
                        .protocolType(event.getProtocolType())
                        .alarm(true)
                        .alarmPointIds(Collections.emptySet())
                        .communicationOffline(true)
                        .timestamp(event.getTimestamp())
                        .build();
                String alarmJson = objectMapper.writeValueAsString(offlineAlarm);
                String alarmTopic = buildTopic(alarmTopicPrefix, event.getWarehouseId());
                log.debug("MQTT 发送掉线告警(告警 topic) topic={} deviceId={} payload={}",
                        alarmTopic, event.getDeviceId(), alarmJson);
                mqttPublishService.sendToMqtt(alarmTopic, alarmJson);
            }
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
            String topic = buildTopic(alarmTopicPrefix, event.getWarehouseId());
            log.debug("MQTT 发送设备告警 topic={} deviceId={} alarmPointIds={} payload={}",
                    topic, event.getDeviceId(), event.getAlarmPointIds(), json);
            mqttPublishService.sendToMqtt(topic, json);
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
