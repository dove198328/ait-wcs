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

- **PLC4X 连接串**：固定为 `s7://<host>:<port>?remote-rack=<rack>&remote-slot=<slot>&field-optimization=true`；`rack`/`slot` 来自 `DeviceEndpoint`（缺省 **0/1**）。
- **借用前探测**：默认开启；优先 `ping`，失败时可配合 **`heartbeat-read-address`** 做一次**读**点探测；**不向 PLC 写巡检心跳位**。探测失败则丢弃该 `ConnectionKey` 下缓存连接，**随后在本次请求内按退避策略重连**。
- **业务 IO 失败**：丢弃连接，避免假活会话继续复用。
- **定时巡检（唯一周期）**：仅由 **`stale-check-interval-millis`** 驱动（毫秒，≤0 关闭）。每周期对池中连接：`isConnected` → 可选 **`stale-check-plc-heartbeat-write-address`** 向 PLC **写**交替 0/1（BOOL，供 PLC 识别 WCS 存活）→ 与借用相同的**读探测**（WCS 侧确认链路）。写失败或读探测失败均丢弃连接。
- **并发**：同一 `ConnectionKey` 上 **探测 + 业务 IO 在同一把锁内串行**，避免多线程共一条 `PlcConnection`。
- **临时连接**：`DeviceTransport#executeWithNewConnection` 可由协议适配器覆盖实现“不经池，单次建连、读写、`finally` 关闭”；S7 当前已提供该能力，供隔离调试或特殊任务使用。

## 7. OPC UA（`wcs.adapter.opcua`）摘要

- **代码分包**（`cn.aitplus.wcs.adapters.io.opcua`）：**`config`**（`OpcUaAdapterProperties`、`OpcUaAdapterConfiguration`）、**`session`**（`OpcUaClientRegistry`、连接回调）、**`transport`**（`OpcUaDeviceTransport`、`OpcUaFallbackDeviceTransport`）、**`subscription`**（`OpcUaSubscriptionService`、`OpcUaSubscriptionNotificationEvent`）。
- **实现**：**Eclipse Milo**（**不使用 PLC4X**）。会话与连接由 **`OpcUaClientRegistry`** 按 **`ConnectionKey`（仓 + `endpoint.opcEndpointUrl`）** 复用；**`OpcUaDeviceTransport`**（`execute` 读/写）与 **`OpcUaSubscriptionService`**（订阅）**共享同一注册表**，避免双连接池。
- **Maven 版本（以 Central 为准）**：当前工程使用 **`org.eclipse.milo:sdk-client:${opcua.version}`**，与父 POM 中 **`opcua.version`** 一致；截至对照 **search.maven.org**，该坐标下 **最新 release 为 `0.6.16`**。新一代栈坐标为 **`org.eclipse.milo:milo-sdk-client`**（如 **`1.0.0`**），包名与 API 与 0.6.x 不同，升级需单独改造依赖与代码。
- **开关**：`wcs.adapter.opcua.enabled=true` 时注册 Bean；默认关闭。
- **连接健康（与 S7「借用前探测」同级语义）**：
  - **`session-probe-enabled`**：复用连接前除会话 Future 外，可选读取标准节点 **`Server_ServerStatus_State`**；失败则丢弃该键下客户端并重连。探活 Read 的服务调用超时与全局 **`default-request-timeout-millis`**（Milo **`setRequestTimeout`**）一致，无单独配置项。
  - **`SessionActivityListener`**：`session inactive` 时拆除池中客户端，避免半死不活会话。
  - **`idle-max-millis`**：连接**最后使用时间**超过该毫秒数则下次借用前断开重连；**0** 表示关闭空闲淘汰（长闲假死场景可在文档/运维建议中非 0）。
- **读/写（多 NodeId，`execute` 路径）**：一次请求可含 **多个** `DeviceIoItem`；每条 `address` 为 **NodeId 字符串**（如 `ns=2;s=MyVar`）。**读**项 `write` 非 true，**写**项 `write=true` 且带 `value`。按 **`max-nodes-per-service-call`**（默认 200）拆批 Read/Write。**`execute-read-enabled`** / **`execute-write-enabled`** 可分别关闭同步读、写（与 **`subscription-enabled`** 独立；订阅不受这两项关闭影响）。**`DeviceIoRequest.timeoutMillis`** 为跨协议字段（如 S7 使用）；OPC 同步 Read/Write 当前以 **`default-request-timeout-millis`**（Milo 建连 **`setRequestTimeout`**）为服务调用超时依据，**不**按请求级 `timeoutMillis` 分支。
- **响应 JSON**：`reads` 为 **数组** `{"address": "<NodeId>","value": ...}`，顺序与请求一致；`writes` 为 **数组** `{"address":"...","ok":true}`，并带 `writeCount`。
- **订阅（MonitoredItem，非 `execute`）**：
  - **`subscription-enabled`**：**MonitoredItem 监听**总开关；**false** 时不可 `register`，且不挂订阅侧 `SubscriptionListener`；**不**影响 `execute` 读/写（除非同时关闭 `execute-*`）。
  - 上层通过注入 **`OpcUaSubscriptionService#register(warehouseId, deviceId, endpoint, nodeIds)`** 注册一组 NodeId；**`unregister(registrationId)`** 注销。同一 `ConnectionKey` 下多设备多注册会 **合并到同一 Subscription**，对当前合并后的 MonitoredItem 列表 **单次 `createMonitoredItems`**（项数受 OPC 服务端与 Milo 限制；适配器内未再按项数拆批）。**重建正在执行**时若再次发生注册变更，适配器会 **标记并在本轮重建结束后补调度一次**，避免内存注册集与服务端 MonitoredItem 长期不一致（已与延迟任务合并的场景仍只跑一次待执行任务，由执行时读取最新注册集）。
  - **Publish 失败 / 订阅 transfer 失败 / 客户端被拆除** 后自动 **延迟重建** 订阅。
  - **`subscription-publishing-interval-millis`**：创建 Subscription 时的发布间隔（毫秒，OPC 双精度配置项）。
  - **`subscription-watchdog-interval-millis`**：大于 0 时映射 Milo **`setTargetKeepAliveInterval`（毫秒）**，由 Milo 按发布间隔推导 **`MaxKeepAliveCount` / `LifetimeCount`**，此时 **`subscription-max-keep-alive-count`**、**`subscription-lifetime-count`** 不生效；小于等于 0 时仅用后两项显式 count。**`subscription-watchdog-silence-multiplier`** 仍为 Milo 客户端 **watchdog 倍数**（与 OPC keep-alive 不同）。
  - **通知事件（载荷）**：每条 MonitoredItem 数据通知发布一次 **`OpcUaSubscriptionNotificationEvent`**（`warehouseId`、`deviceId`、`nodeId`、**`value`**）。**不在适配器内**做时间窗聚合；多值凑齐、防抖等由 **wcs-execution**（或应用层）自行实现。**不在适配器内**写业务点位语义或 Redis 映射。

<a id="opc-subscription-event-integration"></a>

### 7.1 集成必查：OPC 订阅通知线程模型（再次接 execution / 业务监听时必读）

> **目的**：后续在 **wcs-execution** 或 **wcs-app** 中编写 **`OpcUaSubscriptionNotificationEvent`** 监听、消费 OPC 推送时，必须按本节约束实现，否则易在现场出现 **watchdog、notification lost、订阅反复重建** 等问题；**适配器层不改为异步发布**，责任在集成侧。

**检索关键词（代码/文档搜索用）**：`OpcUaSubscriptionNotificationEvent`、`@EventListener` / `ApplicationListener`、`OpcUaSubscriptionService.deliver`、`publishEvent`、OPC 订阅通知、**Milo 回调线程**。

**事实行为（当前实现）**

- 适配器在 **`OpcUaSubscriptionService#deliver`** 中调用 Spring **`ApplicationEventPublisher#publishEvent`**。
- Spring 对同一进程内监听器的默认行为是：**在发布线程上同步调用**监听器方法。
- 该发布线程即 **Eclipse Milo** 在 **MonitoredItem 数据回调**里执行 **`deliver` 的同一线程**（OPC 推送链路的一部分）。

**集成侧必须遵守**

1. **`@EventListener` / `ApplicationListener` 实现须「尽快返回」**：仅做 O(1) 级工作（拷贝字段、入队、提交任务），**禁止**在监听器主路径中执行 **数据库、HTTP/RPC、大循环、重序列化** 等耗时逻辑。
2. **重业务必须脱离该线程**：在监听器内 **`executor.execute`**、**有界队列 + 消费者线程**、**Spring `@Async`（独立线程池）**、**消息中间件** 等任选其一；并自行处理 **背压、顺序、失败重试**（适配器不保证事件顺序跨线程）。
3. **联调/代码评审检查清单**：搜索项目中所有订阅 **`OpcUaSubscriptionNotificationEvent`** 或 **`extends ApplicationEvent` 且类型为该事件** 的监听器，确认 **无阻塞 I/O**；新增监听须在 PR 说明中引用本节。

**与安全 / 观测**：若监听器抛未捕获异常，行为以 Spring 与运行时为准；生产环境建议对监听路径打 **耗时指标** 或 **慢调用日志**。

- **安全**：`security-policy` 与服务器端点协商（如 `None`、`Basic256Sha256`）；可选 `username`/`password`；`insecure-trust-server-certificate` 为 true 时接受任意服务端证书（仅建议开发/内网）。

---

与 [06-Module-Structure.md](./06-Module-Structure.md) 中 wcs-adapters / wcs-execution 职责一致。
