package cn.aitplus.wcs.resolver;

import java.util.Collection;
import java.util.Map;

/**
 * PointAddressResolver is responsible for translating logical PLC point identifiers (e.g. <code>XTXH</code>)
 * to their real PLC addresses (e.g. <code>DB4.DBW68</code>). All business code should rely on this interface
 * to obtain addresses so that the conversion logic is defined in exactly one place.
 */
public interface PointAddressResolver {

    /**
     * Resolve the physical PLC address for the given logical point of the specified device.
     *
     * @param deviceId    device identifier owning the point
     * @param logicalName logical point identifier (e.g. "XTXH")
     * @return physical PLC address (e.g. "DB4.DBW68") or {@code null} if not found
     */
    String resolve(String deviceId, String logicalName);

    /**
     * Resolve multiple logical point identifiers in one call. Implementations should try to perform
     * batched reads from the underlying datastore to minimise I/O overhead.
     *
     * @param deviceId     device identifier
     * @param logicalNames collection of logical point identifiers
     * @return map from logicalName -> physical address (missing entries will be absent or map to null)
     */
    Map<String, String> resolveBatch(String deviceId, Collection<String> logicalNames);

    /**
     * Invalidate any cached value for the given device & logicalName so that next call will refresh
     * from the authoritative data source.
     *
     * @param deviceId    device identifier
     * @param logicalName logical point identifier
     */
    default void invalidate(String deviceId, String logicalName) {
        // default no-op; implementations with caching should override
    }
    /**
     * Resolve multiple logical names with metadata (address + dataType).
     * Default implementation wraps resolveBatch result and leaves dataType null.
     */
    default Map<String, PointMeta> resolveBatchWithMeta(String deviceId, Collection<String> logicalNames){
        Map<String,String> base = resolveBatch(deviceId, logicalNames);
        Map<String,PointMeta> res = new java.util.HashMap<>();
        base.forEach((k,v)-> res.put(k,new PointMeta(v,null)));
        return res;
    }
} 