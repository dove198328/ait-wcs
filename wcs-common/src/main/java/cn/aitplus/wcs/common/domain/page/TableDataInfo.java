package cn.aitplus.wcs.common.domain.page;

import cn.aitplus.wcs.utils.AESUtil;
import cn.aitplus.wcs.utils.EncryptUtils;
import cn.hutool.json.JSONUtil;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * 表格分页数据对象
 * 
 * @author  aitplus
 */
public class TableDataInfo<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int HTTP_OK = 200;

    public interface TableDataInfoView {}

    private long total;

    private List<T> rows;

    private int code;

    private String msg;

    private String encryptRows;

    public TableDataInfo() {
        this.rows = Collections.emptyList();
    }

    /**
     * 分页
     * 
     * @param list 列表数据
     * @param total 总记录数
     */
    public TableDataInfo(List<T> list, long total)
    {
        this.rows = list;
        this.total = total;
    }

    public static <T> TableDataInfo<T> build(Object page) {
        TableDataInfo<T> rspData = new TableDataInfo<>();
        rspData.setCode(HTTP_OK);
        rspData.setMsg("查询成功");
        if (page != null) {
            try {
                Method recordsMethod = page.getClass().getMethod("getRecords");
                Method totalMethod = page.getClass().getMethod("getTotal");
                @SuppressWarnings("unchecked")
                List<T> records = (List<T>) recordsMethod.invoke(page);
                Object total = totalMethod.invoke(page);
                rspData.handleDataEncrypt(records);
                rspData.setTotal(total instanceof Number ? ((Number) total).longValue() : 0L);
            } catch (Exception e) {
                rspData.handleDataEncrypt(Collections.emptyList());
                rspData.setTotal(0L);
            }
        }
        return rspData;
    }

    public static <T> TableDataInfo<T> build(List<T> list) {
        TableDataInfo<T> rspData = new TableDataInfo<>();
        rspData.setCode(HTTP_OK);
        rspData.setMsg("查询成功");
        rspData.handleDataEncrypt(list);
        rspData.setTotal(list == null ? 0 : list.size());
        return rspData;
    }


    /**
     * 处理数据加密
     */
    private void handleDataEncrypt(List<T> data) {
        List<T> safeData = data == null ? Collections.emptyList() : data;
        if (EncryptUtils.needEncrypt()) {
            this.encryptRows = AESUtil.encrypt(JSONUtil.toJsonStr(safeData));
        } else {
            this.rows = safeData;
        }
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getEncryptRows() {
        return encryptRows;
    }

    public void setEncryptRows(String encryptRows) {
        this.encryptRows = encryptRows;
    }
}
