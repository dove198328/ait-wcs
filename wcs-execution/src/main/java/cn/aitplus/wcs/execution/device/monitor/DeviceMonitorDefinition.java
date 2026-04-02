package cn.aitplus.wcs.execution.device.monitor;

import cn.aitplus.wcs.adapters.io.connection.ConnectionKey;
import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.domain.model.device.DevicePointsConfig;
import cn.aitplus.wcs.core.spi.device.DeviceEndpoint;
import cn.aitplus.wcs.execution.device.io.runtime.model.DeviceRuntimeProfile;
import cn.aitplus.wcs.execution.device.io.runtime.model.ResolvedDevicePoint;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单设备的监控定义。defaultPoints 不可变（启动时构建）；dynamicPoints 运行时变化，
 * 通过 volatile effectivePoints 快照保证 poll 线程无锁读取。
 */
@Getter
public class DeviceMonitorDefinition {

    private final DeviceRuntimeProfile runtimeProfile;
    private final ConnectionKey connectionKey;
    private final boolean pollingEnabled;

    /** 默认监控点（不可变） */
    private final Map<String, ResolvedDevicePoint> defaultPoints;

    /** 动态点引用计数：pointId → Set&lt;source&gt; */
    private final ConcurrentHashMap<String, Set<String>> dynamicPointSources = new ConcurrentHashMap<>();

    /** poll 线程读此快照（默认点 ∪ 动态点），无锁 */
    private volatile Map<String, ResolvedDevicePoint> effectivePoints;

    @Builder
    public DeviceMonitorDefinition(DeviceRuntimeProfile runtimeProfile,
                                   ConnectionKey connectionKey,
                                   boolean pollingEnabled,
                                   Map<String, ResolvedDevicePoint> defaultPoints) {
        this.runtimeProfile = runtimeProfile;
        this.connectionKey = connectionKey;
        this.pollingEnabled = pollingEnabled;
        this.defaultPoints = Collections.unmodifiableMap(
                defaultPoints != null ? defaultPoints : Collections.emptyMap());
        this.effectivePoints = this.defaultPoints;
    }

    /**
     * register/unregister 后调用，重算 effectivePoints 快照。
     *
     * @param resolvedDynamicPoints 当前所有动态点的已解析映射
     */
    public void refreshEffectivePoints(Map<String, ResolvedDevicePoint> resolvedDynamicPoints) {
        Map<String, ResolvedDevicePoint> merged = new HashMap<>(defaultPoints);
        if (resolvedDynamicPoints != null) {
            merged.putAll(resolvedDynamicPoints);
        }
        this.effectivePoints = Collections.unmodifiableMap(merged);
    }

    /** 当前是否有动态附加点。 */
    public boolean hasDynamicPoints() {
        return !dynamicPointSources.isEmpty();
    }

    public String getDeviceId() {
        return runtimeProfile.getDeviceConfig().getDeviceId();
    }

    public String getDeviceName() {
        return runtimeProfile.getDeviceConfig().getDeviceName();
    }

    public String getProtocolType() {
        return runtimeProfile.getDeviceConfig().getProtocolType();
    }

    public Long getWarehouseId() {
        return runtimeProfile.getWarehouseId();
    }

    public DomainEnums.CommandDomain getDomain() {
        return runtimeProfile.getDomain();
    }

    public DeviceEndpoint getEndpoint() {
        return runtimeProfile.getEndpoint();
    }

    public DevicePointsConfig getDevicePointsConfig() {
        return runtimeProfile.getDevicePointsConfig();
    }
}
