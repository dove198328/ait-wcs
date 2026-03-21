# 03 - Domain Model

> 采用"上层保留现有模型 + 下层补齐能力表"的收敛方案。  
> 上层：Task → SubTask → Instruction（含 `List<Command>`）  
> 下层（松耦合横切）：CommandExecution（指令执行审计）、ResourceLock（资源锁）、WarehouseProfile / ProfileChainNode（仓库配置与责任链编排）

---

## 1. 上层核心实体（任务执行链路）

### 1.1 Task（主任务）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| warehouseId | Long | 仓库ID（强约束） |
| workflowDefId | String | 流程定义ID |
| taskName | String | 任务名称 |
| priority | Integer | 优先级 |
| status | String | 状态 |
| bizType | String | 业务类型主类 |
| taskType | String | 任务类型 |
| taskCategory | String | 业务类型：OUTBOUND/INBOUND/INTERNAL/INVENTORY |
| taskDirection | String | 任务流向 |
| startPoint | String | 起点 |
| endPoint | String | 终点 |
| location | String | 货位编码（巷道-列-层-深度） |
| depth | String | 货位深度类型：SINGLE/FRONT/BACK |
| frontEmpty | Boolean | 前排是否为空 |
| workDirection | String | 作业方向：IN/OUT/MOVE_LOCAL/MOVE_CROSS |
| vehicleId | String | 载具号 |
| deviceIds | String | 设备ID列表（逗号分隔） |
| taskNumber | String | 任务编号 |
| orderNo | String | WMS订单号 |
| wmsBizId | Long | WMS业务ID |
| aisleNo | Integer | 巷道号 |
| twinsNo | String | 组号（双深位配对） |
| isAutoStart | Integer | 是否自动启动 |
| processInstanceId | String | Camunda 流程实例ID |
| processDefinitionId | String | 流程定义ID |
| taskPhase | String | 任务阶段 |
| scannerDeviceId | String | 扫码器设备ID |
| isCK | Boolean | 是否临时移库任务 |
| taskDistribution | Integer | 任务下发情况 |
| rkkFlag | Boolean | 入库标志 |
| startedAt | LocalDateTime | 开始时间 |
| completedAt | LocalDateTime | 完成时间 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

聚合关系：`Task` 包含 `List<SubTask>`

### 1.2 SubTask（子任务）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| taskId | Long | 所属主任务ID |
| warehouseId | Long | 仓库ID |
| subtaskDefId | String | 子任务定义ID |
| name | String | 子任务名称 |
| priority | Integer | 优先级 |
| status | String | 状态 |
| compensation | String | 补偿策略 |
| allowManual | Integer | 是否允许手动执行 |
| currentInstructionIndex | Integer | 当前执行指令索引 |
| selectedDeviceId | Integer | 选中的设备ID |
| activityInstanceId | String | Camunda 活动实例ID |
| area | String | 区域 |
| workflowDefId | String | 流程定义ID |
| isStartNextProcess | Boolean | 是否推进下一流程 |
| freeDeviceId | String | 推进流程释放的设备ID |
| checkAisle | Boolean | 是否巷道过滤 |
| remark | String | 备注 |
| completedAt | Date | 完成时间 |
| createdAt | Date | 创建时间 |
| updatedAt | Date | 更新时间 |

### 1.3 Instruction（指令）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| subtaskId | Long | 所属子任务ID |
| taskId | Long | 所属主任务ID（冗余，便于快速查询） |
| sequence | Integer | 指令执行顺序 |
| protocol | String | 指令协议（S7/MODBUS/RCS/HTTP 等） |
| commands | List\<Command\> | 指令命令项列表（JSON 存储） |
| params | String | 额外参数（JSON 字符串） |
| status | String | 状态 |
| deviceId | String | 可执行设备ID |
| messageEvent | Boolean | 是否采用消息事件驱动等待 |
| eventLogic | String | 消息事件逻辑关系（AND/OR/NOT） |
| remark | String | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

聚合关系：`Instruction` 包含 `List<Command>`

### 1.4 Command（指令命令项，值对象）

| 字段 | 类型 | 说明 |
|------|------|------|
| command | String | 命令名称（点位/寄存器标识） |
| value | Object | 命令值 |
| isWrite | Boolean | 是否写入命令 |

`Command` 以 JSON 数组存储在 `instructions.commands` 列中，不单独建表。

---

## 2. 下层横切能力实体

### 2.1 CommandExecution（指令执行与审计）

> 记录每一次设备/三方指令的执行细节，用于审计、重试、超时监控。  
> 通过 `task_id` / `subtask_id` / `instruction_id` 与上层模型松耦合关联。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| warehouseId | String | 仓库ID |
| taskId | Long | 关联主任务（可选） |
| subtaskId | Long | 关联子任务（可选） |
| instructionId | Long | 关联指令（可选） |
| domain | String | 协议域：S7/MODBUS/RCS |
| deviceId | String | 设备ID |
| commandType | String | 命令类型 |
| idempotencyKey | String | 幂等键 |
| status | String | 执行状态 |
| requestJson | JSON | 请求报文 |
| responseJson | JSON | 响应报文 |
| errorCode | String | 错误码 |
| errorMessage | String | 错误信息 |
| traceId | String | 链路追踪ID |
| correlationId | String | 关联ID |
| startedAt | Timestamp | 开始时间 |
| endedAt | Timestamp | 结束时间 |
| createdAt | Timestamp | 创建时间 |
| updatedAt | Timestamp | 更新时间 |

### 2.2 ResourceLock（资源锁）

> 站台、巷道、货位、设备等物理资源的排他锁。  
> 通过 `ownerType`（TASK/SUBTASK/INSTRUCTION）+ `ownerId` 关联上层实体。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| warehouseId | String | 仓库ID |
| lockKey | String | 资源标识（如 `AISLE:3`、`STATION:IN-01`） |
| ownerType | String | 持有者类型：TASK/SUBTASK/INSTRUCTION |
| ownerId | String | 持有者ID |
| status | String | 锁状态 |
| expireAt | Timestamp | 过期时间（死锁回收） |
| createdAt | Timestamp | 创建时间 |
| updatedAt | Timestamp | 更新时间 |

### 2.3 WarehouseProfile（仓库配置）

> warehouse 级全局配置，控制该仓库启用哪些插件、使用哪些参数。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| warehouseId | String | 仓库ID（唯一） |
| warehouseType | String | 仓库类型 |
| enabledPluginsJson | JSON | 启用的插件列表 |
| paramsJson | JSON | 参数配置 |
| version | Integer | 版本号 |
| active | Boolean | 是否激活 |
| createdAt | Timestamp | 创建时间 |
| updatedAt | Timestamp | 更新时间 |

### 2.4 ProfileChainNode（责任链节点）

> 定义各 chain 中插件的执行顺序，支持按 warehouse 配置化编排。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| warehouseId | String | 仓库ID |
| chainName | String | 链名称（startGate/routing/planning/dispatch/exception） |
| nodeOrder | Integer | 节点顺序 |
| beanName | String | Spring Bean 名称 |
| enabled | Boolean | 是否启用 |
| createdAt | Timestamp | 创建时间 |
| updatedAt | Timestamp | 更新时间 |

---

## 3. 实体关系概览

```text
Task (tasks)
 ├── 1:N ── SubTask (subtasks)
 │            └── 1:N ── Instruction (instructions)
 │                         └── 1:N ── Command (JSON内嵌，不建表)
 │
 ├── 1:N ── CommandExecution (wcs_command_execution)  [松耦合，通过ID引用]
 └── 1:N ── ResourceLock (wcs_resource_lock)          [松耦合，通过ownerType+ownerId]

WarehouseProfile (wcs_warehouse_profile)              [独立，按warehouseId关联]
 └── 1:N ── ProfileChainNode (wcs_profile_chain_node) [独立，按warehouseId+chainName关联]
```

---

## 4. 状态流转规则

- **Task**：`pending → executing → (completed | suspended | failed | canceled)`
- **SubTask**：`pending → executing → (completed | failed | skipped)`
- **Instruction**：`pending → executing → (completed | failed | skipped)`
- **CommandExecution**：`SENT → ACK → RUNNING → (DONE | ERROR | TIMEOUT | CANCELED)`

---

## 5. 领域事件

- TaskCreated / TaskStarted / TaskCompleted / TaskFailed / TaskSuspended / TaskCanceled
- SubTaskStarted / SubTaskCompleted / SubTaskFailed
- InstructionSent / InstructionDone / InstructionFailed
- CommandExecutionSent / CommandExecutionDone / CommandExecutionFailed
- ResourceLocked / ResourceReleased
- AlarmRaised

---

## 6. 插件 SPI（接口级）

- StartGatePlugin：启动门禁策略
- RoutingPlugin：站台/口路由
- PlanExpanderPlugin：计划扩展（换巷道、双深位临时移库等步骤插入）
- DispatchPolicyPlugin：调度策略（优先级/公平/饥饿控制）
- ExceptionHandlerPlugin：异常处理（放货有货/取货无货/通信异常）

---

## 7. 异常语义

- 业务异常（放货有货/取货无货/资源冲突）→ BpmnError
- 技术异常（超时/断线/5xx）→ failure + retry/backoff
