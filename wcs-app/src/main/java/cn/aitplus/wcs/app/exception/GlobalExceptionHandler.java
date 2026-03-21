package cn.aitplus.wcs.app.exception;

import cn.aitplus.wcs.common.domain.AjaxResult;
import cn.aitplus.wcs.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理，统一返回 AjaxResult。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final int BAD_REQUEST = 400;
    private static final int INTERNAL_SERVER_ERROR = 500;

    @ExceptionHandler(BusinessException.class)
    public AjaxResult<Void> handleBusinessException(BusinessException ex) {
        return AjaxResult.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public AjaxResult<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        return AjaxResult.error(BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public AjaxResult<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        return AjaxResult.error(BAD_REQUEST, "缺少请求参数: " + ex.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public AjaxResult<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return AjaxResult.error(BAD_REQUEST, "请求体格式错误");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "请求参数校验失败";
        return AjaxResult.error(BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    public AjaxResult<Void> handleBindException(BindException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "请求参数绑定失败";
        return AjaxResult.error(BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public AjaxResult<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return AjaxResult.error(BAD_REQUEST, message.isEmpty() ? "请求参数校验失败" : message);
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult<Void> handleException(Exception ex) {
        log.error("系统异常", ex);
        return AjaxResult.error(INTERNAL_SERVER_ERROR, "系统异常，请联系管理员");
    }
}
