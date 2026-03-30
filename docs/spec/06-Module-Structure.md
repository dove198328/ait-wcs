# 06 - Module Structure (Plugins as Modules)

> 目的：定义 WCS 平台的代码模块结构（插件以独立模块拆分），并明确各模块如何在编译期与运行时进行组装。  
> 关键约束：Camunda 7.19 Embedded；Spring Boot 2.7.18；以 `warehouseId` 作为仓库上下文；插件可按 warehouse 配置启用/禁用与排序。

---

## 1. 顶层模块结构

```text
wcs-platform/
├─ wcs-common/                       # 通用工具与 DTO（AjaxResult、TableDataInfo、AESUtil、EncryptUtils 等）
├─ wcs-core/                         # 领域模型、状态机、上下文、SPI接口、ChainRunner
├─ wcs-app/                          # Spring Boot 启动器 + REST API 入口 + Swagger 配置
├─ wcs-infra/                        # DB Mapper/Repository + TypeHandler + 审计 + Profile 存储
├─ wcs-workflow/                     # Camunda embedded、WarehouseGuard、StartCoordinator、Worker
├─ wcs-execution/                    # Instruction/Command 执行编排（不放策略实现）
├─ wcs-adapters/                     # S7/Modbus/RCS 适配实现
├─ wcs-plugins-startgate/            # 插件模块：启动门禁策略
├─ wcs-plugins-routing/              # 插件模块：站台/口路由（同入同出）
├─ wcs-plugins-planning/             # 插件模块：计划扩展（换巷道、双深位临时移库）
├─ wcs-plugins-dispatch/             # 插件模块：调度策略（优先级/公平/饥饿控制）
├─ wcs-plugins-exception/            # 插件模块：异常处理（放货有货/取货无货/通信异常）
└─ docs/                             # 规格文档
```

---

## 2. 模块职责边界

### 2.1 wcs-common（通用基础）

- 统一响应体：`AjaxResult<T>`、`TableDataInfo<T>`
- 分页工具：`PageQuery`、`PageUtils`
- 加密工具：`AESUtil`、`EncryptUtils`
- Spring 上下文工具：`SpringUtils`
- 自定义异常：`BusinessException`
- **不依赖任何业务模块**，仅依赖：jackson-annotations、hutool-crypto、hutool-json、slf4j-api、spring-context/aop/beans

### 2.2 wcs-core（稳定内核）

- 领域对象：Task / SubTask / Instruction / Command / CommandExecution / ResourceLock
- 状态机与领域事件
- 插件 SPI 接口（仅接口，不包含实现）
- ChainRunner：按 warehouse profile 装配并执行插件链（编排引擎）
- 上下文对象：StartContext / RoutingContext / PlanContext / ExecContext / ExceptionContext 等

### 2.3 wcs-app（启动与 API）

- Spring Boot 启动入口（`WcsApplication`）
- REST API：任务创建/查询/取消/人工干预/设备状态
- warehouse 解析：从 path 变量 `{wareHouseId}` 提取仓库ID
- Swagger 配置（Springfox 3.0，可通过 `swagger.enabled` 开关控制）
- 全局异常处理（`GlobalExceptionHandler`）
- 依赖：wcs-common、wcs-core、wcs-infra、wcs-workflow、wcs-execution

### 2.4 wcs-infra（基础设施）

- DB schema（Flyway/Liquibase 迁移脚本）
- MyBatis Mapper XML（tasks/subtasks/instructions/wcs_command_execution/wcs_resource_lock）
- TypeHandler（`CommandListTypeHandler`、`JsonTypeHandler`）
- Service 接口与实现（TasksService、SubTasksService、InstructionsService）
- WarehouseProfile 的存储与读取（按 warehouseId）
- 多数据库方言支持（通过 `_databaseId` 分支：PostgreSQL、SQL Server）

### 2.5 wcs-workflow

- Camunda 7.19 Embedded 集成
- WarehouseGuard：部署/启动/查询/worker 必须绑定 warehouseId
- StartCoordinator：执行 StartGateChain 决策后启动流程
- External Task Worker：topic → handler；handler 调用 wcs-execution

### 2.6 wcs-execution（编排执行）

- InstructionRunner：按 Instruction 顺序执行 Command 列表
- CommandPipeline：执行链（校验→锁→下发→等待→审计→释放→回写 CommandExecution）
- ExceptionDispatcher：按 chain 调用异常插件输出处置决策

### 2.7 wcs-adapters

- 设备/三方接入实现：S7、Modbus、RCS HTTP、OPC UA（域枚举见 `CommandDomain`）；工业 IO 实现包 **`cn.aitplus.wcs.adapters.io`**
- 连接复用、心跳、重连、轮询/回调封装
- 上层通过 wcs-core 定义的抽象（Facade/Client）访问

### 2.8 wcs-plugins-*（插件模块）

- 插件模块提供该域的插件实现（Spring Bean）
- 插件模块只依赖 wcs-core（避免循环依赖）
- 插件职责：决策/扩展/处置，尽量不直接做 IO（IO 在 execution/adapters 完成）

---

## 3. 模块依赖关系

```text
wcs-app        → wcs-common, wcs-core, wcs-infra, wcs-workflow, wcs-execution
wcs-workflow   → wcs-core, wcs-infra, wcs-execution
wcs-execution  → wcs-core, wcs-infra, wcs-adapters
wcs-adapters   → wcs-core
wcs-infra      → wcs-common, wcs-core
wcs-plugins-*  → wcs-core
wcs-core       → wcs-common
wcs-common     → (无业务依赖)
```

---

## 4. 当前已实现模块

| 模块 | 状态 | 说明 |
|------|------|------|
| wcs-common | 已实现 | AjaxResult、TableDataInfo、PageUtils、AESUtil、EncryptUtils、SpringUtils、BusinessException |
| wcs-core | 已实现 | Task、SubTask、Instruction、Command 实体；Swagger 注解 |
| wcs-app | 已实现 | WcsApplication、SwaggerConfig、GlobalExceptionHandler、TasksController、SubTasksController、InstructionsController |
| wcs-infra | 已实现 | TasksMapper/Service、SubTasksMapper/Service、InstructionsMapper/Service；多数据库 XML |
| wcs-workflow | 待实现 | Camunda 集成 |
| wcs-execution | 待实现 | Instruction/Command 执行编排 |
| wcs-adapters | 待实现 | S7/Modbus/RCS 适配 |
| wcs-plugins-* | 待实现 | 各域插件 |
