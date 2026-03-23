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
- **租户与 Camunda**：系统租户即仓库；Camunda 的 `tenantId` 必须使用与 `warehouseId` 一致的字符串（如 `String.valueOf(warehouseId)`）。部署（`Deployment`）、流程实例查询、流程定义查询等凡支持租户过滤的 API，必须带该 `tenantId`，避免跨租户读到部署或实例
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
- **跨模块访问持久化（全局）**：`wcs-app`、`wcs-workflow`、`wcs-execution` 等 **不得** 直接注入或调用 `wcs-infra` 的 `*Mapper`；必须通过 `wcs-infra` 暴露的 `*Service` 完成查询与命令。`Mapper` 仅允许在 `wcs-infra` 模块内的 Service 实现中使用
- BPMN 只负责编排，不做设备细节
- 重试必须受幂等保护
- `CacheType.BOTH` 场景下，本地缓存允许设置过期时间，Redis 二级缓存默认不设置过期时间；缓存一致性依赖业务写路径主动 `put/remove/evict`
- 缓存失效/覆盖应优先在事务提交后执行；**所有业务缓存 key 必须带租户维度（`warehouseId` 或 `warehouseId` 与业务键的组合）**，避免跨租户污染；禁止在仅依赖「全局唯一主键」的前提下省略租户段（除非该 key 空间已按租户隔离且有文档说明）
- **JetCache 使用方式（全局）**：业务缓存以**方法级注解**为主——读用 `@Cached`，写用 `@CacheInvalidate` /（必要时）`@CacheUpdate`；**禁止**在业务代码中使用 `CacheManager#getOrCreateCache` + `QuickConfig` 等方式编程式创建缓存实例。若同一方法需多条失效规则，可使用多个 `@CacheInvalidate`（JetCache 支持）或拆到独立小 Bean 上再循环调用，避免自调用绕过 AOP
- **一级 / 二级过期策略**：`CacheType.BOTH` 时，本地与远程可分别配置——注解上 `localExpire`、`expire` 单位为**秒**；不指定则回落到 `application.yml` 的 `jetcache.local.*` / `jetcache.remote.*`（如远程不设 `expireAfterWriteInMillis` 即不在 Redis 侧设 TTL）。仅开本地时可只配 `local` 默认项
- 跨模块复用的业务字符串（如 deployment source、JetCache 区域名 `name`、状态字面量）必须统一收敛到公共常量或领域枚举中（如 `wcs-common` 的 `WcsConstants`），不得散落在 service / mapper / controller 中
- 有限状态、阶段、方向、类型等优先用领域 enum 收敛取值，并与数据库状态文档一一对齐；若表字段仍为 `varchar`（存字符串），实体可继续用 `String` 承载，业务代码通过 `SomeEnum.XXX.getValue()` 参与赋值、比较与查询条件，避免散落字面量
- 补偿/回滚失败不得覆盖主异常；必须保留原始异常，并记录或附加补偿异常（如 suppressed exception）
- 流程定义创建前必须完成 JSON/BPMN 格式校验、唯一性校验，并在成功写库前完成 Camunda deployment 与 `processDefId/deployId` 回填
- 流程定义允许覆盖更新，但前提是当前 `warehouseId + workflowDefId/processDefId` 不存在活跃任务或活跃流程实例；若已有任务数据引用，则不得修改 `workflowId`、`bizType`
- 流程定义删除采用物理删除，但删除前必须确认当前 `warehouseId + workflowDefId/processDefId` 不存在活跃任务或活跃流程实例；删除时需同步清理 Camunda deployment

### 3.1 二级缓存代码落点（JetCache `CacheType.BOTH` + 方法注解）

当前使用 `@Cached` / `@CacheInvalidate` 的位置（须遵守租户维度；与流程定义一致，写路径在 Service 方法上直接声明 `@CacheInvalidate`，不再使用 `afterCommit`）：

- `wcs-infra`：`WarehouseProfileServiceImpl` — `@Cached` 读 profile / chain；写路径在对应方法上 `@CacheInvalidate`；删除整仓等需按链名循环失效时经 `@Lazy` 自引用调用本类 `evictChainCacheEntry`（保证走 AOP）
- `wcs-infra`：`WorkflowDefinitionsServiceImpl` — `@Cached` 读；`@CacheInvalidate` 在更新/删除时淘汰 `wcs:workflow:*` 相关 key（主键维度 **`warehouseId:definitionId`**，其余为 `warehouseId:bizType|name|workflowId`）；新增依赖首次读加载缓存

## 4. 典型流程骨架（示例）
ProcessKey: `wcs_task_main`
1) Start（门禁前置）
2) GeneratePlan（external task）
3) ExecuteStep（循环 external task）
4) HandleException（external task）
5) Complete / Suspend / Fail



