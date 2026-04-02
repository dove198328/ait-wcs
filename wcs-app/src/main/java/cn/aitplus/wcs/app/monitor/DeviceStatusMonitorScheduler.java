package cn.aitplus.wcs.app.monitor;

import cn.aitplus.wcs.execution.device.monitor.DeviceStatusMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 监控调度器：驱动 {@link DeviceStatusMonitorService} 的初始化和周期轮询。
 */
@Component
public class DeviceStatusMonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeviceStatusMonitorScheduler.class);

    private final DeviceStatusMonitorService monitorService;
    private volatile boolean initialized;

    public DeviceStatusMonitorScheduler(DeviceStatusMonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            monitorService.initialize();
            initialized = true;
            log.info("设备状态监控初始化完成");
        } catch (Exception ex) {
            log.error("设备状态监控初始化失败", ex);
        }
    }

    @Scheduled(fixedDelayString = "${wcs.monitor.poll-interval-millis:2000}")
    public void scheduledPoll() {
        if (!initialized) {
            return;
        }
        try {
            monitorService.poll();
        } catch (Exception ex) {
            log.error("设备状态监控 poll 异常", ex);
        }
    }
}
