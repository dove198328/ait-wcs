# 05 - DB Schema

> 目标：定义最小可落地的数据模型，覆盖 tasks/subtasks/instructions + CommandExecution/ResourceLock 及 warehouse profile。  
> 约束：`warehouse_id` 强约束；所有查询默认按 warehouse 过滤；关键写入必须幂等。  
> 上层表（tasks/subtasks/instructions）保留当前已有结构；下层能力表（wcs_ 前缀）为新增松耦合组件。

缓存说明：
- 二级缓存只作为读取优化层，不改变持久化语义与事务边界
- `CacheType.BOTH` 场景下，本地缓存允许设置过期时间，Redis 二级缓存默认不设置过期时间
- 所有缓存写操作必须由增删改路径主动维护，不能依赖 Redis TTL 被动失效
- 租户与仓库一一对应：凡进入 Redis/本地二级的业务缓存 key 必须带租户维度（通常为 `warehouseId` 前缀或与主键组合，如 `warehouseId:entityId`），禁止仅依赖全局唯一 id 作为唯一维度

---

## 1. 命名与通用约定

- 上层业务表沿用现有命名：`tasks`、`subtasks`、`instructions`
- 下层能力表使用 `wcs_` 前缀：`wcs_command_execution`、`wcs_resource_lock`、`wcs_warehouse_profile`、`wcs_profile_chain_node`
- 所有业务表必须包含：`warehouse_id`（或通过关联表间接约束）、`created_at`、`updated_at`
- 状态字段使用字符串枚举（与 `wcs-core` 枚举一致）
- JSON 字段使用 `jsonb`（PostgreSQL）/ `json`（MySQL）/ `nvarchar(max)`（SQL Server）
- 时间统一使用 `timestamp with time zone`（PostgreSQL）/ `datetime`（SQL Server）
- 主键默认 `bigserial`（PostgreSQL）/ `bigint identity`（SQL Server）
- 支持多数据库方言，Mapper XML 通过 `_databaseId` 分支处理

---

## 2. 上层业务核心表

### 2.1 `tasks`

用途：主任务表（WMS 下发任务与流程实例映射）

```sql
-- PostgreSQL
create table if not exists tasks (
  id bigserial primary key,
  workflow_def_id varchar(128),
  task_name varchar(256) not null,
  priority int default 0,
  status varchar(32) not null,
  started_at timestamp with time zone,
  completed_at timestamp with time zone,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  warehouse_id bigint not null,
  biz_type varchar(32),
  start_point varchar(64),
  end_point varchar(64),
  task_direction varchar(32),
  task_number varchar(128),
  vehicle_id varchar(64),
  task_type varchar(32),
  task_phase varchar(32),
  process_instance_id varchar(64),
  is_auto_start int default 0,
  device_ids varchar(512),
  location varchar(64),
  depth varchar(16),
  front_empty boolean,
  work_direction varchar(32),
  wms_biz_id bigint,
  order_no varchar(128),
  aisle_no int,
  process_definition_id varchar(64),
  task_category varchar(32),
  twins_no varchar(64),
  scanner_device_id varchar(64),
  is_ck boolean default false,
  task_distribution int,
  rkk_flag boolean
);
```

索引建议：

```sql
create index if not exists idx_tasks_warehouse_status on tasks (warehouse_id, status);
create index if not exists idx_tasks_warehouse_created on tasks (warehouse_id, created_at desc);
create index if not exists idx_tasks_warehouse_endpoint on tasks (warehouse_id, end_point);
create index if not exists idx_tasks_task_number on tasks (task_number);
create index if not exists idx_tasks_process_instance on tasks (process_instance_id);
```

---

### 2.2 `subtasks`

用途：子任务表（主任务的执行步骤分解）

```sql
-- PostgreSQL
create table if not exists subtasks (
  id bigserial primary key,
  task_id bigint not null,
  subtask_def_id varchar(128),
  name varchar(256),
  priority int default 0,
  status varchar(32) not null,
  compensation varchar(64),
  allow_manual int default 0,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  warehouse_id bigint not null,
  current_instruction_index int default 0,
  completed_at timestamp with time zone,
  remark varchar(512),
  selected_device_id int,
  activity_instance_id varchar(64),
  area varchar(64),
  workflow_def_id varchar(128),
  is_start_next_process boolean default false,
  free_device_id varchar(64),
  check_aisle boolean default false,
  bind_ddj varchar(64),
  constraint fk_subtask_task foreign key (task_id) references tasks(id)
);
```

索引建议：

```sql
create index if not exists idx_subtasks_warehouse_task on subtasks (warehouse_id, task_id);
create index if not exists idx_subtasks_warehouse_status on subtasks (warehouse_id, status);
create index if not exists idx_subtasks_task_def on subtasks (task_id, subtask_def_id);
```

---

### 2.3 `instructions`

用途：指令表（子任务的具体执行指令，含设备命令列表）

```sql
-- PostgreSQL
create table if not exists instructions (
  id bigserial primary key,
  subtask_id bigint not null,
  task_id bigint not null,
  sequence int not null,
  protocol varchar(32) not null,
  commands jsonb,
  params text,
  status varchar(32) not null,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  remark varchar(512),
  device_id varchar(64),
  message_event boolean default false,
  event_logic varchar(16),
  constraint fk_instruction_subtask foreign key (subtask_id) references subtasks(id),
  constraint uk_instruction_seq unique (subtask_id, sequence)
);
```

> `commands` 列存储 `List<Command>` 的 JSON 数组，通过 `CommandListTypeHandler` 在 MyBatis 中自动序列化/反序列化。  
> `instructions` 表不含 `warehouse_id`，通过 `subtask_id → subtasks.warehouse_id` 间接约束。

索引建议：

```sql
create index if not exists idx_instructions_subtask on instructions (subtask_id, sequence);
create index if not exists idx_instructions_task on instructions (task_id);
create index if not exists idx_instructions_status on instructions (status);
```

---

## 3. 下层能力表（新增）

### 3.1 `wcs_command_execution`

用途：设备/三方指令执行与审计（松耦合，通过 ID 引用上层实体）

```sql
create table if not exists wcs_command_execution (
  id bigserial primary key,
  warehouse_id varchar(64) not null,
  task_id bigint,
  subtask_id bigint,
  instruction_id bigint,
  domain varchar(32) not null,
  device_id varchar(64) not null,
  command_type varchar(64) not null,
  idempotency_key varchar(256) not null,
  status varchar(32) not null,
  request_json jsonb,
  response_json jsonb,
  error_code varchar(64),
  error_message varchar(512),
  trace_id varchar(64),
  correlation_id varchar(64),
  started_at timestamp with time zone,
  ended_at timestamp with time zone,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint uk_cmd_idempotency unique (warehouse_id, idempotency_key)
);
```

> 注意：`task_id`、`subtask_id`、`instruction_id` 为逻辑引用，不设硬外键，以保持松耦合。

索引建议：

```sql
create index if not exists idx_cmd_warehouse_status on wcs_command_execution (warehouse_id, status);
create index if not exists idx_cmd_warehouse_device on wcs_command_execution (warehouse_id, device_id, created_at desc);
create index if not exists idx_cmd_warehouse_instruction on wcs_command_execution (warehouse_id, instruction_id);
create index if not exists idx_cmd_warehouse_task on wcs_command_execution (warehouse_id, task_id);
```

---

### 3.2 `wcs_resource_lock`

用途：资源锁（站台、巷道、货位、设备等）

```sql
create table if not exists wcs_resource_lock (
  id bigserial primary key,
  warehouse_id varchar(64) not null,
  lock_key varchar(128) not null,
  owner_type varchar(32) not null,
  owner_id varchar(64) not null,
  status varchar(32) not null,
  expire_at timestamp with time zone,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint uk_lock_key unique (warehouse_id, lock_key)
);
```

> `owner_type`：TASK / SUBTASK / INSTRUCTION，对应上层不同粒度的持有者。

索引建议：

```sql
create index if not exists idx_lock_warehouse_owner on wcs_resource_lock (warehouse_id, owner_type, owner_id);
create index if not exists idx_lock_warehouse_expire on wcs_resource_lock (warehouse_id, expire_at);
```

---

## 4. 配置与编排表

### 4.1 `wcs_warehouse_profile`

用途：warehouse 级 profile 主配置

```sql
create table if not exists wcs_warehouse_profile (
  id bigserial primary key,
  warehouse_id varchar(64) not null,
  warehouse_type varchar(32) not null,
  enabled_plugins_json jsonb not null,
  params_json jsonb not null default '{}'::jsonb,
  version int not null default 1,
  active boolean not null default true,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint uk_profile_warehouse unique (warehouse_id)
);
```

### 4.2 `wcs_profile_chain_node`

用途：profile 中各 chain 的节点顺序

```sql
create table if not exists wcs_profile_chain_node (
  id bigserial primary key,
  warehouse_id varchar(64) not null,
  chain_name varchar(64) not null,
  node_order int not null,
  bean_name varchar(128) not null,
  enabled boolean not null default true,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint uk_chain_node unique (warehouse_id, chain_name, node_order)
);
```

索引建议：

```sql
create index if not exists idx_chain_warehouse_chain on wcs_profile_chain_node (warehouse_id, chain_name, node_order);
create index if not exists idx_chain_warehouse_bean on wcs_profile_chain_node (warehouse_id, bean_name);
```

---

## 4.3 `workflow_definitions`

用途：流程定义表（业务流程配置 + Camunda deployment 元信息）

```sql
create table if not exists workflow_definitions (
  id bigserial primary key,
  warehouse_id bigint not null,
  biz_type varchar(64) not null,
  workflow_id varchar(128) not null,
  config text not null,
  process_data text,
  process_data_with_device jsonb,
  name varchar(256) not null,
  priority int default 0,
  is_auto_start int default 0,
  first_sub_def varchar(128),
  status int not null default 1,
  process_def_id varchar(128),
  deploy_id varchar(128),
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now()
);
```

约束建议：

```sql
create unique index if not exists uk_workflow_def_warehouse_biz_type on workflow_definitions (warehouse_id, biz_type);
create unique index if not exists uk_workflow_def_warehouse_name on workflow_definitions (warehouse_id, name);
create unique index if not exists uk_workflow_def_warehouse_workflow_id on workflow_definitions (warehouse_id, workflow_id);
create index if not exists idx_workflow_def_warehouse_process_def on workflow_definitions (warehouse_id, process_def_id);
```

说明：
- `config` 存 BPMN XML，写入前必须完成 BPMN 结构校验
- `process_data` 存流程 JSON 配置，非空时必须是合法 JSON
- `process_def_id`、`deploy_id` 在 Camunda 部署成功后回填
- 删除采用物理删除，但前提是不存在活跃任务和活跃流程实例

---

## 5. 状态枚举对齐

| 表 | 状态值 |
|----|--------|
| tasks.status | `pending \| executing \| completed \| suspended \| failed \| canceled` |
| subtasks.status | `pending \| executing \| completed \| failed \| skipped` |
| instructions.status | `pending \| executing \| completed \| failed \| skipped` |
| wcs_command_execution.status | `SENT \| ACK \| RUNNING \| DONE \| ERROR \| TIMEOUT \| CANCELED` |
| wcs_resource_lock.status | `HELD \| RELEASED \| EXPIRED` |
| workflow_definitions.status | `0 \| 1`（建议由领域 enum/常量统一约束其业务语义） |

---

## 6. 关键约束（必须）

- 所有 Repository/Mapper 查询必须显式带 `warehouse_id`（instructions 通过 EXISTS 子查询间接约束）
- 不允许跨 warehouse 外键"逻辑复用"，跨仓库视为数据污染
- 幂等入口保护：指令层 `uk_instruction_seq`（subtask 内 sequence 唯一），执行层 `uk_cmd_idempotency`
- 锁语义：`uk_lock_key` 保证同一 warehouse 同一资源单持有者，`expire_at` 用于死锁回收且回收动作必须审计
- 下层能力表与上层业务表之间采用 **逻辑引用**（不设硬外键），保持松耦合
- 业务状态字面量、缓存 key 前缀、Camunda deployment source 等必须在代码中统一归口，禁止散落硬编码

---

## 7. 最小迁移顺序（Flyway/Liquibase）

1. `V001__create_tasks.sql` — 主任务表
2. `V002__create_subtasks.sql` — 子任务表
3. `V003__create_instructions.sql` — 指令表
4. `V004__create_wcs_command_execution.sql` — 指令执行审计表
5. `V005__create_wcs_resource_lock.sql` — 资源锁表
6. `V006__create_wcs_warehouse_profile.sql` — 仓库配置表
7. `V007__create_wcs_profile_chain_node.sql` — 责任链节点表
8. `V008__create_indexes.sql` — 索引与约束补充
