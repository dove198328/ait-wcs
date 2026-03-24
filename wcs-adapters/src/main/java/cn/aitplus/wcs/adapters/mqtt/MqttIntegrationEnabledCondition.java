package cn.aitplus.wcs.adapters.mqtt;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 与 {@code wcs-app} 中 {@code mqtt.enabled} 约定一致；为适配器模块避免依赖 spring-boot-autoconfigure。
 */
public class MqttIntegrationEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabled = context.getEnvironment().getProperty("mqtt.enabled", "true");
        return !"false".equalsIgnoreCase(enabled.trim());
    }
}
