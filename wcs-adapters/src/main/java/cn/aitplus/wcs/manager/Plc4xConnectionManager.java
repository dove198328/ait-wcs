package cn.aitplus.wcs.manager;

import cn.aitplus.wcs.core.domain.enums.ProtocolType;
import cn.aitplus.wcs.core.domain.enums.device.DeviceConfig;
import cn.aitplus.wcs.plc4x.Plc4xConnectionStringUtil;
import cn.aitplus.wcs.plc4x.PlcConfigUtils;
import cn.aitplus.wcs.resolver.PointAddressResolver;
import cn.aitplus.wcs.utils.PlcOperationLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.plc4x.java.DefaultPlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * PLC4X连接池管理器
 */
@Component
@Slf4j
public class Plc4xConnectionManager {
    /** connectionString -> 活跃连接 (同一 PLC 共享) */
    private final Map<String, PlcConnection> activeConnMap = new ConcurrentHashMap<>();

    /** 设备ID -> 当前连接串（方便检测设备配置变更） */
    private final Map<String, String> deviceConnectionMap = new ConcurrentHashMap<>();

    // 针对每个 connStr 的锁
    private final ConcurrentHashMap<String, Lock> lockMap = new ConcurrentHashMap<>();

//    @Value("${plc4x.drivers.modbus.unit-identifier:1}")
    private int modbusUnitId;
//    @Value("${plc4x.drivers.eip.backplane:1}")
    private int eipBackplane;
//    @Value("${plc4x.drivers.eip.slot:0}")
    private int eipSlot;

    @Autowired
    private PointAddressResolver addressResolver;

    private final PlcDriverManager plcDriverManager = new DefaultPlcDriverManager();

//    @Value("${plc4x.connect-timeout-ms}")
    private int connectTimeoutMs;

//    @Value("${plc4x.probe.timeout-ms:500}")
    private int portProbeTimeoutMs;

//    @Value("${plc4x.reconnect.initial-delay-ms:300}")
    private int reconnectInitialDelayMs;

//    @Value("${plc4x.reconnect.max-delay-ms:2000}")
    private int reconnectMaxDelayMs;

    private final ExecutorService connectExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "plc4x-connect");
        t.setDaemon(true);
        return t;
    });

    public Plc4xConnectionManager() {}

    public String buildConnectionString(String deviceId, DeviceConfig config) {
        return Plc4xConnectionStringUtil.buildConnectionString(
                 config,
                modbusUnitId, eipBackplane, eipSlot
        );
    }

    private Lock getLock(String connStr) {
        return lockMap.computeIfAbsent(connStr, k -> new ReentrantLock());
    }

    public PlcConnection getConnection(String deviceId, DeviceConfig config) {
        if (deviceId == null || config == null) return null;
        String connStr = buildConnectionString(deviceId, config);

        // 检测设备配置是否发生变化
        String prevStr = deviceConnectionMap.get(deviceId);
        if (prevStr != null && !prevStr.equals(connStr)) {
            // 连接串变化，关闭旧连接
            resetDeviceConnection(deviceId);
        }

        Lock lock = getLock(connStr);
        lock.lock();
        // 避免全局锁：按 connStr 分段锁
        try {
            PlcConnection conn = activeConnMap.get(connStr);
            boolean valid = false;
            // 如果连接存在则尝试 ping 检测
            if (conn != null && conn.isConnected()) {
                try {
                    String pingDeviceId = getFirstDeviceForConnection(connStr);
                    if (pingDeviceId != null){
                        if (deviceId.equals(pingDeviceId)){
                            valid= this.customPing(conn, connStr, pingDeviceId);
                        }else {
                            valid = true;
                        }
                    }
                }  catch (Exception e) {
                    valid = false;
                    log.debug("Ping 异常，移除连接 connStr={}，原因: {}", connStr, e.getMessage());
                }
            }

            if (valid) {
                deviceConnectionMap.put(deviceId, connStr);
                return conn;
            }else{
                // 无效或不存在，移除旧连接
                if (conn != null) {
                    activeConnMap.remove(connStr);
                    closeConn(connStr, conn);
                }
                return createConnectionWithRetry(deviceId, connStr);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 外部手动重置连接：从缓存中移除并关闭
     */
    public boolean resetDeviceConnection(String deviceId){
        String connStr = deviceConnectionMap.get(deviceId);
        if(connStr == null) return false;

        // 使用原子操作
        if (deviceConnectionMap.remove(deviceId, connStr)) {
            Lock lock = getLock(connStr);
            lock.lock();
            try {
                boolean inUse = deviceConnectionMap.values().stream().anyMatch(connStr::equals);
                if (!inUse) {
                    PlcConnection conn = activeConnMap.remove(connStr);
                    if (conn != null) {
                        closeConn(connStr, conn);
                        return true;  // 实际关闭了连接
                    }
                }
                return false;  // 没有关闭连接
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    /**
     * 定时清理失效连接（默认每分钟执行）
     */
    @Scheduled(fixedDelay = 60 * 1000)
    public void cleanStaleConnections() {
        List<String> staleList = new ArrayList<>();
        // 第一阶段：扫描收集
        activeConnMap.forEach((connStr, conn) -> {
            boolean remove = false;
            if (!conn.isConnected()) {
                remove = true;
            } else {
                try {
                    //这里要进行ping了，采用自定义的方法
                    String pingDeviceId = getFirstDeviceForConnection(connStr);
                    boolean ok = this.customPing(conn, connStr, pingDeviceId);
                    if (!ok) {
                        remove = true;
                    }
                } catch (Exception e) {
                    remove = true;
                }
            }
            if (remove) staleList.add(connStr);
        });

        // 第二阶段：安全删除
        for (String connStr : staleList) {
            Lock lock = getLock(connStr);
            lock.lock();
            try {
                PlcConnection conn = activeConnMap.remove(connStr);
                if (conn != null) {
                    closeConn(connStr, conn);
                }
                deviceConnectionMap.entrySet().removeIf(e -> connStr.equals(e.getValue()));
            } finally {
                lock.unlock();
            }
            log.debug("已清理失效连接 {}", connStr);
        }
        log.debug("清理后连接池大小={} entries", activeConnMap.size());
    }

    private void closeConn(String connStr, PlcConnection conn) {
        try { conn.close(); } catch (Exception ignore) {}
    }

    @PreDestroy
    private void shutdown() {
        try { connectExecutor.shutdownNow(); } catch (Exception ignore) {}
        activeConnMap.forEach((cs, conn) -> closeConn(cs, conn));
        activeConnMap.clear();
        deviceConnectionMap.clear();
        log.debug("Plc4xConnectionManager 已释放所有连接");
    }

    // 在 getConnection 方法中替换创建连接的部分（不重试，只尝试一次）
    private PlcConnection createConnectionWithRetry(String deviceId, String connStr) {
        // 端口就绪快速探测（S7/Modbus/EIP等），端口未开则跳过本轮创建，避免长超时
        HostPort hp = extractHostPort(connStr);
        if (hp != null && hp.port > 0) {
            if (!isTcpPortOpen(hp.host, hp.port, portProbeTimeoutMs)) {
                log.debug("端口未开放，跳过连接尝试 connStr={} host={} port={}", connStr, hp.host, hp.port);
                return null;
            }
        }

        PlcConnection newConn = null;
        try {
            // 为创建连接对象本身增加超时保护，避免在锁内长时间阻塞
            Future<PlcConnection> createFuture = connectExecutor.submit(() ->
                    plcDriverManager.getConnectionManager().getConnection(connStr)
            );
            try {
                newConn = createFuture.get(connectTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                createFuture.cancel(true);
                log.debug("设备 {} 连接创建超时 {}ms", deviceId, connectTimeoutMs);
                PlcOperationLogger.logConnectionTimeout(deviceId, "createConnection", connectTimeoutMs, connStr);
                return null;
            }
            // 将同步 connect 放到独立线程并设置超时，避免调度线程被长时间阻塞
            final PlcConnection connRef = newConn;
            Future<Boolean> connectFuture = connectExecutor.submit(() -> {
                if (!connRef.isConnected()) {
                    connRef.connect();
                }
                return connRef.isConnected();
            });

            boolean connected;
            try {
                connected = connectFuture.get(connectTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                connectFuture.cancel(true);
                log.debug("设备 {} 连接操作超时 {}ms", deviceId, connectTimeoutMs);
                PlcOperationLogger.logConnectionTimeout(deviceId, "connect", connectTimeoutMs, connStr);
                if (newConn != null) {
                    try { newConn.close(); } catch (Exception ignore) {}
                }
                return null;
            }

            if (connected && newConn.isConnected()) {
                activeConnMap.put(connStr, newConn);
                deviceConnectionMap.put(deviceId, connStr);
                return newConn;
            } else {
                if (newConn != null) {
                    try { newConn.close(); } catch (Exception ignore) {}
                }
                return null;
            }
        } catch (Exception ex) {
            log.debug("设备 {} 连接创建失败: {}", deviceId, ex.getMessage());
            PlcOperationLogger.logConnectionFailure(deviceId, "createConnection", ex.getMessage());
            if (newConn != null) {
                try { newConn.close();
                } catch (Exception ignore) {
                    log.debug("关闭连接 {}", ignore);
                }
            }
            return null;
        }
    }

    public int getModbusUnitId() {
        return modbusUnitId;
    }

    public int getEipBackplane() {
        return eipBackplane;
    }

    public int getEipSlot() {
        return eipSlot;
    }

    /**
     * 自定义ping方法，针对S7协议实现健康检查
     * @param conn PLC连接
     * @param connStr 连接字符串
     * @param deviceId 设备ID
     * @return 是否连接健康
     */
    private boolean customPing(PlcConnection conn, String connStr, String deviceId) throws Exception{
        if (conn == null || !conn.isConnected()) {
            return false;
        }

        // 只对S7协议使用自定义ping，其他协议使用默认ping
        if (connStr.contains("s7")) {
            return pingS7(conn, deviceId);
        }
        if (connStr.contains("modbus")){
            return pingModbus(conn, deviceId);
        }
        return pingDefault(conn);
    }

    private boolean pingModbus(PlcConnection conn, String deviceId) {
        try {
            PlcReadRequest.Builder builder = conn.readRequestBuilder();
            // 显式指定数据类型，避免解析阻塞；地址应存在但不依赖具体业务点位
            builder.addTagAddress("ping", "holding-register:1:INT");
            PlcReadRequest request = builder.build();
            request.execute().get(connectTimeoutMs, TimeUnit.SECONDS);
            return true;
        } catch (TimeoutException te) {
            log.debug("Modbus ping 超时: {}", te.getMessage());
            PlcOperationLogger.logPingTimeout(deviceId, "Modbus", 5);
            return false;
        } catch (Exception ex) {
            log.debug("Modbus ping 失败: {}", ex.getMessage());
            PlcOperationLogger.logPingFailure(deviceId, "Modbus", ex.getMessage());
            return false;
        }
    }

    /**
     * S7协议ping实现 - 读取sbzt地址作为健康检查
     */
    private boolean pingS7(PlcConnection conn, String deviceId) throws Exception {
        // 获取sbzt的真实地址
        String sbztAddress = addressResolver.resolve(deviceId, "SBZT");
        if (sbztAddress == null) {
            log.debug("设备 {} 未找到SBZT地址配置，使用默认ping", deviceId);
            return pingDefault(conn);
        }

        // 格式化地址
        String formattedAddress = PlcConfigUtils.formatPlcAddress(ProtocolType.PLC4X_S7, sbztAddress,null);

        // 读取sbzt地址
        PlcReadRequest.Builder builder = conn.readRequestBuilder();
        builder.addTagAddress("ping", formattedAddress);

        PlcReadRequest request = builder.build();
        try {
            PlcReadResponse plcReadResponse = request.execute().get(5, TimeUnit.SECONDS);
            return true;
        } catch (TimeoutException te) {
            PlcOperationLogger.logPingTimeout(deviceId, "S7", 5);
            throw te;
        } catch (Exception e) {
            PlcOperationLogger.logPingFailure(deviceId, "S7", e.getMessage());
            throw e;
        }
    }

    /**
     * 默认ping实现 - 使用PLC4X的ping方法
     */
    private boolean pingDefault(PlcConnection conn) {
        try {
            conn.ping().get(5, TimeUnit.SECONDS);
            return true;
        } catch (UnsupportedOperationException uoe) {
            // 不支持ping操作，认为连接有效
            return true;
        } catch (TimeoutException te) {
            log.debug("默认ping超时: {}", te.getMessage());
            return false;
        } catch (Exception e) {
            log.debug("默认ping失败: {}", e.getMessage());
            return false;
        }
    }

    private String getFirstDeviceForConnection(String connStr) {
        return deviceConnectionMap.entrySet().stream()
                .filter(entry -> connStr.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .filter(NumberUtils::isCreatable)
                .findFirst()
                .orElse(null);
    }

    private static class HostPort {
        final String host;
        final int port;
        HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    /**
     * 从 PLC4X 连接串中提取 host/port；无端口则按协议默认：S7=102，Modbus TCP=502，EIP=44818；未知返回 -1
     */
    private HostPort extractHostPort(String connStr) {
        if (connStr == null || connStr.isEmpty()) return null;
        try {
            String s = connStr;
            String scheme = null;
            int schemeIdx = s.indexOf("://");
            if (schemeIdx > 0) {
                scheme = s.substring(0, schemeIdx).toLowerCase();
                s = s.substring(schemeIdx + 3);
            }
            int qIdx = s.indexOf('?');
            if (qIdx > 0) s = s.substring(0, qIdx);
            int slashIdx = s.indexOf('/');
            if (slashIdx > 0) s = s.substring(0, slashIdx);

            String host = s;
            int port = -1;

            int colonIdx = s.indexOf(':');
            if (colonIdx > 0) {
                host = s.substring(0, colonIdx);
                String portPart = s.substring(colonIdx + 1);
                // 去掉可能的额外路径
                int nonDigit = -1;
                for (int i = 0; i < portPart.length(); i++) {
                    char c = portPart.charAt(i);
                    if (c < '0' || c > '9') { nonDigit = i; break; }
                }
                if (nonDigit >= 0) {
                    portPart = portPart.substring(0, nonDigit);
                }
                port = Integer.parseInt(portPart);
            } else {
                if (scheme != null) {
                    if (scheme.contains("s7")) port = 102;
                    else if (scheme.contains("modbus")) port = 502;
                    else if (scheme.contains("eip")) port = 44818;
                }
            }
            if (host == null || host.isEmpty()) return null;
            return new HostPort(host, port);
        } catch (Exception ignore) {
            return null;
        }
    }

    private boolean isTcpPortOpen(String host, int port, int timeoutMs) {
        if (host == null || host.isEmpty() || port <= 0) return true; // 不判定
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}