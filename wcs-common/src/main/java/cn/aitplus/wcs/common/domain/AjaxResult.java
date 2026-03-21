package cn.aitplus.wcs.common.domain;

import cn.aitplus.wcs.common.domain.page.TableDataInfo;
import cn.aitplus.wcs.utils.AESUtil;
import cn.aitplus.wcs.utils.EncryptUtils;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.annotation.JsonView;

import java.io.Serializable;

/**
 * 操作消息提醒
 * 
 * @author  aitplus
 */
public class AjaxResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int HTTP_OK = 200;
    private static final int HTTP_ERROR = 500;

    public static final String DATA_TAG = "data";

    @JsonView(TableDataInfo.TableDataInfoView.class)
    private int code;

    @JsonView(TableDataInfo.TableDataInfoView.class)
    private String message;

    @JsonView(TableDataInfo.TableDataInfoView.class)
    private T data;

    @JsonView(TableDataInfo.TableDataInfoView.class)
    private String encryptData;

    /**
     * 初始化一个新创建的 AjaxResult 对象，使其表示一个空消息。
     */
    public AjaxResult() {
    }

    /**
     * 初始化一个新创建的 AjaxResult 对象
     *
     * @param code 状态码
     * @param message  返回内容
     */
    public AjaxResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 初始化一个新创建的 AjaxResult 对象
     *
     * @param code 状态码
     * @param message  返回内容
     * @param data 数据对象
     */
    public AjaxResult(int code, String message, T data) {
        this.code = code;
        this.message = message;

        if (data != null) {
            if (EncryptUtils.needEncrypt()) {
                this.encryptData = AESUtil.encrypt(JSONUtil.toJsonStr(data));
            } else {
                this.data = data;
            }
        }
    }


    /**
     * 返回成功消息
     *
     * @return 成功消息
     */
    public static <T>AjaxResult<T> success() {
        return AjaxResult.success("操作成功");
    }

    /**
     * 返回成功数据
     *
     * @return 成功消息
     */
    public static <T>AjaxResult<T> success(T data) {
        return AjaxResult.success("操作成功", data);
    }

    public static AjaxResult toAjax(int rows) {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 返回成功消息
     *
     * @param message 返回内容
     * @return 成功消息
     */
    public static <T>AjaxResult<T> success(String message) {
        return AjaxResult.success(message, null);
    }

    /**
     * 返回成功消息
     *
     * @param message  返回内容
     * @param data 数据对象
     * @return 成功消息
     */
    public static <T>AjaxResult<T> success(String message, T data) {
        return new AjaxResult<>(HTTP_OK, message, data);
    }

    /**
     * 返回错误消息
     *
     * @return
     */
    public static <T>AjaxResult<T> error() {
        return AjaxResult.error("操作失败");
    }

    /**
     * 返回错误消息
     *
     * @param message 返回内容
     * @return 警告消息
     */
    public static <T>AjaxResult<T> error(String message) {
        return AjaxResult.error(message, null);
    }

    /**
     * 返回错误消息
     *
     * @param message  返回内容
     * @param data 数据对象
     * @return 警告消息
     */
    public static <T>AjaxResult<T> error(String message, T data) {
        return new AjaxResult<>(HTTP_ERROR, message, data);
    }

    /**
     * 返回错误消息
     *
     * @param code 状态码
     * @param message  返回内容
     * @return 警告消息
     */
    public static <T>AjaxResult<T> error(int code, String message) {
        return new AjaxResult<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getEncryptData() {
        return encryptData;
    }

    public void setEncryptData(String encryptData) {
        this.encryptData = encryptData;
    }
}
