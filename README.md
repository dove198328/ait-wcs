# ait-wcs

## Device Point Configuration

The project has wired up the main flow of **device configuration + point configuration + adapter invocation**. Module responsibilities are as follows:

- `wcs-core`
  - Device point models and point read/write result models
  - Status enum contract interface `DeviceStatusValueEnum`
- `wcs-infra`
  - Redis readers
  - Reads `device:config:{deviceId}` and `device:points:config:{deviceId}`
- `wcs-execution`
  - Runtime assembly, protocol parsing, address conversion, and result mapping
  - All device I/O goes through `DeviceIoFacade`

## Current Support Scope

Within the current point extension semantics:

- `scale` — applies a multiplier to numeric points and returns the scaled value; supported across protocols
- `dataType -> Java type` conversion — currently **S7 only**
- `statusEnum` status mapping — currently **S7 only**
- `alarmEnabled + alarmCondition` alarm evaluation — currently **S7 only**

For `modbus`, `http`, `https`, `rcs`, and `opc`, S7-specific status/alarm semantics are **not** applied.

**Modbus `dataType` notes:**

- When `address` is a plain number, `dataType` is used to infer the canonical address type
- When `address` is already in canonical form, the scalar read/write type is determined by the suffix in the canonical address (e.g. `i16`, `u16`, `i32`, `u32`, `f32`)
- Therefore `dataType` is not the sole determinant for Modbus, but it remains important for plain numeric addresses

## Point Configuration Fields

### Required Fields

The following fields are **required**:

- `pointId`
- `name`
- `address`
- `dataType`
- `access`

If any of these is missing, the runtime treats the point configuration as invalid and throws an exception.

### Optional Fields

The following fields are **optional**:

- `description`
- `scale`
- `statusEnum`
- `alarmEnabled`
- `alarmCondition`
- `modbusWordOrder`

Processing rules:

- No `description`
  - Description only; does not affect runtime behavior
- No `scale`
  - No scaling; returns the raw value
- With `scale`
  - Applies only to numeric results
  - Return value = raw value × `scale`
  - Use decimal values such as `0.1`, `1`, `1000`
- No `statusEnum`
  - No status mapping
- `alarmEnabled` is not `true`
  - No alarm evaluation
- `alarmEnabled` is `true` but `alarmCondition` is missing
  - No alarm
- No `modbusWordOrder`
  - Modbus 32-bit points use the global `wcs.adapter.modbus.default-word-order`
- With `modbusWordOrder`
  - Meaningful only for Modbus 32-bit types such as `DINT`, `UDINT`, `REAL`
  - Supported values: `WORD_SWAP` and `BIG_ENDIAN`
  - If the canonical address already includes `:ws` or `:be`, the explicit suffix on the address takes precedence

## Field Reference

### `scale`

`scale` is a **generic** point semantic, not protocol-specific.

- Final return value = raw value × `scale`
- `scale=1` — no scaling
- `scale=0.1` — multiply result by 0.1
- `scale=1000` — multiply result by 1000

Typical use cases:

- Instrument raw values are integers but the business layer needs decimal display
- PLC/Modbus returns pre-scaled raw values
- Unified unit conversion

### `modbusWordOrder`

`modbusWordOrder` is **Modbus-specific**. It defines word order when 32-bit data spans two registers.

Supported values:

- `WORD_SWAP` — low word first, high word second
- `BIG_ENDIAN` — high word first, low word second

Notes:

- Meaningful only for 32-bit types
- No effect on 16-bit types such as `INT`, `UINT`, `WORD`
- Only word order is handled; register-internal byte swap is not applied

## Configuration Examples

### Minimal Example

```json
{
  "pointId": "CKSSDW",
  "name": "Outbound conveyor in position",
  "address": "DB105.DBW10",
  "dataType": "INT",
  "access": "READ_ONLY"
}
```

### Example with `scale`

```json
{
  "pointId": "GROSS",
  "name": "Gross weight",
  "address": 1,
  "dataType": "INT",
  "scale": 0.1,
  "access": "READ_ONLY",
  "description": "Gross weight = raw integer × 0.1 kg"
}
```

### Modbus Example with `modbusWordOrder`

```json
{
  "pointId": "FLOW",
  "name": "Instantaneous flow",
  "address": 10,
  "dataType": "REAL",
  "modbusWordOrder": "BIG_ENDIAN",
  "access": "READ_ONLY"
}
```

### Modbus `address` Formats

Modbus point `address` supports two forms:

- **Plain numeric**
  - Examples: `1`, `20`
  - Combined with `dataType` to infer a canonical address
  - Example: `address=1` and `dataType=INT` → `hr:1:i16`
  - Example: `address=7` and `dataType=BOOL` → `co:7`

- **Canonical** (recommended)
  - Format: `hr|ir|co|di:offset[:i16|u16|i32|u32|f32][:ws|be]`
  - Examples: `hr:1:i16`, `hr:10:f32:be`, `co:7`

Rules:

- `hr` — Holding Register
- `ir` — Input Register
- `co` — Coil
- `di` — Discrete Input
- Plain numeric `address`
  - `BOOL` defaults to `co`
  - Other numeric types default to `hr`
- Canonical `address`
  - Area, type, and word order come from the address itself
  - `dataType` remains in config but no longer alone determines the adapter scalar type
- `modbusWordOrder`
  - Meaningful only for 32-bit types (`DINT`, `UDINT`, `REAL`, etc.)
  - If the canonical address already has `:ws` or `:be`, the address wins

### Full Modbus `pointsConfig` Example (`SBZT`)

Example of a complete `device:points:config:{deviceId}` structure with a single `SBZT` point:

```json
{
  "deviceId": "MODBUS-STACKER-01",
  "pointsConfig": {
    "SBZT": {
      "pointId": "SBZT",
      "name": "Device status",
      "address": "hr:1:i16",
      "dataType": "INT",
      "scale": 1,
      "access": "READ_ONLY",
      "description": "Device status word, Holding Register 1, 16-bit signed integer"
    }
  }
}
```

Notes:

- The same `SBZT` point can use plain numeric form:
  - `address: 1`
  - `dataType: INT`
  - Inferred as `hr:1:i16`
- `scale` is optional; `scale=1` means no scaling (shown here for completeness)
- `modbusWordOrder` is omitted because `INT` uses one register (no 32-bit word order). Add it for `REAL`, `DINT`, `UDINT`, etc. when needed

### Full Configuration Example

```json
{
  "pointId": "SBZT",
  "name": "Device status",
  "address": "DB4.DBW2",
  "dataType": "INT",
  "access": "READ_ONLY",
  "description": "Device status",
  "statusEnum": "StackerCraneStatusEnum",
  "alarmEnabled": true,
  "alarmCondition": "range:[1000,2000];exclude:1041,1087"
}
```

## S7 `dataType` to Java Type Mapping

S7 point reads are converted to Java types by `dataType`:

- `BOOL` → `Boolean`
- `INT` → `Integer`
- `UINT` → `Integer`
- `WORD` → `Integer`
- `BYTE` → `Integer`
- `DINT` → `Long`
- `DWORD` → `Long`
- `REAL` → `BigDecimal`
- `LREAL` → `BigDecimal`
- `STRING[n]` → `String`

Non-S7 points do not undergo this conversion layer.

## Status Enum (`statusEnum`)

`statusEnum` specifies how a **raw device value** maps to a **system status**.

### Location

Place status enums under:

```text
wcs-core/src/main/java/cn/aitplus/wcs/core/domain/enums/device/
```

Rationale:

- `statusEnum` is a domain contract and should not live in `app`
- `execution` reads it but should not own it
- `core` is the natural shared module

### Implementation

Status enums must implement:

```java
cn.aitplus.wcs.core.domain.model.device.DeviceStatusValueEnum;
```

Contract:

```java
public interface DeviceStatusValueEnum {
    String getCode();
    String getStatus();
}
```

- `getCode()` — raw value code from the device
- `getStatus()` — unified internal status string

### Enum Example

Illustrative example only:

```java
package cn.aitplus.wcs.core.domain.enums.device;

import cn.aitplus.wcs.core.domain.model.device.DeviceStatusValueEnum;

/**
 * Example stacker crane status enum.
 */
public enum StackerCraneStatusEnum implements DeviceStatusValueEnum {

    WORKING("1", "WORKING"),
    IDLE("2", "IDLE"),
    ALARM("1001", "ALARM");

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

Corresponding point configuration:

```json
{
  "pointId": "SBZT",
  "name": "Device status",
  "address": "DB4.DBW2",
  "dataType": "INT",
  "access": "READ_ONLY",
  "statusEnum": "StackerCraneStatusEnum"
}
```

## Alarm Rules

Alarm rules apply to **S7 points only**.

### Enable Switch

Alarm evaluation runs only when `alarmEnabled=true`.

### Recommended `alarmCondition` Format

Use a structured, controlled format. Avoid arbitrary expressions.

```text
range:[min,max];exclude:value1,value2
```

- `range:[1000,2000]` — values in 1000–2000 (inclusive) trigger alarm
- `exclude:1041,1087` — values in range but listed here do **not** alarm

### Alarm Examples

#### Example 1: Fixed-value alarm

```json
{
  "pointId": "SBZT",
  "name": "Device status",
  "address": "DB4.DBW2",
  "dataType": "INT",
  "access": "READ_ONLY",
  "alarmEnabled": true,
  "alarmCondition": "1000,1001,1002,1003,1043"
}
```

Any of these values triggers an alarm.

#### Example 2: Range alarm

```json
{
  "pointId": "SBZT",
  "name": "Device status",
  "address": "DB4.DBW2",
  "dataType": "INT",
  "access": "READ_ONLY",
  "alarmEnabled": true,
  "alarmCondition": "range:[1000,2000]"
}
```

Values from 1000 to 2000 (inclusive) trigger an alarm.

#### Example 3: Range with exclusions

```json
{
  "pointId": "SBZT",
  "name": "Device status",
  "address": "DB4.DBW2",
  "dataType": "INT",
  "access": "READ_ONLY",
  "alarmEnabled": true,
  "alarmCondition": "range:[1000,2000];exclude:1041,1087"
}
```

Meaning:

- Value ≥ 1000 and ≤ 2000 → alarm
- Except `1041` and `1087` → no alarm

#### Example 4: Multi-segment rules (documentation format)

For future extension, keep the controlled format, e.g.:

```text
range:[1000,2000];exclude:1041,1087|range:[3000,3999]
```

Meaning:

- 1000–2000 alarms, excluding 1041 and 1087
- **Or** 3000–3999 also alarms

Note:

- This is a **recommended documentation format**
- Do not use until execution-layer parsing is implemented
- Add parser support in the execution layer before enabling

## Runtime Processing Order

Device point read pipeline:

1. Load `deviceConfig` by `deviceId`
2. Load `pointsConfig` by `deviceId`
3. Select adapter by `protocolType`
4. For **S7**, convert point address to PLC4X address
5. Read raw value via adapter
6. Apply protocol-specific type conversion when required
7. Apply `scale` to numeric results if configured
8. Apply `statusEnum` mapping if configured
9. Evaluate alarm via `alarmCondition` if alarms are enabled
