# 02 - Architecture

## 1. 模块结构（插件独立）
- wcs-app：启动与 API 入口
- wcs-core：领域模型、状态机、插件 SPI、上下文模型
- wcs-workflow：Camunda embedded、WarehouseGuard、StartCoordinator、Worker
- wcs-execution：Plan 生成与执行、CommandPipeline、资源锁编排
- wcs-adapters：S7/Modbus/RCS 实现
- wcs-plugins：插件实现（routing/planning/exception/startgate）
- wcs-infra：持久化、缓存、锁、迁移、审计
- docs：规格文档

## 2. 分层职责

### 2.1 Core（稳定内核）
- Task/Plan/Step/CommandExecution/ResourceLock
- 状态机与事件
- 插件接口定义（不含实现）

### 2.2 Workflow
- warehouseId 上下文与校验
- BPMN 部署与版本
- StartGate 决策后启动流程
- External Task Handler 调 execution

### 2.3 Execution
- PlanGenerator（主流程）
- PlanExpander（由插件扩展）
- CommandPipeline（锁→下发→等待→回写）
- 异常流与补偿流

### 2.4 Plugins
- StartGate 插件
- Routing 插件（同入同出）
- Planning 插件（换巷道、双深位临时移库）
- Exception 插件（放货有货、取货无货等）

### 2.5 Adapters
- S7/Modbus/RCS 适配
- 连接复用、心跳、重连、轮询/回调

## 3. 关键设计原则
- 任何入口必须携带 warehouseId 并验证
- 所有业务查询必须按 warehouse 过滤
- BPMN 只负责编排，不做设备细节
- 重试必须受幂等保护

## 4. 典型流程骨架（示例）
ProcessKey: `wcs_task_main`
1) Start（门禁前置）
2) GeneratePlan（external task）
3) ExecuteStep（循环 external task）
4) HandleException（external task）
5) Complete / Suspend / Fail



