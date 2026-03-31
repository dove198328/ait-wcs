package cn.aitplus.wcs.app.bootstrap;

import cn.aitplus.wcs.app.service.device.DeviceConfigSyncService;
import cn.aitplus.wcs.app.service.device.DevicePointsConfigSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动后的初始化任务。
 */
@Component
public class WcsApplicationStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WcsApplicationStartupRunner.class);

    private final DeviceConfigSyncService deviceConfigSyncService;
    private final DevicePointsConfigSyncService devicePointsConfigSyncService;

    public WcsApplicationStartupRunner(DeviceConfigSyncService deviceConfigSyncService,
                                       DevicePointsConfigSyncService devicePointsConfigSyncService) {
        this.deviceConfigSyncService = deviceConfigSyncService;
        this.devicePointsConfigSyncService = devicePointsConfigSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        syncDeviceConfigsFromWms(deviceConfigSyncService);
        loadDevicePointsConfigs(devicePointsConfigSyncService);
    }

    private void syncDeviceConfigsFromWms(DeviceConfigSyncService deviceConfigSyncService) {
        log.info("【启动任务】开始执行 WMS 设备配置同步（写入 Redis）");
        int syncedCount = deviceConfigSyncService.syncFromWms();
        log.info("【启动任务】WMS 设备配置同步结束，共写入 Redis {} 条", syncedCount);
    }

    private void loadDevicePointsConfigs(DevicePointsConfigSyncService devicePointsConfigSyncService) {
        log.info("【启动任务】开始执行设备点位配置同步（写入 Redis）");
        int syncedCount = devicePointsConfigSyncService.loadDevicePointsConfigs();
        log.info("【启动任务】设备点位配置同步结束，共处理 {} 个文件", syncedCount);
    }
}
