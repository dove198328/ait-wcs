package cn.aitplus.wcs.common.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 通用常量信息
 */
public class Constants {
    /**
     * 认证请求头
     */
    public static final String AUTHORIZATION_HEADER_NAME = "Authorization";

    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * http请求
     */
    public static final String HTTP = "http://";

    /**
     * https请求
     */
    public static final String HTTPS = "https://";

    /**
     * 锁前缀,用于判断在两个dispatcher中是不是有任务
     */
    public static final String GUARD_KEY_PREFIX = "corr_guard:";

    /**
     * 设备分配前缀
     */
    public static final String DEVICE_ALLOCATION_PREFIX = "allocation:device:";

    /** Redis 设备配置前缀 */
    public static final String DEVICE_CONFIG_KEY_PREFIX = "device:config:";
    /** Redis 设备点位配置前缀 */
    public static final String DEVICE_POINTS_CONFIG_KEY_PREFIX = "device:points:config:";
    /** Redis 设备工艺/流程 key 格式 device:process:{warehouseId}: */
    public static final String DEVICE_PROCESS_KEY_FORMAT = "device:process:%s:";

    public static final String DEVICE_CAMUNDA_LOCK_KEY_PREFIX = "wcs:proc-camunda-lock:";

    /**
     * 堆垛机设备类型
     */
    public static final String  STORAGE_MACHINE_DEVICE_TYPE= "1";

    /**
     * 查询消息eventSQL
     */
    public static final String MESSAGE_QUERY_SQL = "SELECT distinct e.PROC_INST_ID_ " +
            "FROM ACT_RU_EXECUTION e " +
            "JOIN ACT_RU_EXECUTION pi " +
            "    ON pi.ID_ = e.PROC_INST_ID_  " +
            "JOIN ACT_RU_EVENT_SUBSCR evs " +
            "    ON evs.EXECUTION_ID_ = e.ID_  " +
            "WHERE e.SUSPENSION_STATE_ = 1 " +
            "  AND pi.SUSPENSION_STATE_ = 1" +
            "  AND evs.EVENT_TYPE_ = 'message' " +
            "  AND evs.EVENT_NAME_ IN ('DEVICE_VALUE_EVENT', 'DDJ_EVENT');";

    public static final String MESSAGE_QUERY_SQL_pg = "SELECT distinct e.PROC_INST_ID_ " +
            "FROM ACT_RU_EXECUTION e " +
            "JOIN ACT_RU_EXECUTION pi " +
            "    ON pi.ID_ = e.PROC_INST_ID_ " +
            "JOIN ACT_RU_EVENT_SUBSCR evs " +
            "    ON evs.EXECUTION_ID_ = e.ID_ " +
            "WHERE e.SUSPENSION_STATE_ = 1 " +
            "  AND pi.SUSPENSION_STATE_ = 1 " +
            "  AND evs.EVENT_TYPE_ = 'message' " +
            "  AND evs.EVENT_NAME_ IN ('DEVICE_VALUE_EVENT', 'DDJ_EVENT'); ";

    public static final String MESSAGE_QUERY_SQL_mysql = "SELECT distinct e.PROC_INST_ID_ " +
            "FROM ACT_RU_EXECUTION e " +
            "JOIN ACT_RU_EXECUTION pi " +
            "    ON pi.ID_ = e.PROC_INST_ID_ " +
            "JOIN ACT_RU_EVENT_SUBSCR evs " +
            "    ON evs.EXECUTION_ID_ = e.ID_ " +
            "WHERE e.SUSPENSION_STATE_ = 1 " +
            "  AND pi.SUSPENSION_STATE_ = 1 " +
            "  AND evs.EVENT_TYPE_ = 'message' " +
            "  AND evs.EVENT_NAME_ IN ('DEVICE_VALUE_EVENT', 'DDJ_EVENT'); ";

    public static final List<String> CLEAN_VAR_AUTO = Arrays.asList("pv_YXXF", "pv_CZFS",
            "pv_DDJSBZT","pv_ZXRWH1", "pv_ZXJD1", "pv_HCZT", "pv_FHRWH", "pv_RKRWQQ", "pv_CKDW", "pv_RKRWH", "pv_RKDW", "pv_KXZT", "pv_CKRWH");
    public static final List<String> CLEAN_VAR_SINGLE = Arrays.asList("pv_YXXF", "pv_CZFS",
            "pv_DDJSBZT","pv_ZXRWH1", "pv_ZXJD1", "pv_HCZT", "pv_FHRWH", "pv_RKRWQQ", "pv_RKRWH",  "pv_KXZT");
}
