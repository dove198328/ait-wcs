-- 示例数据：warehouse_id = 1
-- 可重复执行（where not exists + merge 语义）

-- 1) profile 主配置
if exists (select 1 from wcs_warehouse_profile where warehouse_id = 1)
begin
    update wcs_warehouse_profile
    set warehouse_type = 'ASRS',
        enabled_plugins_json = '["startgate","routing","planning","dispatch","exception"]',
        params_json = '{"timeout":{"ackMs":2000,"doneMs":120000},"retry":{"maxRetries":3,"backoffMs":1000},"lock":{"defaultExpireSeconds":30}}',
        active = 1,
        updated_at = getdate()
    where warehouse_id = 1;
end
else
begin
    insert into wcs_warehouse_profile (
        warehouse_id, warehouse_type, enabled_plugins_json, params_json, version, active, created_at, updated_at
    )
    values (
        1, 'ASRS', '["startgate","routing","planning","dispatch","exception"]',
        '{"timeout":{"ackMs":2000,"doneMs":120000},"retry":{"maxRetries":3,"backoffMs":1000},"lock":{"defaultExpireSeconds":30}}',
        1, 1, getdate(), getdate()
    );
end;

-- 2) profile 责任链节点（逐条 upsert）
merge wcs_profile_chain_node as t
using (select 1 warehouse_id, 'startGateChain' chain_name, 1 node_order, 'DeviceOnlinePolicy' bean_name, 1 enabled) s
on t.warehouse_id = s.warehouse_id and t.chain_name = s.chain_name and t.node_order = s.node_order
when matched then update set bean_name = s.bean_name, enabled = s.enabled, updated_at = getdate()
when not matched then insert (warehouse_id, chain_name, node_order, bean_name, enabled, created_at, updated_at)
values (s.warehouse_id, s.chain_name, s.node_order, s.bean_name, s.enabled, getdate(), getdate());

merge wcs_profile_chain_node as t
using (select 1 warehouse_id, 'startGateChain' chain_name, 2 node_order, 'ConcurrencyLimitPolicy' bean_name, 1 enabled) s
on t.warehouse_id = s.warehouse_id and t.chain_name = s.chain_name and t.node_order = s.node_order
when matched then update set bean_name = s.bean_name, enabled = s.enabled, updated_at = getdate()
when not matched then insert (warehouse_id, chain_name, node_order, bean_name, enabled, created_at, updated_at)
values (s.warehouse_id, s.chain_name, s.node_order, s.bean_name, s.enabled, getdate(), getdate());

merge wcs_profile_chain_node as t
using (select 1 warehouse_id, 'routingChain' chain_name, 1 node_order, 'PortModePolicy' bean_name, 1 enabled) s
on t.warehouse_id = s.warehouse_id and t.chain_name = s.chain_name and t.node_order = s.node_order
when matched then update set bean_name = s.bean_name, enabled = s.enabled, updated_at = getdate()
when not matched then insert (warehouse_id, chain_name, node_order, bean_name, enabled, created_at, updated_at)
values (s.warehouse_id, s.chain_name, s.node_order, s.bean_name, s.enabled, getdate(), getdate());

merge wcs_profile_chain_node as t
using (select 1 warehouse_id, 'routingChain' chain_name, 2 node_order, 'DirectionExclusionPolicy' bean_name, 1 enabled) s
on t.warehouse_id = s.warehouse_id and t.chain_name = s.chain_name and t.node_order = s.node_order
when matched then update set bean_name = s.bean_name, enabled = s.enabled, updated_at = getdate()
when not matched then insert (warehouse_id, chain_name, node_order, bean_name, enabled, created_at, updated_at)
values (s.warehouse_id, s.chain_name, s.node_order, s.bean_name, s.enabled, getdate(), getdate());

merge wcs_profile_chain_node as t
using (select 1 warehouse_id, 'planningExpandChain' chain_name, 1 node_order, 'AisleChangeExpander' bean_name, 1 enabled) s
on t.warehouse_id = s.warehouse_id and t.chain_name = s.chain_name and t.node_order = s.node_order
when matched then update set bean_name = s.bean_name, enabled = s.enabled, updated_at = getdate()
when not matched then insert (warehouse_id, chain_name, node_order, bean_name, enabled, created_at, updated_at)
values (s.warehouse_id, s.chain_name, s.node_order, s.bean_name, s.enabled, getdate(), getdate());

merge wcs_profile_chain_node as t
using (select 1 warehouse_id, 'planningExpandChain' chain_name, 2 node_order, 'DoubleDeepRelocationExpander' bean_name, 1 enabled) s
on t.warehouse_id = s.warehouse_id and t.chain_name = s.chain_name and t.node_order = s.node_order
when matched then update set bean_name = s.bean_name, enabled = s.enabled, updated_at = getdate()
when not matched then insert (warehouse_id, chain_name, node_order, bean_name, enabled, created_at, updated_at)
values (s.warehouse_id, s.chain_name, s.node_order, s.bean_name, s.enabled, getdate(), getdate());

merge wcs_profile_chain_node as t
using (select 1 warehouse_id, 'commandPipelineChain' chain_name, 1 node_order, 'AcquireLockStep' bean_name, 1 enabled) s
on t.warehouse_id = s.warehouse_id and t.chain_name = s.chain_name and t.node_order = s.node_order
when matched then update set bean_name = s.bean_name, enabled = s.enabled, updated_at = getdate()
when not matched then insert (warehouse_id, chain_name, node_order, bean_name, enabled, created_at, updated_at)
values (s.warehouse_id, s.chain_name, s.node_order, s.bean_name, s.enabled, getdate(), getdate());

merge wcs_profile_chain_node as t
using (select 1 warehouse_id, 'commandPipelineChain' chain_name, 2 node_order, 'DispatchCommandStep' bean_name, 1 enabled) s
on t.warehouse_id = s.warehouse_id and t.chain_name = s.chain_name and t.node_order = s.node_order
when matched then update set bean_name = s.bean_name, enabled = s.enabled, updated_at = getdate()
when not matched then insert (warehouse_id, chain_name, node_order, bean_name, enabled, created_at, updated_at)
values (s.warehouse_id, s.chain_name, s.node_order, s.bean_name, s.enabled, getdate(), getdate());

merge wcs_profile_chain_node as t
using (select 1 warehouse_id, 'commandPipelineChain' chain_name, 3 node_order, 'AuditStep' bean_name, 1 enabled) s
on t.warehouse_id = s.warehouse_id and t.chain_name = s.chain_name and t.node_order = s.node_order
when matched then update set bean_name = s.bean_name, enabled = s.enabled, updated_at = getdate()
when not matched then insert (warehouse_id, chain_name, node_order, bean_name, enabled, created_at, updated_at)
values (s.warehouse_id, s.chain_name, s.node_order, s.bean_name, s.enabled, getdate(), getdate());

merge wcs_profile_chain_node as t
using (select 1 warehouse_id, 'exceptionHandlingChain' chain_name, 1 node_order, 'DropOccupiedHandler' bean_name, 1 enabled) s
on t.warehouse_id = s.warehouse_id and t.chain_name = s.chain_name and t.node_order = s.node_order
when matched then update set bean_name = s.bean_name, enabled = s.enabled, updated_at = getdate()
when not matched then insert (warehouse_id, chain_name, node_order, bean_name, enabled, created_at, updated_at)
values (s.warehouse_id, s.chain_name, s.node_order, s.bean_name, s.enabled, getdate(), getdate());

merge wcs_profile_chain_node as t
using (select 1 warehouse_id, 'exceptionHandlingChain' chain_name, 2 node_order, 'PickEmptyHandler' bean_name, 1 enabled) s
on t.warehouse_id = s.warehouse_id and t.chain_name = s.chain_name and t.node_order = s.node_order
when matched then update set bean_name = s.bean_name, enabled = s.enabled, updated_at = getdate()
when not matched then insert (warehouse_id, chain_name, node_order, bean_name, enabled, created_at, updated_at)
values (s.warehouse_id, s.chain_name, s.node_order, s.bean_name, s.enabled, getdate(), getdate());

-- 3) 资源锁示例
merge wcs_resource_lock as t
using (
    select 1 warehouse_id, 'AISLE:3' lock_key, 'SUBTASK' owner_type, '2001' owner_id, 'HELD' status,
           dateadd(second, 30, getdate()) expire_at
) s
on t.warehouse_id = s.warehouse_id and t.lock_key = s.lock_key
when matched then update
    set owner_type = s.owner_type,
        owner_id = s.owner_id,
        status = s.status,
        expire_at = s.expire_at,
        updated_at = getdate()
when not matched then insert (warehouse_id, lock_key, owner_type, owner_id, status, expire_at, created_at, updated_at)
    values (s.warehouse_id, s.lock_key, s.owner_type, s.owner_id, s.status, s.expire_at, getdate(), getdate());

merge wcs_resource_lock as t
using (
    select 1 warehouse_id, 'STATION:IN-01' lock_key, 'TASK' owner_type, '1001' owner_id, 'RELEASED' status,
           cast(null as datetime) expire_at
) s
on t.warehouse_id = s.warehouse_id and t.lock_key = s.lock_key
when matched then update
    set owner_type = s.owner_type,
        owner_id = s.owner_id,
        status = s.status,
        expire_at = s.expire_at,
        updated_at = getdate()
when not matched then insert (warehouse_id, lock_key, owner_type, owner_id, status, expire_at, created_at, updated_at)
    values (s.warehouse_id, s.lock_key, s.owner_type, s.owner_id, s.status, s.expire_at, getdate(), getdate());

-- 4) 指令执行审计示例
merge wcs_command_execution as t
using (
    select
        1 warehouse_id, cast(1001 as bigint) task_id, cast(2001 as bigint) subtask_id, cast(3001 as bigint) instruction_id,
        'S7' domain, 'PLC-01' device_id, 'WRITE_POINT' command_type,
        'W1-T1001-S2001-I3001-CMD-001' idempotency_key, 'DONE' status,
        N'{"point":"DB1.0","value":1}' request_json, N'{"ack":true,"done":true}' response_json,
        cast(null as varchar(64)) error_code, cast(null as varchar(512)) error_message,
        'TRACE-1001' trace_id, 'CORR-1001' correlation_id,
        dateadd(second, -5, getdate()) started_at, dateadd(second, -1, getdate()) ended_at
) s
on t.warehouse_id = s.warehouse_id and t.idempotency_key = s.idempotency_key
when matched then update
    set status = s.status,
        response_json = s.response_json,
        error_code = s.error_code,
        error_message = s.error_message,
        ended_at = s.ended_at,
        updated_at = getdate()
when not matched then insert (
        warehouse_id, task_id, subtask_id, instruction_id, domain, device_id, command_type, idempotency_key, status,
        request_json, response_json, error_code, error_message, trace_id, correlation_id, started_at, ended_at, created_at, updated_at
    )
    values (
        s.warehouse_id, s.task_id, s.subtask_id, s.instruction_id, s.domain, s.device_id, s.command_type, s.idempotency_key, s.status,
        s.request_json, s.response_json, s.error_code, s.error_message, s.trace_id, s.correlation_id, s.started_at, s.ended_at, getdate(), getdate()
    );

merge wcs_command_execution as t
using (
    select
        1 warehouse_id, cast(1002 as bigint) task_id, cast(2002 as bigint) subtask_id, cast(3002 as bigint) instruction_id,
        'RCS' domain, 'AGV-07' device_id, 'MOVE' command_type,
        'W1-T1002-S2002-I3002-CMD-001' idempotency_key, 'ERROR' status,
        N'{"from":"A01","to":"B03"}' request_json, N'{"code":"RCS-500"}' response_json,
        'RCS-500' error_code, 'RCS temporary failure' error_message,
        'TRACE-1002' trace_id, 'CORR-1002' correlation_id,
        dateadd(second, -8, getdate()) started_at, dateadd(second, -2, getdate()) ended_at
) s
on t.warehouse_id = s.warehouse_id and t.idempotency_key = s.idempotency_key
when matched then update
    set status = s.status,
        response_json = s.response_json,
        error_code = s.error_code,
        error_message = s.error_message,
        ended_at = s.ended_at,
        updated_at = getdate()
when not matched then insert (
        warehouse_id, task_id, subtask_id, instruction_id, domain, device_id, command_type, idempotency_key, status,
        request_json, response_json, error_code, error_message, trace_id, correlation_id, started_at, ended_at, created_at, updated_at
    )
    values (
        s.warehouse_id, s.task_id, s.subtask_id, s.instruction_id, s.domain, s.device_id, s.command_type, s.idempotency_key, s.status,
        s.request_json, s.response_json, s.error_code, s.error_message, s.trace_id, s.correlation_id, s.started_at, s.ended_at, getdate(), getdate()
    );
