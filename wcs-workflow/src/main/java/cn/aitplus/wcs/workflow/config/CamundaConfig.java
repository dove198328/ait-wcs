package cn.aitplus.wcs.workflow.config;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.spring.boot.starter.event.PostDeployEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Camunda 嵌入式引擎的 Java 侧调优与启动后自检（不替代 {@code application.yml} 中的 {@code camunda.bpm}）。
 * <p>
 * 历史清理、历史级别等以 {@code camunda.bpm.generic-properties} 为准，避免在此处重复设置导致与 YAML 打架。
 */
@Configuration
public class CamundaConfig {

    private static final Logger log = LoggerFactory.getLogger(CamundaConfig.class);

    private final ObjectProvider<ProcessEngine> processEngineProvider;
    private final ObjectProvider<RuntimeService> runtimeServiceProvider;

    public CamundaConfig(
            ObjectProvider<ProcessEngine> processEngineProvider,
            ObjectProvider<RuntimeService> runtimeServiceProvider) {
        this.processEngineProvider = processEngineProvider;
        this.runtimeServiceProvider = runtimeServiceProvider;
    }

    /**
     * 引擎与 Spring 上下文就绪后打一条观测日志（流程定义数、活动实例数等），便于联调与运维确认。
     * 不包含“自动恢复流程”等业务逻辑；若需要应在明确需求与文档后单独实现。
     */
    @EventListener
    public void onPostDeploy(PostDeployEvent event) {
        ProcessEngine processEngine = event != null ? event.getProcessEngine() : null;
        if (processEngine == null) {
            processEngine = processEngineProvider.getIfAvailable();
        }
        if (processEngine == null) {
            log.warn("PostDeployEvent 后仍无法取得 ProcessEngine，跳过启动自检");
            return;
        }

        RuntimeService runtimeService = processEngine.getRuntimeService();
        if (runtimeService == null) {
            runtimeService = runtimeServiceProvider.getIfAvailable();
        }

        org.camunda.bpm.engine.ProcessEngineConfiguration pec = processEngine.getProcessEngineConfiguration();
        String history = pec != null ? pec.getHistory() : "?";
        String databaseType = "?";
        boolean jobExecutorOn = false;
        if (pec instanceof ProcessEngineConfigurationImpl) {
            ProcessEngineConfigurationImpl impl = (ProcessEngineConfigurationImpl) pec;
            databaseType = impl.getDatabaseType();
            jobExecutorOn = impl.isJobExecutorActivate();
        }

        log.info(
                "Camunda 引擎就绪: databaseType={}, jobExecutor={}, history={}",
                databaseType,
                jobExecutorOn ? "on" : "off",
                history);

        long definitionCount = processEngine.getRepositoryService().createProcessDefinitionQuery().count();
        long activeInstances = runtimeService != null
                ? runtimeService.createProcessInstanceQuery().active().count()
                : 0L;

        log.info("Camunda 自检: 流程定义数={}, 活动流程实例数={}", definitionCount, activeInstances);
    }

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



