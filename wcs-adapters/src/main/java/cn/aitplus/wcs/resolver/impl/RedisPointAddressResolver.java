package cn.aitplus.wcs.resolver.impl;

import cn.aitplus.wcs.plc4x.PlcConfigUtils;
import cn.aitplus.wcs.resolver.PointAddressResolver;
import cn.aitplus.wcs.resolver.PointMeta;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link PointAddressResolver} which retrieves point configurations from Redis
 * and caches the mapping locally via Caffeine for fast repeat look-ups.
 */
@Slf4j
@Component
public class RedisPointAddressResolver implements PointAddressResolver {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisPointAddressResolver(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // ------------------- PointAddressResolver ------------------- //

    @Override
    public String resolve(String deviceId, String logicalName) {
        if (deviceId == null || logicalName == null) {
            return null;
        }

        // 若调用方传入的本就是物理地址，则直接返回，避免多余日志 & Redis 查询
        if (isPhysicalAddress(logicalName)) {
            return logicalName; // no caching，使用原字符串即可
        }
        return loadAddress(deviceId, logicalName);
    }

    @Override
    public Map<String, String> resolveBatch(String deviceId, Collection<String> logicalNames) {
        if (deviceId == null || logicalNames == null || logicalNames.isEmpty()) {
            return Collections.emptyMap();
        }

        // 直接全部走 Redis 查询
        return loadAddresses(deviceId, logicalNames);
    }

    @Override
    public Map<String, PointMeta> resolveBatchWithMeta(String deviceId, Collection<String> logicalNames) {
        if(deviceId==null || logicalNames==null || logicalNames.isEmpty()){return Collections.emptyMap();}
        // load point config map once
        Map<String, Map<String,Object>> cfgMap =
                cn.aitplus.wcs.plc4x.PlcConfigUtils.getPointsConfigMap(new ArrayList<>(logicalNames), deviceId, redisTemplate, objectMapper);

        Map<String, PointMeta> result = new HashMap<>();
        logicalNames.forEach(name->{
            Map<String,Object> pc = cfgMap.get(name);
            if(pc!=null){
                Object addr = pc.get("address");
                Object dt = pc.get("dataType");
                if(addr!=null){
                    result.put(name,new PointMeta(addr.toString(), dt!=null?dt.toString():null));
                }
            }
        });
        return result;
    }

    @Override
    public void invalidate(String deviceId, String logicalName) {
        // 无本地缓存无需失效操作
        return;
    }

    // ------------------- internal helpers ------------------- //

    private String loadAddress(String deviceId, String logicalName) {
        try {
            List<String> list = Collections.singletonList(logicalName);
            Map<String, Map<String, Object>> map = PlcConfigUtils.getPointsConfigMap(list, deviceId, redisTemplate, objectMapper);
            if (map.containsKey(logicalName)) {
                Object addr = map.get(logicalName).get("address");
                if (addr != null) {
                    return addr.toString();
                }
            }
            log.debug("Cannot resolve address for deviceId={} logicalName={}", deviceId, logicalName);
            return null;
        } catch (Exception e) {
            log.error("Failed to load address for deviceId={} logicalName={} : {}", deviceId, logicalName, e.getMessage(), e);
            return null;
        }
    }

    private Map<String, String> loadAddresses(String deviceId, Collection<String> logicalNames) {
        try {
            Map<String, Map<String, Object>> bulk = PlcConfigUtils.getPointsConfigMap(new ArrayList<>(logicalNames), deviceId, redisTemplate, objectMapper);
            return bulk.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> Objects.toString(e.getValue().get("address"), null)));
        } catch (Exception e) {
            log.error("Failed to batch load addresses for deviceId={} : {}", deviceId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 判断给定字符串是否已经是物理 PLC 地址。
     * 粗略规则：包含点号（.）或冒号（:）即视为物理地址。
     */
    private boolean isPhysicalAddress(String name) {
        return name != null && (name.contains(".") || name.contains(":"));
    }
} 