# ait-wcs

## 设备点位配置说明

当前项目已经补齐“设备配置 + 点位配置 + adapter 调用”的主链路，模块职责如下：

- `wcs-core`
  - 放设备点位模型、点位读写结果模型
  - 放状态枚举约定接口 `DeviceStatusValueEnum`
- `wcs-infra`
  - 放 Redis 读取器
  - 负责读取 `device:config:{deviceId}` 和 `device:points:config:{deviceId}`
- `wcs-execution`
  - 放运行时装配、协议解析、S7 地址转换、结果映射
  - 统一通过 `DeviceIoFacade` 发起设备 IO

## 当前支持范围

当前这套点位扩展语义只对 `S7` 生效，包括：

- `dataType -> Java 类型` 转换
- `statusEnum` 状态转换
- `alarmEnabled + alarmCondition` 报警判断

对于 `modbus/http/https/rcs/opc`，当前先只返回 adapter 原始值，不套用 S7 的点位语义规则。

## 点位配置字段

### 必填字段

以下 5 个字段是必须的：

- `pointId`
- `name`
- `address`
- `dataType`
- `access`

如果缺少其中任意一个，执行期会直接判定该点位配置不合法，并抛出中文异常。

### 可选字段

以下字段都不是必须的：

- `description`
- `statusEnum`
- `alarmEnabled`
- `alarmCondition`

处理规则如下：

- 没有 `statusEnum`
  - 不做状态转换
- `alarmEnabled` 不是 `true`
  - 不做报警判断
- `alarmEnabled` 是 `true` 但没有 `alarmCondition`
  - 不报警

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

原因：

- `statusEnum` 属于领域约定，不应放在 `app`
- `execution` 需要读取它，但不应该拥有它
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

说明：

- 这是推荐的文档格式示例
- 当前代码还没有实现这类复杂规则解析时，不应直接使用
- 如果要启用，需要先补执行层解析逻辑

## 运行时处理顺序

当前设备点位读取的处理顺序如下：

1. 根据 `deviceId` 读取 `deviceConfig`
2. 根据 `deviceId` 读取 `pointsConfig`
3. 根据 `protocolType` 选择 adapter
4. 如果是 `S7`，先把点位地址转成 PLC4X 地址
5. 调用 adapter 读取原始值
6. 如果是 `S7`，按 `dataType` 转成 Java 类型
7. 如果配置了 `statusEnum`，再做状态转换
8. 如果开启了报警，再按 `alarmCondition` 判断是否报警
