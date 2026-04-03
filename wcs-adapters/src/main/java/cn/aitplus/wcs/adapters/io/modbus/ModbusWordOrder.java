package cn.aitplus.wcs.adapters.io.modbus;

/**
 * Modbus 双字（32 位）在相邻两个 16 位寄存器中的排布。
 */
public enum ModbusWordOrder {

    WORD_SWAP,
    BIG_ENDIAN
}
