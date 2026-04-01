package cn.aitplus.wcs.execution.device.io;

import cn.aitplus.wcs.core.domain.model.device.DevicePointDefinition;
import cn.aitplus.wcs.execution.device.io.s7.S7PointAddressConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class S7PointAddressConverterTest {

    private final S7PointAddressConverter converter = new S7PointAddressConverter();

    @Test
    void shouldConvertDbWordAddress() {
        String actual = converter.convert(DevicePointDefinition.builder()
            .pointId("CZFS")
            .address("DB4.DBW0")
            .dataType("INT")
            .build());

        assertEquals("%DB4.DBW0:INT", actual);
    }

    @Test
    void shouldConvertDbDwordAddress() {
        String actual = converter.convert(DevicePointDefinition.builder()
            .pointId("RWH1")
            .address("DB5.DBD0")
            .dataType("DInt")
            .build());

        assertEquals("%DB5.DBD0:DINT", actual);
    }

    @Test
    void shouldConvertStringAddress() {
        String actual = converter.convert(DevicePointDefinition.builder()
            .pointId("TMFK")
            .address("DB4.DBX56.0")
            .dataType("String[8]")
            .build());

        assertEquals("%DB4:56:STRING(8)", actual);
    }
}
