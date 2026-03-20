# 06 - Module Structure (Plugins as Modules)

> 目的：定义 WCS 平台的代码模块结构（插件以独立模块拆分），并明确各模块如何在编译期与运行时进行组装。  
> 关键约束：Camunda 7.19 Embedded；以 `warehouseId` 作为仓库上下文；插件可按 warehouse 配置启用/禁用与排序。

---

## 1. 推荐的顶层模块结构（插件独立模块）

```text
wcs-platform/
├─ wcs-app/                          # Spring Boot 启动器 + API 入口
├─ wcs-core/                         # 领域模型、状态机、上下文、SPI接口、ChainRunner
├─ wcs-workflow/                     # Camunda embedded、WarehouseGuard、StartCoordinator、Worker
├─ wcs-execution/                    # Plan/Step/Command 执行编排（不放策略实现）
├─ wcs-adapters/                     # S7/Modbus/RCS 适配实现
├─ wcs-infra/                        # DB/Redis/锁/审计/迁移脚本 + Profile 存储
├─ wcs-plugins-startgate/            # 插件模块：启动门禁策略
├─ wcs-plugins-routing/              # 插件模块：站台/口路由（同入同出）
├─ wcs-plugins-planning/             # 插件模块：计划扩展（换巷道、双深位临时移库）
├─ wcs-plugins-dispatch/             # 插件模块：调度策略（优先级/公平/饥饿控制）
├─ wcs-plugins-exception/            # 插件模块：异常处理（放货有货/取货无货/通信异常）
└─ docs/                             # 规格文档
```
## 2. 模块职责边界
### 2.1 wcs-app
Spring Boot 启动入口
REST API：任务创建/查询/取消/人工干预/设备状态
warehouse 解析：从 path 或 header（例如 X-Warehouse-Id）提取 warehouseId，并建立 WarehouseContext
### 2.2 wcs-core（稳定内核）
领域对象：Task/Plan/Step/CommandExecution/ResourceLock
状态机与领域事件
插件 SPI 接口（仅接口，不包含实现）
ChainRunner：按 warehouse profile 装配并执行插件链（编排引擎）
上下文对象：StartContext / RoutingContext / PlanContext / ExecContext / ExceptionContext 等

### 2.3 wcs-workflow
Camunda 7.19 Embedded 集成
WarehouseGuard：部署/启动/查询/worker 必须绑定 warehouseId
StartCoordinator：执行 StartGateChain 决策后启动流程
External Task Worker：topic -> handler；handler 调用 wcs-execution
### 2.4 wcs-execution（编排执行）
PlanGenerator：按任务类型生成初始 Plan
PlanExpanderRunner：按 chain 执行 planning 插件扩展 Plan（插入换巷道/临时移库等步骤）
CommandPipeline：执行链（校验→锁→下发→等待→审计→释放→回写）
ExceptionDispatcher：按 chain 调用异常插件输出处置决策
### 2.5 wcs-adapters
设备/三方接入实现：S7、Modbus、RCS HTTP
连接复用、心跳、重连、轮询/回调封装
上层通过 wcs-core 定义的抽象（Facade/Client）访问
### 2.6 wcs-infra
DB schema（Flyway/Liquibase）、Repository/Mapper
Redis、分布式锁实现
审计与事件发布
WarehouseProfile 的存储与读取（按 warehouseId）
### 2.7 wcs-plugins-*（插件模块）
插件模块提供该域的插件实现（Spring Bean）
插件模块只依赖 wcs-core（避免循环依赖）
插件职责：决策/扩展/处置，尽量不直接做 IO（IO 在 execution/adapters 完成）
## 3 模块依赖关系
wcs-app -> wcs-workflow, wcs-execution, wcs-infra, wcs-core
wcs-workflow -> wcs-execution, wcs-infra, wcs-core
wcs-execution -> wcs-adapters, wcs-infra, wcs-core
wcs-adapters -> wcs-core
wcs-infra -> wcs-core
wcs-plugins-* -> wcs-core
wcs-core -> (no dependency to business implementation)




