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


