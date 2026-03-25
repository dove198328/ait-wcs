package cn.aitplus.wcs.workflow.config;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Camunda 嵌入式引擎的 Java 侧调优（不替代 {@code application.yml} 中的 {@code camunda.bpm}）。
 */
@Configuration
public class CamundaConfig {

    /**
     * 仅 BPMN：关闭 DMN/CMMN，减少不需要的引擎能力。
     */
    @Bean
    public ProcessEnginePlugin wcsCamundaEngineTuningPlugin() {
        return new WcsCamundaEngineTuningPlugin();
    }

    private static final class WcsCamundaEngineTuningPlugin implements ProcessEnginePlugin {

        private static final Logger pluginLog = LoggerFactory.getLogger(WcsCamundaEngineTuningPlugin.class);

        @Override
        public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
            processEngineConfiguration.setDmnEnabled(false);
            processEngineConfiguration.setCmmnEnabled(false);
            pluginLog.info("Camunda: 已关闭 DMN 与 CMMN（仅使用 BPMN）");
        }

        @Override
        public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
            // no-op
        }

        @Override
        public void postProcessEngineBuild(ProcessEngine processEngine) {
            // no-op
        }
    }
    //todo 流程向下写的时候，注意这里是不是要增加

    /*
     * BackoffStrategy / ExponentialBackoffStrategy 来自 camunda-external-task-client（远程拉取 External Task 使用）。
     * 当前 wcs-workflow 仅依赖嵌入式引擎 starter，未引入 camunda-bpm-spring-boot-starter-external-task-client，
     * 故不能在此定义该 Bean；若以后采用独立 REST Worker，请在对应模块加依赖后再配置。
     */
}
