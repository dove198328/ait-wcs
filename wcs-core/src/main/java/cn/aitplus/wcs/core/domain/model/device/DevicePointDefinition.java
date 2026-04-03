package cn.aitplus.wcs.core.domain.model.device;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 设备点位定义。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DevicePointDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    private String pointId;
    private String name;
    private String address;
    private String dataType;
    private String access;
    private String description;
    private BigDecimal scale;

    private String statusEnum;

    private Boolean alarmEnabled;
    private String alarmCondition;

    /**
     * Modbus 32 位寄存器组合字序，覆盖全局 {@code wcs.adapter.modbus.default-word-order}。
     * {@code WORD_SWAP} / {@code BIG_ENDIAN}，为空则沿用全局默认。
     */
    private String modbusWordOrder;
}
