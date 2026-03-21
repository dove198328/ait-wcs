package cn.aitplus.wcs.common.exception;

/**
 * 自定义业务异常。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_CODE = 400;

    private final int code;

    public BusinessException(String message) {
        this(DEFAULT_CODE, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
