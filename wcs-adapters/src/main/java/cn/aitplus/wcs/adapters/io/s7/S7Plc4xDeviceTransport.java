package cn.aitplus.wcs.adapters.io.s7;

import cn.aitplus.wcs.adapters.io.connection.ConnectionKey;
import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceEndpoint;
import cn.aitplus.wcs.core.spi.device.DeviceIoItem;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.core.spi.device.DeviceTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcConnectionManager;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.messages.PlcWriteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Reuse long-lived connections by {@link ConnectionKey}. Borrow, probe and business IO
 * are serialized per key to avoid concurrent access to the same {@link PlcConnection}.
 */
public class S7Plc4xDeviceTransport implements DeviceTransport, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(S7Plc4xDeviceTransport.class);

    private final S7AdapterProperties properties;
    private final ObjectMapper objectMapper;
    private final PlcConnectionManager connectionManager;
    private final ConcurrentHashMap<ConnectionKey, ManagedConnection> pool = new ConcurrentHashMap<>();
    private volatile boolean destroyed;

    public S7Plc4xDeviceTransport(S7AdapterProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, PlcDriverManager.getDefault().getConnectionManager());
    }

    S7Plc4xDeviceTransport(S7AdapterProperties properties, ObjectMapper objectMapper,
                           PlcConnectionManager connectionManager) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.connectionManager = connectionManager;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public boolean supports(DomainEnums.CommandDomain domain) {
        return domain == DomainEnums.CommandDomain.S7;
    }

    @Override
    public DeviceIoResult execute(DeviceIoRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return DeviceIoResult.fail("NO_ITEMS", "DeviceIoRequest.items is empty");
        }
        if (request.getEndpoint() == null || !StringUtils.hasText(request.getEndpoint().getHost())) {
            return DeviceIoResult.fail("INVALID_ENDPOINT", "endpoint.host required");
        }
        if (destroyed) {
            return DeviceIoResult.fail("TRANSPORT_DESTROYED", "S7 transport is destroyed");
        }
        ConnectionKey key = null;
        ManagedConnection managed = null;
        try {
            key = ConnectionKey.from(
                request.getWarehouseId(),
                DomainEnums.CommandDomain.S7,
                request.getEndpoint()
            );
            managed = pool.computeIfAbsent(key, k -> new ManagedConnection());
            retainManaged(managed);
            long timeoutMs = request.getTimeoutMillis() > 0
                ? request.getTimeoutMillis()
                : properties.getDefaultRequestTimeoutMillis();
            while (true) {
                PlcConnection connection = ensureLiveConnection(key, managed, request.getEndpoint());
                synchronized (managed.lock) {
                    ensureNotDestroyed();
                    if (managed.connection != connection || connection == null || !connection.isConnected()) {
                        continue;
                    }
                    try {
                        return runIo(connection, request.getItems(), timeoutMs);
                    } catch (Exception e) {
                        teardownConnection(key, managed, "S7 business IO failed, reconnect on next request");
                        throw e;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DeviceIoResult.fail("INTERRUPTED", "S7 IO interrupted");
        } catch (Exception e) {
            LOG.warn("S7 IO failed warehouseId={} deviceId={}", request.getWarehouseId(), request.getDeviceId(), e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return DeviceIoResult.fail("S7_IO_ERROR", msg);
        } finally {
            if (key != null && managed != null) {
                releaseManaged(key, managed);
            }
        }
    }

    /**
     * Create a dedicated connection for the request and always close it in finally.
     */
    public DeviceIoResult executeWithNewConnection(DeviceIoRequest request) {
        if (request.getDomain() != null && request.getDomain() != DomainEnums.CommandDomain.S7) {
            return DeviceIoResult.fail("INVALID_DOMAIN", "executeWithNewConnection only supports CommandDomain.S7");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return DeviceIoResult.fail("NO_ITEMS", "DeviceIoRequest.items is empty");
        }
        if (request.getEndpoint() == null || !StringUtils.hasText(request.getEndpoint().getHost())) {
            return DeviceIoResult.fail("INVALID_ENDPOINT", "endpoint.host required");
        }
        if (destroyed) {
            return DeviceIoResult.fail("TRANSPORT_DESTROYED", "S7 transport is destroyed");
        }
        long timeoutMs = request.getTimeoutMillis() > 0
            ? request.getTimeoutMillis()
            : properties.getDefaultRequestTimeoutMillis();
        PlcConnection conn = null;
        try {
            String url = buildConnectionUrl(request.getEndpoint());
            conn = connectionManager.getConnection(url);
            conn.connect();
            return runIo(conn, request.getItems(), timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DeviceIoResult.fail("INTERRUPTED", "S7 IO interrupted");
        } catch (Exception e) {
            LOG.warn("S7 ephemeral IO failed warehouseId={} deviceId={}", request.getWarehouseId(), request.getDeviceId(), e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return DeviceIoResult.fail("S7_IO_ERROR", msg);
        } finally {
            closeQuietly(conn);
        }
    }

    private PlcConnection ensureLiveConnection(ConnectionKey key, ManagedConnection managed, DeviceEndpoint endpoint)
        throws Exception {
        for (;;) {
            synchronized (managed.lock) {
                ensureNotDestroyed();
                PlcConnection conn = managed.connection;
                if (conn != null) {
                    if (!conn.isConnected()) {
                        teardownConnection(key, managed, "Connection is disconnected");
                    } else if (properties.isBorrowProbeEnabled() && !probeConnectionHealthy(conn)) {
                        teardownConnection(key, managed, "Borrow probe failed");
                    } else {
                        return conn;
                    }
                }
                if (!managed.connecting) {
                    managed.connecting = true;
                    break;
                }
                while (managed.connecting) {
                    managed.lock.wait();
                    ensureNotDestroyed();
                }
            }
        }

        long delay = Math.max(1L, properties.getReconnectInitialDelayMillis());
        long maxDelay = Math.max(delay, properties.getReconnectMaxDelayMillis());
        int maxTries = Math.max(1, properties.getMaxReconnectAttempts());
        Exception last = null;
        try {
            for (int i = 0; i < maxTries; i++) {
                PlcConnection fresh = null;
                try {
                    ensureNotDestroyed();
                    String url = buildConnectionUrl(endpoint);
                    fresh = connectionManager.getConnection(url);
                    fresh.connect();
                    synchronized (managed.lock) {
                        ensureNotDestroyed();
                        managed.connection = fresh;
                        managed.connecting = false;
                        managed.lock.notifyAll();
                        return fresh;
                    }
                } catch (Exception ex) {
                    closeQuietly(fresh);
                    last = ex;
                    LOG.debug("S7 connect attempt {}/{} failed", i + 1, maxTries, ex);
                    if (i + 1 < maxTries) {
                        Thread.sleep(delay);
                        delay = Math.min(delay * 2, maxDelay);
                    }
                }
            }
        } finally {
            synchronized (managed.lock) {
                if (managed.connecting) {
                    managed.connecting = false;
                    managed.lock.notifyAll();
                    tryEvictIdleManaged(key, managed);
                }
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("S7 connect failed");
    }

    private boolean probeConnectionHealthy(PlcConnection conn) {
        long probeMs = properties.getBorrowProbeTimeoutMillis() > 0
            ? properties.getBorrowProbeTimeoutMillis()
            : properties.getDefaultRequestTimeoutMillis();
        probeMs = Math.max(500L, probeMs);
        try {
            conn.ping().get(probeMs, TimeUnit.MILLISECONDS);
            return true;
        } catch (Exception e) {
            Throwable root = e;
            if (e instanceof ExecutionException && e.getCause() != null) {
                root = e.getCause();
            }
            if (root instanceof UnsupportedOperationException) {
                return true;
            }
            LOG.debug("S7 ping probe failed", e);
            return false;
        }
    }

    private void teardownConnection(ConnectionKey key, ManagedConnection managed, String reason) {
        PlcConnection c = managed.connection;
        managed.connection = null;
        if (c != null) {
            LOG.info("S7 connection teardown key={} reason={}", key, reason);
            closeQuietly(c);
        }
        tryEvictIdleManaged(key, managed);
    }

    private static void closeQuietly(PlcConnection c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Exception e) {
            LOG.warn("Close S7 PLC connection failed", e);
        }
    }

    private DeviceIoResult runIo(PlcConnection connection, List<DeviceIoItem> items, long timeoutMs) throws Exception {
        List<DeviceIoItem> reads = new ArrayList<>();
        List<DeviceIoItem> writes = new ArrayList<>();
        for (DeviceIoItem item : items) {
            if (!StringUtils.hasText(item.getAddress())) {
                return DeviceIoResult.fail("INVALID_ADDRESS", "item.address required");
            }
            boolean isWrite = Boolean.TRUE.equals(item.getWrite());
            if (isWrite) {
                writes.add(item);
            } else {
                reads.add(item);
            }
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        if (!reads.isEmpty()) {
            PlcReadRequest.Builder rb = connection.readRequestBuilder();
            for (int i = 0; i < reads.size(); i++) {
                rb.addTagAddress("r" + i, reads.get(i).getAddress());
            }
            PlcReadResponse resp = rb.build().execute().get(timeoutMs, TimeUnit.MILLISECONDS);
            Map<String, Object> readMap = new LinkedHashMap<>();
            for (int i = 0; i < reads.size(); i++) {
                readMap.put(reads.get(i).getAddress(), resp.getObject("r" + i));
            }
            payload.put("reads", readMap);
        }
        if (!writes.isEmpty()) {
            PlcWriteRequest.Builder wb = connection.writeRequestBuilder();
            for (int i = 0; i < writes.size(); i++) {
                DeviceIoItem w = writes.get(i);
                wb.addTagAddress("w" + i, w.getAddress(), w.getValue());
            }
            wb.build().execute().get(timeoutMs, TimeUnit.MILLISECONDS);
            payload.put("writes", writes.size());
        }
        return DeviceIoResult.ok(objectMapper.writeValueAsString(payload));
    }

    private String buildConnectionUrl(DeviceEndpoint endpoint) {
        int port = endpoint.getPort() > 0 ? endpoint.getPort() : 102;
        int rack = endpoint.getRack() != null ? endpoint.getRack() : 0;
        int slot = endpoint.getSlot() != null ? endpoint.getSlot() : 1;
        return String.format(
            Locale.ROOT,
            "s7://%s:%d?remote-rack=%d&remote-slot=%d&field-optimization=true",
            endpoint.getHost().trim(),
            port,
            rack,
            slot
        );
    }

    public void destroy() {
        destroyed = true;
        for (Map.Entry<ConnectionKey, ManagedConnection> entry : pool.entrySet()) {
            ManagedConnection managed = entry.getValue();
            synchronized (managed.lock) {
                teardownConnection(entry.getKey(), managed, "Transport destroy");
            }
        }
        pool.clear();
    }

    int pooledConnectionCount() {
        return pool.size();
    }

    private void retainManaged(ManagedConnection managed) {
        synchronized (managed.lock) {
            managed.borrowers++;
        }
    }

    private void releaseManaged(ConnectionKey key, ManagedConnection managed) {
        synchronized (managed.lock) {
            if (managed.borrowers > 0) {
                managed.borrowers--;
            }
            tryEvictIdleManaged(key, managed);
        }
    }

    private void tryEvictIdleManaged(ConnectionKey key, ManagedConnection managed) {
        if (managed.connection == null && managed.borrowers == 0 && !managed.connecting) {
            pool.remove(key, managed);
        }
    }

    private void ensureNotDestroyed() {
        if (destroyed) {
            throw new IllegalStateException("S7 transport is destroyed");
        }
    }

    private static final class ManagedConnection {
        private final Object lock = new Object();
        private PlcConnection connection;
        private int borrowers;
        private boolean connecting;
    }
}
