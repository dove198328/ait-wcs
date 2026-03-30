# 04 - Config Profile Chains

## 1. 目标
按 warehouse（warehouse）配置：
- 仓型
- 启用插件
- 责任链顺序
- 参数阈值（并发、超时、重试、限流）

## 2. WarehouseProfile（建议）
字段：
- warehouseId
- warehouseType（ASRS/SHUTTLE/MIXED）
- enabledPlugins（list）
- chains（map: chainName -> ordered plugins）
- params（json）

## 3. 责任链定义（建议）
- startGateChain
- routingChain
- planningExpandChain
- commandPipelineChain
- exceptionHandlingChain

## 4. 示例（YAML 语义）
```yaml
warehouseId: WH001
warehouseType: ASRS
enabledPlugins:
  - bidirectional-port
  - aisle-change
  - double-deep-relocation
  - exception-storage-mismatch
chains:
  startGate:
    - DeviceOnlinePolicy
    - ConcurrencyLimitPolicy
    - ResourceAvailablePolicy
    - DedupPolicy
  routing:
    - PortModePolicy
    - DirectionExclusionPolicy
    - LoadBalancingPolicy
  planningExpand:
    - AisleChangeExpander
    - DoubleDeepRelocationExpander
  exceptionHandling:
    - DropOccupiedHandler
    - PickEmptyHandler
params:
  timeout:
    ackMs: 2000
    doneMs: 120000
  retry:
    maxRetries: 3
    backoffMs: 1000
  port:
    allowBidirectionalParallel: false
```

## 5. 实例级工作流配置

除 `WarehouseProfile` 这类按仓配置外，WCS 实例本身还需要一组实例级工作流配置，用于约束当前实例能处理哪些仓。

推荐配置：

```yaml
wcs:
  workflow:
    allowedWarehouseIds:
      - 1
      - 2
      - 3
```

语义：

- 这是“当前实例允许处理的仓列表”
- 不是默认仓，也不是单仓运行配置
- 当前实例只允许部署、启动、查询、消费这些仓对应的 Camunda tenant

约束：

- `tenantId = String.valueOf(warehouseId)`
- 所有工作流入口都必须校验 `warehouseId` 是否在 `allowedWarehouseIds` 中
- 如果 `allowedWarehouseIds` 为空，代码必须显式定义语义，不能隐式放开所有仓

不推荐继续保留以下旧语义配置：

```yaml
wareHouseId: 1
```

原因：

- 它只能表达单仓
- 在多实例共享同一个 Camunda 库时语义不清
- 很容易被误用成运行时仓隔离依据

