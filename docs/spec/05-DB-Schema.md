# 05 - DB Schema

> 目标：定义最小可落地的数据模型，覆盖 Task/Plan/Step/CommandExecution/ResourceLock 及 warehouse profile。  
> 约束：`warehouseId` 强约束；所有查询默认按 warehouse 过滤；关键写入必须幂等。

---

## 1. 命名与通用约定

- 所有业务表必须包含：`warehouse_id`, `created_at`, `updated_at`
- 状态字段使用字符串枚举（与 `wcs-core` 枚举一致）
- JSON 字段使用 `jsonb`（若使用 MySQL 可替换为 `json`）
- 时间统一使用 `timestamp with time zone`
- 主键默认 `bigserial`（若项目统一雪花 ID，可改为 `bigint` + 应用层生成）

---

## 2. 业务核心表

### 2.1 `wcs_task`

用途：任务主表（WMS 下发任务与流程实例映射）

```sql
create table if not exists wcs_task (
  id bigserial primary key,
  warehouse_id varchar(64) not null,
  upstream_task_id varchar(128) not null,
  task_type varchar(32) not null,
  status varchar(32) not null,
  priority int not null default 0,
  business_key varchar(256) not null,
  payload_json jsonb null,
  process_instance_id varchar(64) null,
  trace_id varchar(64) null,
  correlation_id varchar(64) null,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint uk_task_business_key unique (warehouse_id, business_key)
);
```

索引建议：

```sql
create index if not exists idx_task_warehouse_status on wcs_task (warehouse_id, status);
create index if not exists idx_task_warehouse_created on wcs_task (warehouse_id, created_at desc);
create index if not exists idx_task_upstream on wcs_task (warehouse_id, upstream_task_id);
```

---

### 2.2 `wcs_plan`

用途：任务执行计划

```sql
create table if not exists wcs_plan (
  id bigserial primary key,
  warehouse_id varchar(64) not null,
  task_id bigint not null,
  status varchar(32) not null,
  version int not null default 0,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint fk_plan_task foreign key (task_id) references wcs_task(id)
);
```

索引建议：

```sql
create index if not exists idx_plan_warehouse_task on wcs_plan (warehouse_id, task_id);
create index if not exists idx_plan_warehouse_status on wcs_plan (warehouse_id, status);
```

---

### 2.3 `wcs_step`

用途：计划步骤（执行编排最小单元）

```sql
create table if not exists wcs_step (
  id bigserial primary key,
  warehouse_id varchar(64) not null,
  plan_id bigint not null,
  seq int not null,
  step_type varchar(64) not null,
  status varchar(32) not null,
  payload_json jsonb null,
  resource_locks_json jsonb null,
  retry_count int not null default 0,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint fk_step_plan foreign key (plan_id) references wcs_plan(id),
  constraint uk_step_seq unique (warehouse_id, plan_id, seq)
);
```

索引建议：

```sql
create index if not exists idx_step_warehouse_plan_status on wcs_step (warehouse_id, plan_id, status);
create index if not exists idx_step_warehouse_type on wcs_step (warehouse_id, step_type);
```

---

### 2.4 `wcs_command_execution`

用途：设备/三方指令执行与审计

```sql
create table if not exists wcs_command_execution (
  id bigserial primary key,
  warehouse_id varchar(64) not null,
  task_id bigint null,
  plan_id bigint null,
  step_id bigint null,
  domain varchar(32) not null,
  device_id varchar(64) not null,
  command_type varchar(64) not null,
  idempotency_key varchar(256) not null,
  status varchar(32) not null,
  request_json jsonb null,
  response_json jsonb null,
  error_code varchar(64) null,
  error_message varchar(512) null,
  trace_id varchar(64) null,
  correlation_id varchar(64) null,
  started_at timestamp with time zone null,
  ended_at timestamp with time zone null,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint uk_cmd_idempotency unique (warehouse_id, idempotency_key),
  constraint fk_cmd_task foreign key (task_id) references wcs_task(id),
  constraint fk_cmd_plan foreign key (plan_id) references wcs_plan(id),
  constraint fk_cmd_step foreign key (step_id) references wcs_step(id)
);
```

索引建议：

```sql
create index if not exists idx_cmd_warehouse_status on wcs_command_execution (warehouse_id, status);
create index if not exists idx_cmd_warehouse_device on wcs_command_execution (warehouse_id, device_id, created_at desc);
create index if not exists idx_cmd_warehouse_step on wcs_command_execution (warehouse_id, step_id);
```

---

### 2.5 `wcs_resource_lock`

用途：资源锁（站台、巷道、货位、设备等）

```sql
create table if not exists wcs_resource_lock (
  id bigserial primary key,
  warehouse_id varchar(64) not null,
  lock_key varchar(128) not null,
  owner_type varchar(32) not null,
  owner_id varchar(64) not null,
  status varchar(32) not null,
  expire_at timestamp with time zone null,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint uk_lock_key unique (warehouse_id, lock_key)
);
```

索引建议：

```sql
create index if not exists idx_lock_warehouse_owner on wcs_resource_lock (warehouse_id, owner_type, owner_id);
create index if not exists idx_lock_warehouse_expire on wcs_resource_lock (warehouse_id, expire_at);
```

---

## 3. 配置与编排表

### 3.1 `wcs_warehouse_profile`

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

### 3.2 `wcs_profile_chain_node`

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

## 4. 状态枚举对齐

- `wcs_task.status`：`CREATED|READY|RUNNING|SUSPENDED|FAILED|COMPLETED|CANCELED`
- `wcs_plan.status`：`CREATED|RUNNING|COMPLETED|FAILED`
- `wcs_step.status`：`PENDING|RUNNING|DONE|FAILED|SKIPPED`
- `wcs_command_execution.status`：`SENT|ACK|RUNNING|DONE|ERROR|TIMEOUT|CANCELED`

---

## 5. 关键约束（必须）

- 所有 Repository 查询必须显式带 `warehouse_id`
- 不允许跨 warehouse 外键“逻辑复用”，跨仓库视为数据污染
- 幂等入口至少两层保护：任务层 `uk_task_business_key`，指令层 `uk_cmd_idempotency`
- 锁语义：`uk_lock_key` 保证同一 warehouse 同一资源单持有者，`expire_at` 用于死锁回收且回收动作必须审计

---

## 6. 最小迁移顺序（Flyway/Liquibase）

1. `V001__create_wcs_task.sql`
2. `V002__create_wcs_plan_step.sql`
3. `V003__create_wcs_command_execution.sql`
4. `V004__create_wcs_resource_lock.sql`
5. `V005__create_wcs_profile.sql`
6. `V006__create_indexes_and_constraints.sql`



