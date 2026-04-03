package cn.aitplus.wcs.adapters.io.modbus;

import org.springframework.util.StringUtils;

import java.util.Locale;

final class ModbusParsedAddress {

    enum Region {
        HR, IR, CO, DI
    }

    enum Scalar {
        BIT, I16, U16, I32, U32, F32
    }

    private final Region region;
    private final int offset;
    private final Scalar scalar;
    private final ModbusWordOrder wordOrder;

    private ModbusParsedAddress(Region region, int offset, Scalar scalar, ModbusWordOrder wordOrder) {
        this.region = region;
        this.offset = offset;
        this.scalar = scalar;
        this.wordOrder = wordOrder;
    }

    static ModbusParsedAddress parse(String address, ModbusWordOrder defaultWordOrder) {
        if (!StringUtils.hasText(address)) {
            throw new IllegalArgumentException("Modbus address empty");
        }
        String[] p = address.trim().split(":");
        if (p.length < 2) {
            throw new IllegalArgumentException("Modbus address 须为 表:偏移[:类型][:ws|be]，当前=" + address);
        }
        Region region = switch (p[0].trim().toLowerCase(Locale.ROOT)) {
            case "hr" -> Region.HR;
            case "ir" -> Region.IR;
            case "co" -> Region.CO;
            case "di" -> Region.DI;
            default -> throw new IllegalArgumentException("未知 Modbus 表前缀: " + p[0]);
        };
        int offset = Integer.parseInt(p[1].trim());
        if (offset < 0 || offset > 65535) {
            throw new IllegalArgumentException("Modbus 偏移非法: " + offset);
        }
        Scalar scalar = Scalar.U16;
        ModbusWordOrder wo = defaultWordOrder != null ? defaultWordOrder : ModbusWordOrder.WORD_SWAP;
        if (region == Region.CO || region == Region.DI) {
            scalar = Scalar.BIT;
        } else if (p.length >= 3) {
            scalar = switch (p[2].trim().toLowerCase(Locale.ROOT)) {
                case "i16" -> Scalar.I16;
                case "u16" -> Scalar.U16;
                case "i32" -> Scalar.I32;
                case "u32" -> Scalar.U32;
                case "f32" -> Scalar.F32;
                default -> throw new IllegalArgumentException("未知 Modbus 类型: " + p[2]);
            };
        }
        if (p.length >= 4) {
            wo = switch (p[3].trim().toLowerCase(Locale.ROOT)) {
                case "ws" -> ModbusWordOrder.WORD_SWAP;
                case "be" -> ModbusWordOrder.BIG_ENDIAN;
                default -> throw new IllegalArgumentException("字序仅支持 ws / be，当前=" + p[3]);
            };
        }
        return new ModbusParsedAddress(region, offset, scalar, wo);
    }

    int registerCount() {
        return switch (scalar) {
            case I32, U32, F32 -> 2;
            default -> 1;
        };
    }

    Region getRegion() {
        return region;
    }

    int getOffset() {
        return offset;
    }

    Scalar getScalar() {
        return scalar;
    }

    ModbusWordOrder getWordOrder() {
        return wordOrder;
    }
}
