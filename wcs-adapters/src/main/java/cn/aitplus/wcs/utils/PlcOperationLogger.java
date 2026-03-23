package cn.aitplus.wcs.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PLC操作专用日志记录器
 * 用于记录连接超时、读取超时、写入超时和失败等关键操作日志
 * 这些日志会单独输出到 logs/plc-operation.log 文件
 */
public class PlcOperationLogger {
    
    private static final Logger logger = LoggerFactory.getLogger("PLC_OPERATION");

    private static String joinDeviceIds(Iterable<String> deviceIds) {
        if (deviceIds == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String deviceId : deviceIds) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(deviceId);
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private static Throwable rootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }
    
    /**
     * 记录连接超时
     */
    public static void logConnectionTimeout(String deviceId, String operation, long timeoutMs, String details) {
        logger.warn("[连接超时] deviceId={}, operation={}, timeout={}ms, details={}", 
                deviceId, operation, timeoutMs, details);
    }
    
    /**
     * 记录连接失败
     */
    public static void logConnectionFailure(String deviceId, String operation, String reason) {
        logger.error("[连接失败] deviceId={}, operation={}, reason={}", 
                deviceId, operation, reason);
    }
    
    /**
     * 记录读取超时
     */
    public static void logReadTimeout(String deviceId, String pointId, long timeoutMs, String details) {
        logger.warn("[读取超时] deviceId={}, pointId={}, timeout={}ms, details={}", 
                deviceId, pointId, timeoutMs, details);
    }
    
    /**
     * 记录读取失败
     */
    public static void logReadFailure(String deviceId, String pointId, String reason) {
        logger.error("[读取失败] deviceId={}, pointId={}, reason={}", 
                deviceId, pointId, reason);
    }

    /**
     * 按连接串汇总记录读取超时
     */
    public static void logBatchReadTimeout(String connectionString, Iterable<String> deviceIds,
                                           String pointId, long timeoutMs, String details) {
        logger.warn("[批量读取超时] conn={}, deviceIds={}, pointId={}, timeout={}ms, details={}",
                connectionString, joinDeviceIds(deviceIds), pointId, timeoutMs, details);
    }

    /**
     * 按连接串汇总记录读取失败
     */
    public static void logBatchReadFailure(String connectionString, Iterable<String> deviceIds,
                                           String pointId, String reason) {
        logger.error("[批量读取失败] conn={}, deviceIds={}, pointId={}, reason={}",
                connectionString, joinDeviceIds(deviceIds), pointId, reason);
    }

    /**
     * 按连接串汇总记录读取失败，并打印完整堆栈和根异常
     */
    public static void logBatchReadFailure(String connectionString, Iterable<String> deviceIds,
                                           String pointId, String reason, Throwable throwable) {
        Throwable root = rootCause(throwable);
        logger.error("[批量读取失败] conn={}, deviceIds={}, pointId={}, reason={}, rootType={}, rootMsg={}",
                connectionString,
                joinDeviceIds(deviceIds),
                pointId,
                reason,
                root == null ? "unknown" : root.getClass().getName(),
                root == null ? "null" : String.valueOf(root.getMessage()),
                throwable);
    }
    
    /**
     * 记录写入超时
     */
    public static void logWriteTimeout(String deviceId, String pointId, long timeoutMs, String details) {
        logger.warn("[写入超时] deviceId={}, pointId={}, timeout={}ms, details={}", 
                deviceId, pointId, timeoutMs, details);
    }
    
    /**
     * 记录写入失败
     */
    public static void logWriteFailure(String deviceId, String pointId, String reason) {
        logger.error("[写入失败] deviceId={}, pointId={}, reason={}", 
                deviceId, pointId, reason);
    }

    /**
     * 按连接串汇总记录写入超时
     */
    public static void logBatchWriteTimeout(String connectionString, Iterable<String> deviceIds,
                                            String pointId, long timeoutMs, String details) {
        logger.warn("[批量写入超时] conn={}, deviceIds={}, pointId={}, timeout={}ms, details={}",
                connectionString, joinDeviceIds(deviceIds), pointId, timeoutMs, details);
    }

    /**
     * 按连接串汇总记录写入失败
     */
    public static void logBatchWriteFailure(String connectionString, Iterable<String> deviceIds,
                                            String pointId, String reason) {
        logger.error("[批量写入失败] conn={}, deviceIds={}, pointId={}, reason={}",
                connectionString, joinDeviceIds(deviceIds), pointId, reason);
    }

    /**
     * 按连接串汇总记录写入失败，并打印完整堆栈和根异常
     */
    public static void logBatchWriteFailure(String connectionString, Iterable<String> deviceIds,
                                            String pointId, String reason, Throwable throwable) {
        Throwable root = rootCause(throwable);
        logger.error("[批量写入失败] conn={}, deviceIds={}, pointId={}, reason={}, rootType={}, rootMsg={}",
                connectionString,
                joinDeviceIds(deviceIds),
                pointId,
                reason,
                root == null ? "unknown" : root.getClass().getName(),
                root == null ? "null" : String.valueOf(root.getMessage()),
                throwable);
    }
    
    /**
     * 记录Ping超时
     */
    public static void logPingTimeout(String deviceId, String protocol, long timeoutMs) {
        logger.warn("[Ping超时] deviceId={}, protocol={}, timeout={}ms", 
                deviceId, protocol, timeoutMs);
    }
    
    /**
     * 记录Ping失败
     */
    public static void logPingFailure(String deviceId, String protocol, String reason) {
        logger.error("[Ping失败] deviceId={}, protocol={}, reason={}", 
                deviceId, protocol, reason);
    }
}

