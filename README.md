# ait-wcs

## 设备点位配置说明

当前项目已经打通“设备配置 + 点位配置 + adapter 调用”的主链路，模块职责如下：

- `wcs-core`
  - 放设备点位模型、点位读写结果模型
  - 放状态枚举约定接口 `DeviceStatusValueEnum`
- `wcs-infra`
  - 放 Redis 读取器
  - 负责读取 `device:config:{deviceId}` 和 `device:points:config:{deviceId}`
- `wcs-execution`
  - 放运行时装配、协议解析、地址转换、结果映射
  - 统一通过 `DeviceIoFacade` 发起设备 IO

## 当前支持范围

当前这套点位扩展语义中：

- `scale` 对数值型点位按倍率换算后返回，适用于各协议
- `dataType -> Java 类型` 转换当前只对 `S7` 生效
- `statusEnum` 状态转换当前只对 `S7` 生效
- `alarmEnabled + alarmCondition` 报警判断当前只对 `S7` 生效

对于 `modbus/http/https/rcs/opc`，当前不套用 S7 的状态/报警语义规则。

其中 `Modbus` 的 `dataType` 需要特别说明：

- 当 `address` 是纯数字时，`dataType` 用于推断 canonical 地址类型
- 当 `address` 已经是 canonical 形式时，真正决定 Modbus 读写标量类型的是 canonical 地址中的后缀，如 `i16/u16/i32/u32/f32`
- 因此 `dataType` 对 Modbus 不是唯一决定因素，但对于纯数字地址配置仍然很重要

## 点位配置字段

### 必填字段

以下字段是必填的：

- `pointId`
- `name`
- `address`
- `dataType`
- `access`

如果缺少其中任意一个，执行期会直接判定该点位配置不合法并抛出异常。

### 可选字段

以下字段都是可选的：

- `description`
- `scale`
- `statusEnum`
- `alarmEnabled`
- `alarmCondition`
- `modbusWordOrder`

处理规则如下：

- 没有 `description`
  - 仅缺少描述，不影响运行
- 没有 `scale`
  - 不做倍率换算，直接返回原始值
- 配了 `scale`
  - 仅对数值型结果生效
  - 返回值按 `原始值 × scale` 计算
  - 建议使用十进制值，如 `0.1`、`1`、`1000`
- 没有 `statusEnum`
  - 不做状态转换
- `alarmEnabled` 不是 `true`
  - 不做报警判断
- `alarmEnabled` 是 `true` 但没有 `alarmCondition`
  - 不报警
- 没有 `modbusWordOrder`
  - Modbus 32 位点位沿用全局 `wcs.adapter.modbus.default-word-order`
- 配了 `modbusWordOrder`
  - 仅对 Modbus 32 位类型有意义，如 `DINT`、`UDINT`、`REAL`
  - 当前仅支持 `WORD_SWAP` 和 `BIG_ENDIAN`
  - 如果 `address` 本身已经写了 canonical 后缀 `:ws` 或 `:be`，则以地址上的显式后缀为准

## 字段说明

### `scale`

`scale` 是通用点位语义，不是协议专属字段。

含义如下：

- 最终返回值 = 原始值 × `scale`
- `scale=1` 表示不放大不缩小
- `scale=0.1` 表示结果按 0.1 倍返回
- `scale=1000` 表示结果按 1000 倍返回

适用建议如下：

- 仪表原始值是整数，但业务想按小数展示时使用
- PLC/Modbus 返回的是缩放前原值时使用
- 需要统一单位换算时使用

### `modbusWordOrder`

`modbusWordOrder` 是 Modbus 专属字段，用于指定 32 位数据跨两个寄存器时的字顺序。

当前支持：

- `WORD_SWAP`
  - 低字在前，高字在后
- `BIG_ENDIAN`
  - 高字在前，低字在后

注意：

- 该字段只对 32 位类型有意义
- 对 16 位类型如 `INT`、`UINT`、`WORD` 没有实际作用
- 当前只处理字顺序，不处理寄存器内部字节交换

## 点位配置示例

### 最小配置示例

```json
{
  "pointId": "CKSSDW",
  "name": "出库输送到位",
  "address": "DB105.DBW10",
  "dataType": "INT",
  "access": "READ_ONLY"
}
```

### 含 `scale` 的示例

```json
{
  "pointId": "GROSS",
  "name": "毛重",
  "address": 1,
  "dataType": "INT",
  "scale": 0.1,
  "access": "READ_ONLY",
  "description": "毛重 = 原始整数 × 0.1kg"
}
```

### 含 `modbusWordOrder` 的 Modbus 示例

```json
{
  "pointId": "FLOW",
  "name": "瞬时流量",
  "address": 10,
  "dataType": "REAL",
  "modbusWordOrder": "BIG_ENDIAN",
  "access": "READ_ONLY"
}
```

### 完整配置示例

```json
{
  "pointId": "SBZT",
  "name": "设备状态",
  "address": "DB4.DBW2",
  "dataType": "INT",
  "access": "READ_ONLY",
  "description": "设备状态",
  "statusEnum": "StackerCraneStatusEnum",
  "alarmEnabled": true,
  "alarmCondition": "range:[1000,2000];exclude:1041,1087"
}
```

## S7 数据类型到 Java 类型的转换规则

当前 S7 点位读取结果会按 `dataType` 转为 Java 类型：

- `BOOL -> Boolean`
- `INT -> Integer`
- `UINT -> Integer`
- `WORD -> Integer`
- `BYTE -> Integer`
- `DINT -> Long`
- `DWORD -> Long`
- `REAL -> BigDecimal`
- `LREAL -> BigDecimal`
- `STRING[n] -> String`

如果不是 S7 点位，当前不做这层类型转换。

## 状态枚举说明

`statusEnum` 用来指定“设备原始值如何转换成系统状态”。

### 放置位置

状态枚举建议统一放在：

```text
wcs-core/src/main/java/cn/aitplus/wcs/core/domain/enums/device/
```

原因如下：

- `statusEnum` 属于领域约定，不应放在 `app`
- `execution` 需要读取它，但不应拥有它
- 放在 `core` 最容易被多模块复用

### 实现要求

状态枚举需要实现：

```java
cn.aitplus.wcs.core.domain.model.device.DeviceStatusValueEnum;
```

接口约定如下：

```java
public interface DeviceStatusValueEnum {
    String getCode();
    String getStatus();
}
```

含义如下：

- `getCode()`
  - 返回设备读到的原始值编码
- `getStatus()`
  - 返回系统内部统一状态值

### 枚举示例

下面是推荐写法示例，示例代码只用于说明：

```java
package cn.aitplus.wcs.core.domain.enums.device;

import cn.aitplus.wcs.core.domain.model.device.DeviceStatusValueEnum;

/**
 * 堆垛机状态枚举示例。
 */
public enum StackerCraneStatusEnum implements DeviceStatusValueEnum {

    工作("1", "WORKING"),
    待机("2", "IDLE"),
    报警("1001", "ALARM");

    private final String code;
    private final String status;

    StackerCraneStatusEnum(String code, String status) {
        this.code = code;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getStatus() {
        return status;
    }
}
```

对应点位配置可以这样写：

```json
{
  "pointId": "SBZT",
  "name": "设备状态",
  "address": "DB4.DBW2",
  "dataType": "INT",
  "access": "READ_ONLY",
  "statusEnum": "StackerCraneStatusEnum"
}
```

## 报警规则说明

当前报警规则只针对 S7 点位生效。

### 开关

只有当 `alarmEnabled=true` 时，系统才会判断报警。

### 推荐规则格式

`alarmCondition` 建议使用结构清晰的受控格式，不建议写任意表达式。

当前推荐格式如下：

```text
range:[最小值,最大值];exclude:排除值1,排除值2
```

含义如下：

- `range:[1000,2000]`
  - 表示读值在 `1000` 到 `2000` 之间视为报警
- `exclude:1041,1087`
  - 表示虽然落在报警区间内，但 `1041`、`1087` 不报警

### 报警规则示例

#### 示例一：固定值报警

```json
{
  "pointId": "SBZT",
  "name": "设备状态",
  "address": "DB4.DBW2",
  "dataType": "INT",
  "access": "READ_ONLY",
  "alarmEnabled": true,
  "alarmCondition": "1000,1001,1002,1003,1043"
}
```

表示读到这些值中的任意一个即报警。

#### 示例二：区间报警

```json
{
  "pointId": "SBZT",
  "name": "设备状态",
  "address": "DB4.DBW2",
  "dataType": "INT",
  "access": "READ_ONLY",
  "alarmEnabled": true,
  "alarmCondition": "range:[1000,2000]"
}
```

表示读值在 `1000` 到 `2000` 之间即报警。

#### 示例三：区间报警但排除特殊值

```json
{
  "pointId": "SBZT",
  "name": "设备状态",
  "address": "DB4.DBW2",
  "dataType": "INT",
  "access": "READ_ONLY",
  "alarmEnabled": true,
  "alarmCondition": "range:[1000,2000];exclude:1041,1087"
}
```

表示：

- 大于等于 `1000`
- 且小于等于 `2000`
- 但 `1041` 和 `1087` 不报警

#### 示例四：更复杂的多段规则示例

如果后续有需要，建议继续沿用受控格式扩展，例如：

```text
range:[1000,2000];exclude:1041,1087|range:[3000,3999]
```

含义是：

- `1000` 到 `2000` 报警，但排除 `1041`、`1087`
- 或者 `3000` 到 `3999` 也报警

说明如下：

- 这是推荐的文档格式示例
- 当前代码如果还没有实现这类复杂规则解析，不应直接使用
- 如需启用，需要先补执行层解析逻辑

## 运行时处理顺序

当前设备点位读取的处理顺序如下：

1. 根据 `deviceId` 读取 `deviceConfig`
2. 根据 `deviceId` 读取 `pointsConfig`
3. 根据 `protocolType` 选择 adapter
4. 如果是 `S7`，先把点位地址转成 PLC4X 地址
5. 调用 adapter 读取原始值
6. 如果协议需要类型转换，先按协议规则转成运行时值
7. 如果配置了 `scale`，对数值结果做倍率换算
8. 如果配置了 `statusEnum`，再做状态转换
9. 如果开启了报警，再按 `alarmCondition` 判断是否报警
