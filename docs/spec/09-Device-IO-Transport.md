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
- **wcs-adapters**：各协议实现位于 **`cn.aitplus.wcs.adapters.io`**（如 `io.s7`、`io.modbus` 等），含连接池、心跳/重连（分协议）。**S7 / OPC UA 等适配默认项** 写在 **`wcs-adapters/src/main/resources/application.yml`**，与 **`wcs-app`** 的 **`application.yml`** 由 Spring Boot **自动合并**（主应用同键覆盖库内配置，无需 `spring.config.import`）；环境差异（如 **`wcs.adapter.*.enabled`**）在 **`wcs-app`** 或 profile 中覆盖即可。
- **wcs-execution**：CommandPipeline 下发步调用 **`cn.aitplus.wcs.adapters.io.registry.DeviceTransportRegistry`**，并回写 infra 层服务。

## 5. 协议策略（摘要）

| 域 | 连接形态 | 说明 |
|----|-----------|------|
| S7 / MODBUS | 长连接为主 | 可选心跳；轮询为业务读周期，与长连接正交 |
| RCS(HTTP) | 无状态请求 + 客户端连接池 | 非工业长连接语义 |
| OPC | 长会话 + 订阅 | 保活按 OPC UA 规范 |

## 6. S7（`wcs.adapter.s7`）健康检查与自动恢复（摘要）

- **借用前探测**：默认开启；优先 `ping`，失败时可配合 **`heartbeat-read-address`** 做一次**读**点探测；**不向 PLC 写巡检心跳位**。探测失败则丢弃该 `ConnectionKey` 下缓存连接，**随后在本次请求内按退避策略重连**。
- **业务 IO 失败**：丢弃连接，避免假活会话继续复用。
- **定时巡检（唯一周期）**：仅由 **`stale-check-interval-millis`** 驱动（毫秒，≤0 关闭）。每周期对池中连接：`isConnected` → 可选 **`stale-check-plc-heartbeat-write-address`** 向 PLC **写**交替 0/1（BOOL，供 PLC 识别 WCS 存活）→ 与借用相同的**读探测**（WCS 侧确认链路）。写失败或读探测失败均丢弃连接。
- **并发**：同一 `ConnectionKey` 上 **探测 + 业务 IO 在同一把锁内串行**，避免多线程共一条 `PlcConnection`。
- **临时连接**：`S7Plc4xDeviceTransport#executeWithNewConnection` 不经池，单次建连、读写、`finally` 关闭；供隔离调试或特殊任务使用（需直接注入该 Bean）。

## 7. OPC UA（`wcs.adapter.opcua`）摘要

- **代码分包**（`cn.aitplus.wcs.adapters.io.opcua`）：**`config`**（`OpcUaAdapterProperties`、`OpcUaAdapterConfiguration`）、**`session`**（`OpcUaClientRegistry`、连接回调）、**`transport`**（`OpcUaDeviceTransport`、`OpcUaFallbackDeviceTransport`）、**`subscription`**（`OpcUaSubscriptionService`、`OpcUaSubscriptionNotificationEvent`）。
- **实现**：**Eclipse Milo**（**不使用 PLC4X**）。会话与连接由 **`OpcUaClientRegistry`** 按 **`ConnectionKey`（仓 + `endpoint.opcEndpointUrl`）** 复用；**`OpcUaDeviceTransport`**（`execute` 读/写）与 **`OpcUaSubscriptionService`**（订阅）**共享同一注册表**，避免双连接池。
- **Maven 版本（以 Central 为准）**：当前工程使用 **`org.eclipse.milo:sdk-client:${opcua.version}`**，与父 POM 中 **`opcua.version`** 一致；截至对照 **search.maven.org**，该坐标下 **最新 release 为 `0.6.16`**。新一代栈坐标为 **`org.eclipse.milo:milo-sdk-client`**（如 **`1.0.0`**），包名与 API 与 0.6.x 不同，升级需单独改造依赖与代码。
- **开关**：`wcs.adapter.opcua.enabled=true` 时注册 Bean；默认关闭。
- **连接健康（与 S7「借用前探测」同级语义）**：
  - **`session-probe-enabled`**：复用连接前除会话 Future 外，可选读取标准节点 **`Server_ServerStatus_State`**；失败则丢弃该键下客户端并重连。
  - **`session-probe-timeout-millis`**：上述读超时。
  - **`SessionActivityListener`**：`session inactive` 时拆除池中客户端，避免半死不活会话。
  - **`idle-max-millis`**：连接**最后使用时间**超过该毫秒数则下次借用前断开重连；**0** 表示关闭空闲淘汰（长闲假死场景可在文档/运维建议中非 0）。
- **读/写（多 NodeId，`execute` 路径）**：一次请求可含 **多个** `DeviceIoItem`；每条 `address` 为 **NodeId 字符串**（如 `ns=2;s=MyVar`）。**读**项 `write` 非 true，**写**项 `write=true` 且带 `value`。按 **`max-nodes-per-service-call`**（默认 200）拆批 Read/Write。**`execute-read-enabled`** / **`execute-write-enabled`** 可分别关闭同步读、写（与 **`subscription-enabled`** 独立；订阅不受这两项关闭影响）。
- **响应 JSON**：`reads` 为 **数组** `{"address": "<NodeId>","value": ...}`，顺序与请求一致；`writes` 为 **数组** `{"address":"...","ok":true}`，并带 `writeCount`。
- **订阅（MonitoredItem，非 `execute`）**：
  - **`subscription-enabled`**：**MonitoredItem 监听**总开关；**false** 时不可 `register`，且不挂订阅侧 `SubscriptionListener`；**不**影响 `execute` 读/写（除非同时关闭 `execute-*`）。
  - 上层通过注入 **`OpcUaSubscriptionService#register(warehouseId, deviceId, endpoint, nodeIds)`** 注册一组 NodeId；**`unregister(registrationId)`** 注销。同一 `ConnectionKey` 下多设备多注册会 **合并到同一 `UaSubscription`**，按批 **`max-monitored-items-per-create-batch`** 创建监控项。
  - **Publish 失败 / 订阅 transfer 失败 / 客户端被拆除** 后自动 **延迟重建** 订阅。
  - **`subscription-publishing-interval-millis`**：创建 Subscription 时的发布间隔（毫秒，OPC 双精度配置项）。
  - **通知事件**：每条 MonitoredItem 数据通知发布一次 **`OpcUaSubscriptionNotificationEvent`**（`warehouseId`、`deviceId`、`nodeId`、**`value`**）。**不在适配器内**做时间窗聚合；多值凑齐、防抖等由 **wcs-execution**（或应用层）自行实现。**不在适配器内**写业务点位语义或 Redis 映射。
- **安全**：`security-policy` 与服务器端点协商（如 `None`、`Basic256Sha256`）；可选 `username`/`password`；`insecure-trust-server-certificate` 为 true 时接受任意服务端证书（仅建议开发/内网）。

---

与 [06-Module-Structure.md](./06-Module-Structure.md) 中 wcs-adapters / wcs-execution 职责一致。
