# 09 - Device IO 契约与连接端点键

## 1. 目标

- 在 **wcs-core** 定义协议无关的 **DeviceTransport** 与 **DeviceIoRequest/Result**，供 **wcs-execution** 编排与 **wcs-adapters** 实现。
- 连接复用键与业务 **deviceId** 解耦：多台逻辑设备共用 **同一 IP/端口** 时，共享少量长连接（或会话）。

## 2. 连接端点键（ConnectionKey）

- **必须**包含：`warehouseId`（租户隔离）、**协议域**（与 `CommandDomain` 一致）、**host**、**port**。
- **按协议附加**：S7 含 **rack/slot**；Modbus TCP 键以 **host:port** 为主，**unitId** 在单次请求中携带；OPC UA 以 **endpointUrl** 为主；HTTP(RCS) 以 **baseUrl** 为主。
- **不得**默认用 `deviceId` 作为 TCP 连接唯一键。

## 3. 与 Command / CommandExecution

- **Instruction.params / Command** 中的点位信息映射为 **DeviceIoItem.address/value/write**，由适配器解析地址语法。
- **幂等键、traceId** 由 execution 传入请求，适配器仅执行 IO；**审计回写**由 execution 调用 **CommandExecutionService** 完成。

## 4. 模块边界

- **wcs-core**：仅接口与 DTO，不依赖 PLC4X/Modbus/HTTP/OPC 库。
- **wcs-adapters**：各协议实现位于 **`cn.aitplus.wcs.adapters.io`**（如 `io.s7`、`io.modbus` 等），含连接池、心跳/重连（分协议）。
- **wcs-execution**：CommandPipeline 下发步调用 **`cn.aitplus.wcs.adapters.io.registry.DeviceTransportRegistry`**，并回写 infra 层服务。

## 5. 协议策略（摘要）

| 域 | 连接形态 | 说明 |
|----|-----------|------|
| S7 / MODBUS | 长连接为主 | 可选心跳；轮询为业务读周期，与长连接正交 |
| RCS(HTTP) | 无状态请求 + 客户端连接池 | 非工业长连接语义 |
| OPC | 长会话 + 订阅 | 保活按 OPC UA 规范 |

## 6. S7（`wcs.adapter.s7`）健康检查与自动恢复（摘要）

- **借用前探测**：默认开启；优先 `ping`，失败时可配合 **`heartbeat-read-address`** 做一次读点探测；探测失败则丢弃该 `ConnectionKey` 下缓存连接，**随后在本次请求内按退避策略重连**。
- **心跳**：周期读失败则 **丢弃连接**（不再仅打日志），下次业务请求走重连。
- **业务 IO 失败**：丢弃连接，避免假活会话继续复用。
- **定时巡检**：可选（`stale-check-interval-millis`），对池中连接做 `isConnected` + 与借用相同的探测逻辑，减轻长期无流量时的假活。
- **并发**：同一 `ConnectionKey` 上 **探测 + 业务 IO 在同一把锁内串行**，避免多线程共一条 `PlcConnection`。
- **临时连接**：`S7Plc4xDeviceTransport#executeWithNewConnection` 不经池，单次建连、读写、`finally` 关闭；供隔离调试或特殊任务使用（需直接注入该 Bean）。

---

与 [06-Module-Structure.md](./06-Module-Structure.md) 中 wcs-adapters / wcs-execution 职责一致。
