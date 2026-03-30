package cn.aitplus.wcs.adapters.io.opcua.transport;

import cn.aitplus.wcs.adapters.io.connection.ConnectionKey;
import cn.aitplus.wcs.adapters.io.opcua.config.OpcUaAdapterProperties;
import cn.aitplus.wcs.adapters.io.opcua.session.OpcUaClientRegistry;
import cn.aitplus.wcs.core.domain.enums.DomainEnums;
import cn.aitplus.wcs.core.spi.device.DeviceIoItem;
import cn.aitplus.wcs.core.spi.device.DeviceIoRequest;
import cn.aitplus.wcs.core.spi.device.DeviceIoResult;
import cn.aitplus.wcs.core.spi.device.DeviceTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OPC UA 同步 IO：通过 {@link OpcUaClientRegistry} 复用会话；与 {@link cn.aitplus.wcs.adapters.io.opcua.subscription.OpcUaSubscriptionService} 共享注册表。
 */
public class OpcUaDeviceTransport implements DeviceTransport, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(OpcUaDeviceTransport.class);

    private final OpcUaClientRegistry registry;
    private final ObjectMapper objectMapper;
    private final OpcUaAdapterProperties properties;

    public OpcUaDeviceTransport(OpcUaClientRegistry registry, ObjectMapper objectMapper, OpcUaAdapterProperties properties) {
        this.registry = registry;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    @Override
    public boolean supports(DomainEnums.CommandDomain domain) {
        return domain == DomainEnums.CommandDomain.OPC;
    }

    @Override
    public DeviceIoResult execute(DeviceIoRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return DeviceIoResult.fail("NO_ITEMS", "DeviceIoRequest.items is empty");
        }
        if (request.getEndpoint() == null || !StringUtils.hasText(request.getEndpoint().getOpcEndpointUrl())) {
            return DeviceIoResult.fail("INVALID_ENDPOINT", "endpoint.opcEndpointUrl required");
        }
        if (registry.isDestroyed()) {
            return DeviceIoResult.fail("TRANSPORT_DESTROYED", "OPC UA registry is destroyed");
        }
        if (request.getDomain() != null && request.getDomain() != DomainEnums.CommandDomain.OPC) {
            return DeviceIoResult.fail("INVALID_DOMAIN", "request.domain must be OPC for this transport");
        }

        boolean hasRead = false;
        boolean hasWrite = false;
        for (DeviceIoItem item : request.getItems()) {
            if (Boolean.TRUE.equals(item.getWrite())) {
                hasWrite = true;
            } else {
                hasRead = true;
            }
        }
        if (hasRead && !properties.isExecuteReadEnabled()) {
            return DeviceIoResult.fail("OPC_UA_READ_DISABLED", "wcs.adapter.opcua.execute-read-enabled=false");
        }
        if (hasWrite && !properties.isExecuteWriteEnabled()) {
            return DeviceIoResult.fail("OPC_UA_WRITE_DISABLED", "wcs.adapter.opcua.execute-write-enabled=false");
        }

        ConnectionKey key;
        try {
            key = ConnectionKey.from(
                request.getWarehouseId(),
                DomainEnums.CommandDomain.OPC,
                request.getEndpoint()
            );
        } catch (IllegalArgumentException e) {
            return DeviceIoResult.fail("INVALID_ENDPOINT", e.getMessage());
        }

        long timeoutMs = request.getTimeoutMillis() > 0
            ? request.getTimeoutMillis()
            : properties.getDefaultRequestTimeoutMillis();

        try {
            return registry.executeWithClient(key, request.getEndpoint(), timeoutMs,
                client -> runIo(client, request.getItems(), timeoutMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return DeviceIoResult.fail("INTERRUPTED", "OPC UA IO interrupted");
        } catch (Exception e) {
            LOG.warn("OPC UA IO failed warehouseId={} deviceId={}", request.getWarehouseId(), request.getDeviceId(), e);
            registry.teardownConnection(key);
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return DeviceIoResult.fail("OPC_UA_IO_ERROR", msg);
        }
    }

    private DeviceIoResult runIo(OpcUaClient client, List<DeviceIoItem> items, long timeoutMs) throws Exception {
        List<DeviceIoItem> reads = new ArrayList<>();
        List<DeviceIoItem> writes = new ArrayList<>();
        for (DeviceIoItem item : items) {
            if (!StringUtils.hasText(item.getAddress())) {
                return DeviceIoResult.fail("INVALID_ADDRESS", "item.address required (OPC UA NodeId string)");
            }
            if (Boolean.TRUE.equals(item.getWrite())) {
                writes.add(item);
            } else {
                reads.add(item);
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        int chunkSize = Math.max(1, properties.getMaxNodesPerServiceCall());

        if (!reads.isEmpty()) {
            List<Map<String, Object>> readList = new ArrayList<>(reads.size());
            for (List<DeviceIoItem> chunk : chunkItems(reads, chunkSize)) {
                List<ReadValueId> readIds = new ArrayList<>(chunk.size());
                for (DeviceIoItem item : chunk) {
                    NodeId nodeId = NodeId.parse(item.getAddress().trim());
                    readIds.add(new ReadValueId(nodeId, AttributeId.Value.uid(), null, null));
                }
                var readResponse = client.read(0.0, TimestampsToReturn.Both, readIds)
                    .get(timeoutMs, TimeUnit.MILLISECONDS);
                DataValue[] results = readResponse.getResults();
                if (results == null || results.length != chunk.size()) {
                    return DeviceIoResult.fail("OPC_UA_READ_ERROR", "Read response size mismatch");
                }
                for (int i = 0; i < chunk.size(); i++) {
                    DataValue dv = results[i];
                    if (dv.getStatusCode() == null || !dv.getStatusCode().isGood()) {
                        StatusCode sc = dv.getStatusCode();
                        return DeviceIoResult.fail("OPC_UA_BAD_STATUS",
                            chunk.get(i).getAddress() + ": " + (sc != null ? sc.toString() : "null"));
                    }
                    Variant v = dv.getValue();
                    Object val = v != null && v.isNotNull() ? v.getValue() : null;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("address", chunk.get(i).getAddress());
                    row.put("value", val);
                    readList.add(row);
                }
            }
            payload.put("reads", readList);
        }

        if (!writes.isEmpty()) {
            List<Map<String, Object>> writeList = new ArrayList<>(writes.size());
            for (List<DeviceIoItem> chunk : chunkItems(writes, chunkSize)) {
                List<WriteValue> writeValues = new ArrayList<>(chunk.size());
                for (DeviceIoItem w : chunk) {
                    NodeId nodeId = NodeId.parse(w.getAddress().trim());
                    Variant variant = new Variant(w.getValue());
                    DataValue dataValue = new DataValue(variant, null, null);
                    writeValues.add(new WriteValue(nodeId, AttributeId.Value.uid(), null, dataValue));
                }
                var writeResponse = client.write(writeValues).get(timeoutMs, TimeUnit.MILLISECONDS);
                StatusCode[] codes = writeResponse.getResults();
                if (codes == null || codes.length != chunk.size()) {
                    return DeviceIoResult.fail("OPC_UA_WRITE_ERROR", "Write response size mismatch");
                }
                for (int i = 0; i < codes.length; i++) {
                    if (!codes[i].isGood()) {
                        return DeviceIoResult.fail("OPC_UA_BAD_STATUS",
                            chunk.get(i).getAddress() + ": " + codes[i]);
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("address", chunk.get(i).getAddress());
                    row.put("ok", true);
                    writeList.add(row);
                }
            }
            payload.put("writes", writeList);
            payload.put("writeCount", writes.size());
        }

        return DeviceIoResult.ok(objectMapper.writeValueAsString(payload));
    }

    private static <T> List<List<T>> chunkItems(List<T> items, int chunkSize) {
        List<List<T>> chunks = new ArrayList<>();
        for (int i = 0; i < items.size(); i += chunkSize) {
            chunks.add(items.subList(i, Math.min(i + chunkSize, items.size())));
        }
        return chunks;
    }
}
