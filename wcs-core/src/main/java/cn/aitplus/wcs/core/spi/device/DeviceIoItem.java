package cn.aitplus.wcs.core.spi.device;

import cn.aitplus.wcs.core.domain.model.execution.Command;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 单次 IO 项：与 {@link Command} 类似，{@code address} 由适配器按协议解析（如 PLC4X 地址串）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceIoItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 协议侧地址或逻辑点（如 {@code %DB1.DBW0:INT}） */
    private String address;

    /** 写入值；读操作可为 null */
    private Object value;

    /** true=写，false=读；null 视为读 */
    private Boolean write;
}
