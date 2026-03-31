package cn.aitplus.wcs.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MQTT 配置：Broker 共用；WMS（B）与设备（A）入站分离。
 */
@Data
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

    /** 扁平读取 {@code mqtt.completion.timeout} */
    public int getCompletionTimeout() {
        return completion != null ? completion.getTimeout() : 30000;
    }

    public void setCompletionTimeout(int completionTimeout) {
        if (this.completion == null) {
            this.completion = new Completion();
        }
        this.completion.setTimeout(completionTimeout);
    }

    @Data
    public static class Completion {
        private int timeout = 30000;
    }
}
