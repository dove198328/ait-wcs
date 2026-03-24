package cn.aitplus.wcs.app.integration.mqtt;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@code mqtt.enabled} 非 false，且 {@code mqtt.wms-inbound-enabled=true} 时启用 WMS 入站示例。
 */
public class WmsMqttInboundExampleEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String mqttEnabled = context.getEnvironment().getProperty("mqtt.enabled", "true");
        if ("false".equalsIgnoreCase(mqttEnabled.trim())) {
            return false;
        }
        return context.getEnvironment().getProperty("mqtt.wms-inbound-enabled", Boolean.class, false);
    }
}
