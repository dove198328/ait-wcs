
---

# 07 - Plugin Orchestration (How Plugins Are Called)

> 目的：解释“插件引入后如何编排与调用”，并把它固化为可实现的运行机制：固定入口 + 配置驱动责任链 + 强校验 + 审计。  
> 原则：插件不会自行触发；所有插件都由框架固定入口通过 ChainRunner 调用。

---

## 1. 核心回答：插件引入了，但怎么调用？

插件引入后（进入 classpath 并注册为 Spring Bean），并不会自动执行。  
真正的执行由两部分决定：

1) **固定调用入口**（框架代码固定写死）
2) **WarehouseProfile 配置**（决定启用哪些插件、顺序如何）

即：**调用点固定，执行序列由配置决定。**

---

## 2. 固定调用入口（必须只有这些入口）

### 2.1 启动门禁入口（StartCoordinator）
调用链：`startGateChain`

用途：在启动流程前判断 START/DEFER/REJECT，并补充流程变量。

### 2.2 路由入口（Station/Port Routing）
调用链：`routingChain`

用途：选择入/出口/站台；同入同出策略在此链中生效；决定是否排队/拒绝。

### 2.3 计划扩展入口（Plan Expander）
调用链：`planningExpandChain`

用途：对初始 Plan 做扩展/改写，例如：
- 插入 ChangeAisle（换巷道）
- 插入 TEMP_RELOCATE（双深位临时移库）
- 插入 RCS_MOVE（AGV 介入）

### 2.4 执行入口（Command Pipeline）
调用链：`commandPipelineChain`

用途：执行 Step/Command，典型步骤：
校验 → 申请锁 → 下发 → 等待反馈 → 审计 → 释放锁 → 回写工作流。

> 注意：pipeline step 是“执行环节”，通常放在 wcs-execution 里；也可以允许作为插件，但要严格类型校验。

### 2.5 异常处置入口（Exception Dispatcher）
调用链：`exceptionHandlingChain`

用途：对异常做策略化处置（复核/纠偏/改道/挂起/人工），并决定错误语义：
- 业务异常：BpmnError
- 技术异常：Failure（可重试/退避）

---

## 3. 责任链编排方式：完全配置驱动

### 3.1 Profile 配置结构（必须）
每个 warehouseId 一份 profile，核心字段：

- enabledPlugins（启用哪些插件组）
- chains（chainName -> BeanName list）
- params（阈值）

示例（语义）：

```yaml
warehouseId: WH001
enabledPlugins: [routing, planning, exception]
chains:
  routingChain:
    - PortModePolicy
    - DirectionExclusionPolicy
    - LoadBalancingPolicy
  planningExpandChain:
    - AisleChangeExpander
    - DoubleDeepRelocationExpander
  exceptionHandlingChain:
    - DropOccupiedHandler
    - PickEmptyHandler
params:
  port.allowBidirectionalParallel: false
```

---

## 4. ChainRunner 执行规则（必须实现）

### 4.1 通用算法

对任意 chain，执行流程统一为：

1) 读取 `warehouseId` 对应 profile  
2) 解析 `chains.<chainName>` 为有序 BeanName 列表  
3) 校验节点合法性（存在、类型匹配、启用）  
4) 逐节点执行，生成决策或副作用  
5) 写入审计与度量  
6) 返回统一结果给入口编排器

伪代码（语义）：

```java
for (String beanName : chainNodes) {
  PluginNode node = registry.get(beanName);
  validateType(node, expectedSpiType);
  NodeResult r = node.invoke(context);
  audit(node, r, context);
  if (r.terminal()) {
    return r.toChainResult();
  }
}
return defaultResult(chainName, context);
```

### 4.2 终止策略

- `startGateChain`: 任一节点 `REJECT` 立即终止并拒绝启动；`DEFER` 终止并延后；全通过才 `START`
- `routingChain`: 任一节点 `accepted=false` 且 `terminal=true` 则拒绝；否则继续直到选出目标口
- `planningExpandChain`: 默认不短路，允许多个插件叠加改写 Plan
- `commandPipelineChain`: 出现不可恢复失败立即终止并进入异常分发
- `exceptionHandlingChain`: 第一个可处理并给出动作的节点终止（避免多策略冲突）

---

## 5. 插件校验与安全约束

### 5.1 启动前校验（Fail Fast）

- chain 名称必须在白名单：  
`startGateChain|routingChain|planningExpandChain|commandPipelineChain|exceptionHandlingChain`
- BeanName 必须存在于 Spring 容器
- Bean 必须实现该 chain 对应 SPI 接口
- profile 中引用但未启用的插件组，不允许进入执行链
- 同一 chain 内 `node_order` 不可重复

### 5.2 运行时校验

- 上下文必须有 `warehouseId`
- 所有插件不得修改 `warehouseId`
- 插件产生的副作用必须可审计（至少记录 before/after 或决策原因）
- 插件超时必须可控（默认超时建议 500ms，可 profile 覆盖）

---

## 6. 各入口返回语义（统一契约）

### 6.1 StartCoordinator

- 输入：`StartContext`
- 输出：`StartDecision(action, reason)`
- 映射规则：`START` -> 启动流程实例；`DEFER` -> 任务置 `READY/SUSPENDED`（按策略）并记录待重试原因；`REJECT` -> 任务置 `FAILED/CANCELED`（按业务语义）

### 6.2 Routing

- 输入：`RoutingContext`
- 输出：`RoutingDecision(accepted, targetPort, reason)`
- 规则：`accepted=true` 时 `targetPort` 必填

### 6.3 PlanExpander

- 输入：`PlanContext`
- 输出：改写后的 `PlanContext`（主要是 step 列表变更）
- 规则：禁止删除已执行 step；可插入未执行 step；插入必须重排 `seq`

### 6.4 DispatchPolicy

- 输入：`DispatchContext`
- 输出：`DispatchDecision(executable, reason)`
- 规则：`executable=false` 时必须给出可观测 reason（用于审计与告警）

### 6.5 ExceptionDispatcher

- 输入：`ExceptionContext`
- 输出：`ExceptionDecision(action, reason)`
- 动作映射：`RETRY` -> failure + retry/backoff；`SUSPEND` -> 流程挂起 + 人工介入；`REDIRECT` -> 改道并回到计划/执行入口；`MANUAL_INTERVENTION` -> 创建人工处理工单并挂起

---

## 7. 异常语义映射（BPMN/Worker）

- 业务异常（如 `DROP_OCCUPIED`, `PICK_EMPTY`, `RESOURCE_CONFLICT`）通过 `BpmnError` 抛给流程分支
- 技术异常（超时、断连、5xx、序列化失败）通过 worker failure 上报，触发 Camunda retry
- 重试耗尽后进入 `exceptionHandlingChain` 做最终处置

---

## 8. 审计与可观测性（最低要求）

每次节点调用至少落一条 `plugin_invocation_audit` 语义日志（可先日志后落库）：

- warehouseId
- traceId/correlationId
- chainName
- pluginBeanName
- inputDigest（输入摘要，避免全量敏感数据）
- outputDecision
- latencyMs
- success/failure
- errorCode/errorMessage
- occurredAt

建议指标：

- `wcs_plugin_invocations_total{warehouse,chain,plugin,result}`
- `wcs_plugin_latency_ms{warehouse,chain,plugin}`
- `wcs_chain_failures_total{warehouse,chain,reason}`

---

## 9. 默认降级与空链策略

- `startGateChain` 为空：默认 `START`
- `routingChain` 为空：使用系统默认路由器（单口优先）
- `planningExpandChain` 为空：按初始 Plan 执行，不扩展
- `commandPipelineChain` 为空：使用内置标准 pipeline
- `exceptionHandlingChain` 为空：默认 `SUSPEND + 告警`

---

## 10. 测试验收清单（最小）

1. profile 指向不存在 Bean -> 启动失败（Fail Fast）
2. routingChain 配置了错误 SPI 类型 -> 启动失败
3. 同 warehouse 修改 chain 顺序后，新任务按新顺序生效，运行中任务不受影响
4. 插件超时触发技术异常，进入 retry/backoff
5. `DropOccupiedHandler` 触发业务异常分支并输出预期动作
6. 多 warehouse 同名插件并行执行，审计记录互不污染



