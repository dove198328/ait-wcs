package cn.aitplus.wcs.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MQTT 配置：Broker 共用；WMS（B）与设备（A）入站分离。
 */
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    private String broker = "tcp://localhost:1883";
    private String username = "";
    private String password = "";
    private String clientId = "wcs-mqtt-client";
    /** 对应 YAML：mqtt.completion.timeout */
    private Completion completion = new Completion();
    private String publishDefaultTopic = "";
    private boolean wmsInboundEnabled = false;
    private String wmsInboundTopics = "";
    private boolean deviceInboundEnabled = true;
    private String deviceInboundTopics = "";

    public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getCompletionTimeout() {
        return completion.getTimeout();
    }

    public void setCompletionTimeout(int completionTimeout) {
        this.completion.setTimeout(completionTimeout);
    }

    public Completion getCompletion() {
        return completion;
    }

    public void setCompletion(Completion completion) {
        this.completion = completion;
    }

    public static class Completion {
        private int timeout = 30000;

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

    public String getPublishDefaultTopic() {
        return publishDefaultTopic;
    }

    public void setPublishDefaultTopic(String publishDefaultTopic) {
        this.publishDefaultTopic = publishDefaultTopic;
    }

    public boolean isWmsInboundEnabled() {
        return wmsInboundEnabled;
    }

    public void setWmsInboundEnabled(boolean wmsInboundEnabled) {
        this.wmsInboundEnabled = wmsInboundEnabled;
    }

    public String getWmsInboundTopics() {
        return wmsInboundTopics;
    }

    public void setWmsInboundTopics(String wmsInboundTopics) {
        this.wmsInboundTopics = wmsInboundTopics;
    }

    public boolean isDeviceInboundEnabled() {
        return deviceInboundEnabled;
    }

    public void setDeviceInboundEnabled(boolean deviceInboundEnabled) {
        this.deviceInboundEnabled = deviceInboundEnabled;
    }

    public String getDeviceInboundTopics() {
        return deviceInboundTopics;
    }

    public void setDeviceInboundTopics(String deviceInboundTopics) {
        this.deviceInboundTopics = deviceInboundTopics;
    }
}
