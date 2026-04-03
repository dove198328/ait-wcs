package cn.aitplus.wcs.adapters.io.modbus;

import cn.aitplus.wcs.adapters.io.connection.ConnectionKey;
import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceEndpoint;
import cn.aitplus.wcs.core.spi.device.DeviceIoItem;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.core.spi.device.DeviceTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.util.BitVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reuse long-lived Modbus TCP connections by {@link ConnectionKey}.
 */
public class ModbusTcpDeviceTransport implements DeviceTransport, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(ModbusTcpDeviceTransport.class);

    private final ModbusAdapterProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<ConnectionKey, ManagedConnection> pool = new ConcurrentHashMap<>();
    private volatile boolean destroyed;

    public ModbusTcpDeviceTransport(ModbusAdapterProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public boolean supports(DomainEnums.CommandDomain domain) {
        return domain == DomainEnums.CommandDomain.MODBUS;
    }

    @Override
    public DeviceIoResult execute(DeviceIoRequest request) {
        DeviceIoResult validation = validateRequest(request, false);
        if (validation != null) {
            return validation;
        }

        int unitId;
        try {
            unitId = resolveUnitId(request.getEndpoint());
        } catch (IllegalArgumentException e) {
            return DeviceIoResult.fail("INVALID_UNIT_ID", e.getMessage());
        }

        ConnectionKey key = null;
        ManagedConnection managed = null;
        try {
            key = ConnectionKey.from(
                    request.getWarehouseId(),
                    DomainEnums.CommandDomain.MODBUS,
                    request.getEndpoint());
            managed = pool.computeIfAbsent(key, k -> new ManagedConnection());
            retainManaged(managed);
            long timeoutMs = resolveTimeoutMillis(request);
            while (true) {
                ModbusTCPMaster master = ensureLiveConnection(key, managed, request.getEndpoint());
                synchronized (managed.lock) {
                    ensureNotDestroyed();
                    if (managed.master != master || master == null || !master.isConnected()) {
                        continue;
                    }
                    try {
                        applySocketTimeout(master, timeoutMs);
                        return runIo(master, request.getItems(), unitId);
                    } catch (Exception e) {
                        teardownConnection(key, managed, "Modbus business IO failed, reconnect on next request");
                        throw e;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DeviceIoResult.fail("INTERRUPTED", "Modbus IO interrupted");
        } catch (Exception e) {
            LOG.warn("Modbus IO failed warehouseId={} deviceId={}", request.getWarehouseId(), request.getDeviceId(), e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return DeviceIoResult.fail("MODBUS_IO_ERROR", msg);
        } finally {
            if (key != null && managed != null) {
                releaseManaged(key, managed);
            }
        }
    }

    @Override
    public DeviceIoResult executeWithNewConnection(DeviceIoRequest request) {
        if (request != null && request.getDomain() != null && request.getDomain() != DomainEnums.CommandDomain.MODBUS) {
            return DeviceIoResult.fail("INVALID_DOMAIN", "executeWithNewConnection only supports CommandDomain.MODBUS");
        }

        DeviceIoResult validation = validateRequest(request, true);
        if (validation != null) {
            return validation;
        }

        int unitId;
        try {
            unitId = resolveUnitId(request.getEndpoint());
        } catch (IllegalArgumentException e) {
            return DeviceIoResult.fail("INVALID_UNIT_ID", e.getMessage());
        }

        ModbusTCPMaster master = null;
        try {
            master = openFreshMaster(request.getEndpoint());
            applySocketTimeout(master, resolveTimeoutMillis(request));
            return runIo(master, request.getItems(), unitId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DeviceIoResult.fail("INTERRUPTED", "Modbus IO interrupted");
        } catch (Exception e) {
            LOG.warn("Modbus ephemeral IO failed warehouseId={} deviceId={}",
                    request.getWarehouseId(), request.getDeviceId(), e);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return DeviceIoResult.fail("MODBUS_IO_ERROR", msg);
        } finally {
            closeQuietly(master);
        }
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

    private DeviceIoResult validateRequest(DeviceIoRequest request, boolean allowDomainCheck) {
        if (request == null) {
            return DeviceIoResult.fail("INVALID_REQUEST", "DeviceIoRequest required");
        }
        if (allowDomainCheck && request.getDomain() != null && request.getDomain() != DomainEnums.CommandDomain.MODBUS) {
            return DeviceIoResult.fail("INVALID_DOMAIN", "request.domain must be MODBUS");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return DeviceIoResult.fail("NO_ITEMS", "DeviceIoRequest.items is empty");
        }
        if (request.getEndpoint() == null || !StringUtils.hasText(request.getEndpoint().getHost())) {
            return DeviceIoResult.fail("INVALID_ENDPOINT", "endpoint.host required");
        }
        if (destroyed) {
            return DeviceIoResult.fail("TRANSPORT_DESTROYED", "Modbus transport is destroyed");
        }
        return null;
    }

    private long resolveTimeoutMillis(DeviceIoRequest request) {
        return request.getTimeoutMillis() > 0
                ? request.getTimeoutMillis()
                : properties.getDefaultRequestTimeoutMillis();
    }

    private ModbusTCPMaster ensureLiveConnection(ConnectionKey key, ManagedConnection managed,
                                                 DeviceEndpoint endpoint) throws Exception {
        for (;;) {
            synchronized (managed.lock) {
                ensureNotDestroyed();
                ModbusTCPMaster master = managed.master;
                if (master != null) {
                    if (!master.isConnected()) {
                        teardownConnection(key, managed, "Connection is disconnected");
                    } else {
                        return master;
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
                ModbusTCPMaster fresh = null;
                try {
                    ensureNotDestroyed();
                    fresh = openFreshMaster(endpoint);
                    synchronized (managed.lock) {
                        ensureNotDestroyed();
                        closeQuietly(managed.master);
                        managed.master = fresh;
                        managed.connecting = false;
                        managed.lock.notifyAll();
                        return fresh;
                    }
                } catch (Exception ex) {
                    closeQuietly(fresh);
                    last = ex;
                    LOG.debug("Modbus connect attempt {}/{} failed", i + 1, maxTries, ex);
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
        throw new IllegalStateException("Modbus connect failed");
    }

    private static void applySocketTimeout(ModbusTCPMaster master, long timeoutMs) {
        int timeout = (int) Math.min(Integer.MAX_VALUE, Math.max(500L, timeoutMs));
        master.setTimeout(timeout);
    }

    private ModbusTCPMaster openFreshMaster(DeviceEndpoint endpoint) throws Exception {
        String host = endpoint.getHost().trim();
        int port = endpoint.getPort() > 0 ? endpoint.getPort() : 502;
        int timeout = (int) Math.min(Integer.MAX_VALUE, Math.max(500L, properties.getDefaultRequestTimeoutMillis()));
        ModbusTCPMaster master = new ModbusTCPMaster(host, port);
        master.setReconnecting(false);
        master.setTimeout(timeout);
        master.connect();
        return master;
    }

    private void teardownConnection(ConnectionKey key, ManagedConnection managed, String reason) {
        ModbusTCPMaster master = managed.master;
        managed.master = null;
        if (master != null) {
            LOG.info("Modbus connection teardown key={} reason={}", key, reason);
            closeQuietly(master);
        }
        tryEvictIdleManaged(key, managed);
    }

    private static void closeQuietly(ModbusTCPMaster master) {
        if (master == null) {
            return;
        }
        try {
            master.disconnect();
        } catch (Exception e) {
            LOG.warn("Close Modbus master failed", e);
        }
    }

    private DeviceIoResult runIo(ModbusTCPMaster master, List<DeviceIoItem> items, int unitId) throws Exception {
        List<DeviceIoItem> reads = new ArrayList<>();
        List<DeviceIoItem> writes = new ArrayList<>();
        for (DeviceIoItem item : items) {
            if (!StringUtils.hasText(item.getAddress())) {
                return DeviceIoResult.fail("INVALID_ADDRESS", "item.address required");
            }
            if (Boolean.TRUE.equals(item.getWrite())) {
                writes.add(item);
            } else {
                reads.add(item);
            }
        }

        Map<String, Object> readMap = new LinkedHashMap<>();
        for (DeviceIoItem read : reads) {
            String addr = read.getAddress().trim();
            ModbusParsedAddress parsed = ModbusParsedAddress.parse(addr, properties.getDefaultWordOrder());
            Object value = readOne(master, unitId, parsed);
            readMap.put(addr, value);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        if (!readMap.isEmpty()) {
            payload.put("reads", readMap);
        }
        if (!writes.isEmpty()) {
            for (DeviceIoItem write : writes) {
                writeOne(master, unitId, write, properties.getDefaultWordOrder());
            }
            payload.put("writes", writes.size());
        }
        return DeviceIoResult.ok(objectMapper.writeValueAsString(payload));
    }

    private static Object readOne(ModbusTCPMaster master, int unitId, ModbusParsedAddress parsed)
            throws ModbusException {
        return switch (parsed.getRegion()) {
            case CO -> {
                BitVector bv = master.readCoils(unitId, parsed.getOffset(), 1);
                yield bv.getBit(0);
            }
            case DI -> {
                BitVector bv = master.readInputDiscretes(unitId, parsed.getOffset(), 1);
                yield bv.getBit(0);
            }
            case IR -> decodeRegisters(master.readInputRegisters(unitId, parsed.getOffset(), parsed.registerCount()), parsed);
            case HR -> decodeRegisters(
                    toInputs(master.readMultipleRegisters(unitId, parsed.getOffset(), parsed.registerCount())),
                    parsed
            );
        };
    }

    private static InputRegister[] toInputs(Register[] registers) {
        if (registers == null) {
            return new InputRegister[0];
        }
        InputRegister[] result = new InputRegister[registers.length];
        System.arraycopy(registers, 0, result, 0, registers.length);
        return result;
    }

    private static Object decodeRegisters(InputRegister[] regs, ModbusParsedAddress parsed) {
        if (regs == null || regs.length < parsed.registerCount()) {
            return null;
        }
        return switch (parsed.getScalar()) {
            case I16 -> (int) regs[0].toShort();
            case U16 -> regs[0].toUnsignedShort();
            case I32 -> decodeInt32(regs[0], regs[1], parsed.getWordOrder());
            case U32 -> decodeInt32(regs[0], regs[1], parsed.getWordOrder()) & 0xffffffffL;
            case F32 -> Float.intBitsToFloat(decodeInt32(regs[0], regs[1], parsed.getWordOrder()));
            default -> regs[0].toUnsignedShort();
        };
    }

    private static int decodeInt32(InputRegister r0, InputRegister r1, ModbusWordOrder order) {
        int w0 = r0.toUnsignedShort() & 0xffff;
        int w1 = r1.toUnsignedShort() & 0xffff;
        return order == ModbusWordOrder.WORD_SWAP ? (w1 << 16) | w0 : (w0 << 16) | w1;
    }

    private static void writeOne(ModbusTCPMaster master, int unitId, DeviceIoItem item,
                                 ModbusWordOrder defaultWordOrder) throws ModbusException {
        ModbusParsedAddress parsed = ModbusParsedAddress.parse(item.getAddress().trim(), defaultWordOrder);
        Object value = item.getValue();
        if (value == null) {
            throw new ModbusException("Write value required for " + item.getAddress());
        }
        switch (parsed.getRegion()) {
            case CO -> master.writeCoil(unitId, parsed.getOffset(), toBoolean(value));
            case HR -> writeHolding(master, unitId, parsed, value);
            default -> throw new ModbusException("Write not supported for region " + parsed.getRegion());
        }
    }

    private static void writeHolding(ModbusTCPMaster master, int unitId, ModbusParsedAddress parsed, Object value)
            throws ModbusException {
        switch (parsed.getScalar()) {
            case I16, U16 -> master.writeSingleRegister(
                    unitId,
                    parsed.getOffset(),
                    new SimpleRegister(toUint16(value, parsed.getScalar()))
            );
            case I32, U32 -> {
                int bits = toIntBits(value, parsed.getScalar() == ModbusParsedAddress.Scalar.I32);
                master.writeMultipleRegisters(unitId, parsed.getOffset(), encodeInt32(bits, parsed.getWordOrder()));
            }
            case F32 -> {
                int bits = Float.floatToIntBits(toFloat(value));
                master.writeMultipleRegisters(unitId, parsed.getOffset(), encodeInt32(bits, parsed.getWordOrder()));
            }
            default -> throw new ModbusException("Unsupported write scalar for " + parsed);
        }
    }

    private static Register[] encodeInt32(int bits, ModbusWordOrder order) {
        int w0 = (bits >>> 16) & 0xffff;
        int w1 = bits & 0xffff;
        if (order == ModbusWordOrder.WORD_SWAP) {
            return new Register[]{new SimpleRegister(w1), new SimpleRegister(w0)};
        }
        return new Register[]{new SimpleRegister(w0), new SimpleRegister(w1)};
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static int toUint16(Object value, ModbusParsedAddress.Scalar scalar) {
        long n = parseIntegerValue(value, "16-bit write");
        if (scalar == ModbusParsedAddress.Scalar.I16 && (n < Short.MIN_VALUE || n > Short.MAX_VALUE)) {
            throw new IllegalArgumentException("Value out of int16 range: " + n);
        }
        if (scalar == ModbusParsedAddress.Scalar.U16 && (n < 0 || n > 65535)) {
            throw new IllegalArgumentException("Value out of uint16 range: " + n);
        }
        return (int) n & 0xffff;
    }

    private static int toIntBits(Object value, boolean signed) {
        long n = parseIntegerValue(value, signed ? "int32 write" : "uint32 write");
        if (signed) {
            if (n < Integer.MIN_VALUE || n > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Value out of int32 range: " + n);
            }
            return (int) n;
        }
        if (n < 0 || n > 0xffffffffL) {
            throw new IllegalArgumentException("Value out of uint32 range: " + n);
        }
        return (int) n;
    }

    private static long parseIntegerValue(Object value, String usage) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("Expected integer for " + usage);
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected integer for " + usage + ": " + value, e);
        }
    }

    private static float toFloat(Object value) {
        if (value instanceof Number n) {
            return n.floatValue();
        }
        return Float.parseFloat(String.valueOf(value));
    }

    private static int resolveUnitId(DeviceEndpoint endpoint) {
        Integer unitId = endpoint.getModbusUnitId();
        if (unitId == null) {
            return 1;
        }
        if (unitId < 0 || unitId > 255) {
            throw new IllegalArgumentException("endpoint.modbusUnitId must be between 0 and 255: " + unitId);
        }
        return unitId;
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
        if (managed.master == null && managed.borrowers == 0 && !managed.connecting) {
            pool.remove(key, managed);
        }
    }

    private void ensureNotDestroyed() {
        if (destroyed) {
            throw new IllegalStateException("Modbus transport is destroyed");
        }
    }

    private static final class ManagedConnection {
        private final Object lock = new Object();
        private ModbusTCPMaster master;
        private int borrowers;
        private boolean connecting;
    }
}
