package cn.aitplus.wcs.core.spi.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 设备 IO 统一结果，便于写入 {@code CommandExecution} 的 response/error 字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceIoResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;

    private String errorCode;

    private String errorMessage;

    /** JSON 等可审计摘要 */
    private String responseJson;

    public static DeviceIoResult ok(String responseJson) {
        return DeviceIoResult.builder()
            .success(true)
            .responseJson(responseJson)
            .build();
    }

    public static DeviceIoResult fail(String errorCode, String errorMessage) {
        return DeviceIoResult.builder()
            .success(false)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .build();
    }
}
