# 08 - Workflow Integration

> 目的：定义 WCS 在“多实例部署、共享业务库与 Camunda 库、按 `warehouseId` 进行仓隔离”的前提下，如何集成 Camunda 7.19 Embedded。
> 结论先行：允许多个 WCS 实例共同连接同一个 Camunda 库；仓库隔离不通过“单实例单库”实现，而通过 `tenantId = String.valueOf(warehouseId)`、业务幂等、全局锁与查询过滤共同实现。

---

## 1. 适用场景

本规范适用于以下部署方式：

- WCS 以多实例方式部署，例如实例 A / B / C
- 多个实例共享同一个业务数据库
- 多个实例共享同一个 Camunda 数据库
- 实例可按仓处理任务，仓库通过 `warehouseId` 标识
- Camunda 中的 `tenantId` 与 `warehouseId` 一一对应

不采用以下模式：

- 一仓一个 Camunda 库
- 把 `application.yml` 中的单个 `wareHouseId` 作为运行时多仓隔离主依据
- 依赖 JVM 本地内存完成跨实例幂等或资源锁

---

## 2. 核心建议

### 2.1 总体建议

建议采用“单库多 tenant”模式，而不是“一仓一库”：

- Camunda 天然支持多 tenant 查询与部署
- 多实例共享库更容易统一运维、升级、监控与备份
- 一仓一库会显著增加连接池、DDL、迁移、告警与版本管理复杂度

### 2.2 关键约束

必须满足以下条件，否则共享库方案不成立：

1. 所有流程定义部署必须绑定 `tenantId = warehouseId`
2. 所有流程实例启动必须绑定 `tenantId = warehouseId`
3. 所有流程查询必须按 `tenantId` 过滤
4. 所有 Worker 消费必须限制在实例被授权处理的 `warehouseId` 范围内
5. 业务幂等键、缓存键、资源锁键都必须包含 `warehouseId`
6. 不允许用实例本地变量替代请求级 `warehouseId`

---

## 3. 为什么不建议一仓一个库

除非客户明确要求物理隔离，否则不建议一仓一个 Camunda 库，原因如下：

- 运维复杂度高：仓越多，库越多，连接池和巡检成本越高
- 流程版本管理复杂：同一版流程要重复部署到多个库
- 多实例路由复杂：实例与数据库之间需要额外路由层
- 故障排查困难：跨仓问题变成跨库问题，监控视图分裂

只有在以下情况才考虑一仓一库：

- 客户要求物理隔离或合规隔离
- 单仓规模极大，数据库负载明显独立
- 仓与仓之间的升级节奏长期不同

---

## 4. 运行模型

### 4.1 `warehouseId` 与 `tenantId`

统一约定：

- 业务仓标识：`warehouseId`
- Camunda 租户标识：`tenantId = String.valueOf(warehouseId)`

示例：

- `warehouseId = 1` -> `tenantId = "1"`
- `warehouseId = 20001` -> `tenantId = "20001"`

禁止出现：

- Camunda tenant 用别名而业务库用数值 ID
- 同一仓在不同表或不同层中使用不同 tenant 表示

### 4.2 实例与仓的关系

实例部署建议支持配置“允许处理哪些仓”，而不是固定单仓：

```yaml
wcs:
  workflow:
    allowedWarehouseIds: [1, 2, 3]
```

语义如下：

- 实例 A 可配置处理 `[1,2]`
- 实例 B 可配置处理 `[3,4]`
- 实例 C 可配置处理 `[5]`

用途：

- 控制 Worker 只消费被授权仓的数据
- 控制实例启动时的自检范围
- 为后续扩容、迁仓、灰度提供基础

`application.yml` 中的单个 `wareHouseId` 只允许作为开发默认值，不允许作为生产多仓主配置。

---

## 5. 模块落点

工作流集成必须落在 `wcs-workflow`，各模块职责如下：

- `wcs-app`
  - 解析 API 入参中的 `warehouseId`
  - 调用 `wcs-workflow` 提供的服务
  - 不直接操作 Camunda 原生 API

- `wcs-workflow`
  - 封装 Camunda 部署、启动、查询、Worker、错误映射
  - 执行 tenant 绑定校验
  - 负责 `StartCoordinator`、`WarehouseGuard`、Worker 注册

- `wcs-execution`
  - 执行计划生成与命令执行编排
  - 不直接决定 tenant

- `wcs-infra`
  - 提供业务表持久化
  - 提供 profile、任务、资源锁、审计等服务

---

## 6. 推荐的工作流集成组件

建议在 `wcs-workflow` 中落以下组件：

### 6.1 `WarehouseTenantSupport`

职责：

- `Long warehouseId -> String tenantId`
- 校验 `warehouseId` 是否为空、是否在实例允许处理范围中
- 提供统一 tenant 转换，避免字符串散落

### 6.2 `WarehouseGuard`

职责：

- 对部署、启动、查询、Worker 拉取进行统一仓校验
- 拒绝未授权仓或缺失仓上下文的调用

### 6.3 `WorkflowDeploymentService`

职责：

- 将 BPMN 部署到指定 tenant
- 查询指定 tenant 下的流程定义
- 控制覆盖更新与删除前校验

### 6.4 `WorkflowStartService`

职责：

- 执行 `StartCoordinator`
- 计算并检查 businessKey 幂等
- 启动指定 tenant 的流程实例

### 6.5 `WorkflowQueryService`

职责：

- 提供按 tenant 查询流程定义、流程实例、任务的只读能力
- 禁止无 tenant 的全局查询

### 6.6 `ExternalTaskWorkerRegistry`

职责：

- 注册 topic 对应的 worker
- 按实例允许的 `warehouseId` 范围消费任务
- 将 worker 结果映射回 Camunda complete / handleFailure / handleBpmnError

---

## 7. 启动、部署、查询、消费的强约束

### 7.1 流程定义部署

部署时必须：

- 使用 `tenantId = warehouseId`
- 指定统一 `source`
- 在业务表中保存 `deployId`、`processDefId`

伪代码：

```java
repositoryService.createDeployment()
    .tenantId(tenantId)
    .source(WcsConstants.DEPLOYMENT_SOURCE)
    .addString(processKey + ".bpmn", bpmnXml)
    .deploy();
```

### 7.2 流程实例启动

启动时必须：

- 先经过 `StartGateChain`
- 构造幂等 `businessKey`
- 使用 tenant 启动

推荐 businessKey：

```text
warehouseId:upstreamTaskId:taskType
```

如果上游没有 `upstreamTaskId`，必须定义等价唯一业务键，禁止直接用随机 UUID。

### 7.3 流程查询

所有查询必须 tenant-aware：

- 查流程定义：按 `tenantId`
- 查流程实例：按 `tenantId`
- 查任务：按 `tenantId`
- 查业务表：按 `warehouseId`

禁止：

- 先查全量再在 Java 内存中过滤
- 省略 tenant 查询“因为当前实例只服务一个仓”

### 7.4 Worker 消费

Worker 是共享库模式下的核心风险点，必须满足：

- 只能消费实例被授权的 `warehouseId`
- topic 获取后必须再次校验 `tenantId`
- 命令执行前必须做幂等和资源锁

如果 Worker 层做不到 tenant 过滤，则不能上线共享库模式。

---

## 8. 多实例下的并发与一致性建议

### 8.1 十几个客户端刷新是否有问题

通常不是本质问题，但应遵循以下原则：

- 列表页、统计页优先查询业务表
- Camunda 仅做编排引擎，不作为所有页面的主查询源
- 高频页面使用 3 到 5 秒轮询，或改为 SSE / WebSocket
- 不做“全量运行实例 + 全量历史表”的高频扫描

### 8.2 真正需要关注的问题

共享库模式下，真正的风险是：

- 重复启动流程实例
- 多实例并发消费同一业务动作
- 本地锁在多实例下失效
- 缓存未带 tenant 导致串仓

### 8.3 必须具备的保护措施

必须同时具备以下机制：

1. businessKey 幂等
2. 业务表唯一约束或幂等记录
3. 全局资源锁，禁止只用 JVM 本地锁
4. 设备命令幂等，重复执行不得导致重复动作
5. 异常重试与补偿分离，避免重试掩盖业务异常

---

## 9. 对查询与页面刷新的建议

推荐查询分层：

- API 列表页：查询 `tasks / subtasks / command_execution / resource_lock` 等业务表
- API 详情页：必要时补查 Camunda 运行态
- 审计页：优先查业务审计表和业务日志
- 运维页：允许查询 Camunda，但必须 tenant 过滤且有分页

不推荐：

- 前端每次刷新都直接访问 Camunda `runtime` / `history` 全量视图
- 用流程引擎表替代业务投影表

---

## 10. 配置建议

### 10.1 推荐新增配置

```yaml
wcs:
  workflow:
    allowedWarehouseIds: [1, 2, 3]
    deployment:
      source: wcs-workflow-definition
    worker:
      lockDurationMs: 60000
      maxTasks: 10
      asyncResponseTimeoutMs: 30000
      topics:
        generatePlan: wcs.generate-plan
        executeStep: wcs.execute-step
        handleException: wcs.handle-exception
camunda:
  bpm:
    database:
      schema-update: true
    history-level: full
    job-execution:
      enabled: true
    authorization:
      enabled: false
```

### 10.2 配置原则

- 允许处理的仓范围必须显式配置
- Worker 参数要集中配置，不散落在代码中
- 单个 `wareHouseId` 不作为生产多仓架构核心配置

---

## 11. 推荐落地顺序

### 阶段 1：基础接入

- 引入 `WarehouseTenantSupport`
- 引入 `WarehouseGuard`
- 完成 tenant-aware 的部署、启动、查询封装

### 阶段 2：启动编排

- 实现 `StartCoordinator`
- 固化 businessKey 幂等
- 完成任务创建 -> 流程启动的主链路

### 阶段 3：Worker 体系

- 实现 `ExternalTaskWorkerRegistry`
- 接通 `GeneratePlan / ExecuteStep / HandleException`
- 确保 Worker 只消费授权仓

### 阶段 4：可观测与稳态

- 增加流程审计、topic 指标、失败率指标
- 增加缓存、查询分层、限流
- 补齐集成测试与并发测试

---

## 12. 验收清单

至少满足以下验收项：

1. 不同 `warehouseId` 的流程定义只能在对应 tenant 下看到
2. 同一上游任务被多次提交时，只启动一个流程实例
3. 未授权仓不会被当前实例消费
4. 多实例并发时，同一条设备命令不会重复下发
5. 列表页高频刷新时，不需要直接扫描 Camunda 全量运行表
6. 任一缓存 key 都能从规则上看出 tenant 维度
7. 任一 Camunda 查询调用都能指出它使用了哪个 tenant

---

## 13. 最终建议

对当前项目，推荐采用以下方案：

- 继续使用单个共享 Camunda 库
- `warehouseId` 统一映射为 Camunda `tenantId`
- 工作流集成全部落在 `wcs-workflow`
- 实例通过 `allowedWarehouseIds` 控制消费范围
- 不再以单个全局 `wareHouseId` 作为多仓运行时依据

该方案在架构、运维复杂度和后续扩展性之间是当前最平衡的选择。
