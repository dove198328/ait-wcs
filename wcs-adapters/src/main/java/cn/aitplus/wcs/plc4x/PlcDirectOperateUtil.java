package cn.aitplus.wcs.plc4x;

import cn.aitplus.wcs.core.domain.enums.ProtocolType;
import cn.aitplus.wcs.core.domain.enums.device.DeviceConfig;
import cn.aitplus.wcs.manager.Plc4xConnectionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.messages.PlcWriteRequest;
import org.apache.plc4x.java.api.messages.PlcWriteResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PLC4X 直接批量读写工具
 */
@Slf4j
public class PlcDirectOperateUtil {

    private PlcDirectOperateUtil(){}

    /**
     * 批量读取
     */
    @SuppressWarnings("unchecked")
    public static Map<String,Object> batchRead(String protocol,
                                               String connectionString,
                                               String pointsJson,
                                               List<String> pointIds,
                                               Plc4xConnectionManager connectionManager,
                                               ObjectMapper objectMapper){
        Map<String,Object> results = new HashMap<>();
        if(pointIds==null || pointIds.isEmpty()) return results;
        try{
            Map<String,Object> root = objectMapper.readValue(pointsJson, Map.class);
            Map<String,Object> pointsCfg = root.containsKey("pointsConfig") ?
                    (Map<String,Object>) root.get("pointsConfig") : root;

            DeviceConfig dc = new DeviceConfig();
            dc.setDeviceId("BATCH_OP_TMP");
            dc.setProtocolType(protocol);
            dc.setConnectionString(connectionString);

            ProtocolType protoType = ProtocolType.fromString(protocol);
            if(protoType==null){
                log.error("不支持的协议类型: {}",protocol);
                return results;
            }

            PlcConnection plc = connectionManager.getConnection(dc.getDeviceId(),dc);
            if(plc==null){
                log.error("PLC 连接失败");
                return results;
            }

            PlcReadRequest.Builder builder = plc.readRequestBuilder();
            for(String pid:pointIds){
                Object cfgObj = pointsCfg.get(pid.toUpperCase());
                if(!(cfgObj instanceof Map)) continue;
                Map<String,Object> cfg = (Map<String,Object>)cfgObj;
                String addr = String.valueOf(cfg.get("address"));
                String dataType = String.valueOf(cfg.get("dataType"));
                String fullAddr = PlcConfigUtils.formatPlcAddress(protoType,addr,dataType);
                builder.addTagAddress(pid,fullAddr);
            }
            PlcReadResponse resp = builder.build().execute().get(5, java.util.concurrent.TimeUnit.SECONDS);
            for(String pid:pointIds){
                if(resp.getResponseCode(pid)== PlcResponseCode.OK){
                    results.put(pid,resp.getObject(pid));
                }
            }
        }catch(Exception e){
            log.error("批量读取点位异常",e);
        }
        return results;
    }

    /**
     * 批量写入
     */
    @SuppressWarnings("unchecked")
    public static boolean batchWrite(String protocol,
                                     String connectionString,
                                     String pointsJson,
                                     Map<String,Object> values,
                                     Plc4xConnectionManager connectionManager,
                                     ObjectMapper objectMapper){
        if(values==null || values.isEmpty()) return false;
        try{
            Map<String,Object> root = objectMapper.readValue(pointsJson, Map.class);
            Map<String,Object> pointsCfg = root.containsKey("pointsConfig") ?
                    (Map<String,Object>) root.get("pointsConfig") : root;

            DeviceConfig dc = new DeviceConfig();
            dc.setDeviceId("BATCH_OP_TMP");
            dc.setProtocolType(protocol);
            dc.setConnectionString(connectionString);

            ProtocolType protoType = ProtocolType.fromString(protocol);
            if(protoType==null){
                log.error("不支持的协议类型: {}",protocol);
                return false;
            }
            PlcConnection plc = connectionManager.getConnection(dc.getDeviceId(),dc);
            if(plc==null){
                log.error("PLC 连接失败");
                return false;
            }
            PlcWriteRequest.Builder builder = plc.writeRequestBuilder();
            for(Map.Entry<String,Object> entry:values.entrySet()){
                String pid = entry.getKey();
                Object val = entry.getValue();
                Object cfgObj = pointsCfg.get(pid.toUpperCase());
                if(!(cfgObj instanceof Map)) continue;
                Map<String,Object> cfg = (Map<String,Object>)cfgObj;
                String addr = String.valueOf(cfg.get("address"));
                String dataType = String.valueOf(cfg.get("dataType"));
                String fullAddr = PlcConfigUtils.formatPlcAddress(protoType,addr,dataType);
                builder.addTagAddress(pid,fullAddr,val);
            }
            PlcWriteResponse resp = builder.build().execute().get(5, java.util.concurrent.TimeUnit.SECONDS);
            boolean allOk = true;
            for(String pid:values.keySet()){
                if(resp.getResponseCode(pid)!=PlcResponseCode.OK){
                    allOk=false;
                    log.error("写入点位 {} 失败，响应码 {}",pid,resp.getResponseCode(pid));
                }
            }
            return allOk;
        }catch(Exception e){
            log.error("批量写入点位异常",e);
            return false;
        }
    }
} 