-- 示例数据：warehouse_id = 1
-- 可重复执行（使用 upsert / where not exists）

-- 1) profile 主配置
insert into wcs_warehouse_profile (
    warehouse_id, warehouse_type, enabled_plugins_json, params_json, version, active, created_at, updated_at
)
values (
    1,
    'ASRS',
    '["startgate","routing","planning","dispatch","exception"]'::jsonb,
    '{
      "timeout": {"ackMs": 2000, "doneMs": 120000},
      "retry": {"maxRetries": 3, "backoffMs": 1000},
      "lock": {"defaultExpireSeconds": 30}
    }'::jsonb,
    1,
    true,
    now(),
    now()
)
on conflict (warehouse_id) do update
set warehouse_type = excluded.warehouse_type,
    enabled_plugins_json = excluded.enabled_plugins_json,
    params_json = excluded.params_json,
    active = excluded.active,
    updated_at = now();

-- 测试运行
INSERT INTO wcs_warehouse_profile (
    warehouse_id, warehouse_type, enabled_plugins_json, params_json, version, active, created_at, updated_at
)
VALUES (
           1,
           'ASRS',
           '["startgate","routing","planning","dispatch","exception"]',
           '{
             "timeout": {"ackMs": 2000, "doneMs": 120000},
             "retry": {"maxRetries": 3, "backoffMs": 1000},
             "lock": {"defaultExpireSeconds": 30}
           }',
           1,
           true,
           now(),
           now()
       )
    ON DUPLICATE KEY UPDATE
                         warehouse_type = VALUES(warehouse_type),
                         enabled_plugins_json = VALUES(enabled_plugins_json),
                         params_json = VALUES(params_json),
                         active = VALUES(active),
                         updated_at = now();


-- 2) profile 责任链节点
insert into wcs_profile_chain_node (warehouse_id, chain_name, node_order, bean_name, enabled, created_at, updated_at)
values
    (1, 'startGateChain', 1, 'DeviceOnlinePolicy', true, now(), now()),
    (1, 'startGateChain', 2, 'ConcurrencyLimitPolicy', true, now(), now()),
    (1, 'routingChain', 1, 'PortModePolicy', true, now(), now()),
    (1, 'routingChain', 2, 'DirectionExclusionPolicy', true, now(), now()),
    (1, 'planningExpandChain', 1, 'AisleChangeExpander', true, now(), now()),
    (1, 'planningExpandChain', 2, 'DoubleDeepRelocationExpander', true, now(), now()),
    (1, 'commandPipelineChain', 1, 'AcquireLockStep', true, now(), now()),
    (1, 'commandPipelineChain', 2, 'DispatchCommandStep', true, now(), now()),
    (1, 'commandPipelineChain', 3, 'AuditStep', true, now(), now()),
    (1, 'exceptionHandlingChain', 1, 'DropOccupiedHandler', true, now(), now()),
    (1, 'exceptionHandlingChain', 2, 'PickEmptyHandler', true, now(), now())
on conflict (warehouse_id, chain_name, node_order) do update
set bean_name = excluded.bean_name,
    enabled = excluded.enabled,
    updated_at = now();


-- 测试运行
INSERT INTO wcs_profile_chain_node (
    warehouse_id, chain_name, node_order, bean_name, enabled, created_at, updated_at
)
VALUES
    (1, 'startGateChain', 1, 'DeviceOnlinePolicy', true, now(), now()),
    (1, 'startGateChain', 2, 'ConcurrencyLimitPolicy', true, now(), now()),
    (1, 'routingChain', 1, 'PortModePolicy', true, now(), now()),
    (1, 'routingChain', 2, 'DirectionExclusionPolicy', true, now(), now()),
    (1, 'planningExpandChain', 1, 'AisleChangeExpander', true, now(), now()),
    (1, 'planningExpandChain', 2, 'DoubleDeepRelocationExpander', true, now(), now()),
    (1, 'commandPipelineChain', 1, 'AcquireLockStep', true, now(), now()),
    (1, 'commandPipelineChain', 2, 'DispatchCommandStep', true, now(), now()),
    (1, 'commandPipelineChain', 3, 'AuditStep', true, now(), now()),
    (1, 'exceptionHandlingChain', 1, 'DropOccupiedHandler', true, now(), now()),
    (1, 'exceptionHandlingChain', 2, 'PickEmptyHandler', true, now(), now())
    ON DUPLICATE KEY UPDATE
                         bean_name = VALUES(bean_name),
                         enabled = VALUES(enabled),
                         updated_at = now();

-- 3) 资源锁示例
insert into wcs_resource_lock (
    warehouse_id, lock_key, owner_type, owner_id, status, expire_at, created_at, updated_at
)
values
    (1, 'AISLE:3', 'SUBTASK', '2001', 'HELD', now() + interval '30 seconds', now(), now()),
    (1, 'STATION:IN-01', 'TASK', '1001', 'RELEASED', null, now(), now())
on conflict (warehouse_id, lock_key) do update
set owner_type = excluded.owner_type,
    owner_id = excluded.owner_id,
    status = excluded.status,
    expire_at = excluded.expire_at,
    updated_at = now();

-- 测试运行
INSERT INTO wcs_resource_lock (
    warehouse_id, lock_key, owner_type, owner_id, status, expire_at, created_at, updated_at
)
VALUES
    (1, 'AISLE:3', 'SUBTASK', '2001', 'HELD', now() + interval '30 seconds', now(), now()),
    (1, 'STATION:IN-01', 'TASK', '1001', 'RELEASED', null, now(), now())
    ON DUPLICATE KEY UPDATE
                         owner_type = VALUES(owner_type),
                         owner_id = VALUES(owner_id),
                         status = VALUES(status),
                         expire_at = VALUES(expire_at),
                         updated_at = now();

-- 4) 指令执行审计示例
insert into wcs_command_execution (
    warehouse_id, task_id, subtask_id, instruction_id, domain, device_id, command_type, idempotency_key, status,
    request_json, response_json, error_code, error_message, trace_id, correlation_id, started_at, ended_at, created_at, updated_at
)
values
    (
        1, 1001, 2001, 3001, 'S7', 'PLC-01', 'WRITE_POINT', 'W1-T1001-S2001-I3001-CMD-001', 'DONE',
        '{"point":"DB1.0","value":1}'::jsonb, '{"ack":true,"done":true}'::jsonb, null, null, 'TRACE-1001', 'CORR-1001',
        now() - interval '5 seconds', now() - interval '1 second', now(), now()
    ),
    (
        1, 1002, 2002, 3002, 'RCS', 'AGV-07', 'MOVE', 'W1-T1002-S2002-I3002-CMD-001', 'ERROR',
        '{"from":"A01","to":"B03"}'::jsonb, '{"code":"RCS-500"}'::jsonb, 'RCS-500', 'RCS temporary failure',
        'TRACE-1002', 'CORR-1002', now() - interval '8 seconds', now() - interval '2 seconds', now(), now()
    )
on conflict (warehouse_id, idempotency_key) do update
set status = excluded.status,
    response_json = excluded.response_json,
    error_code = excluded.error_code,
    error_message = excluded.error_message,
    ended_at = excluded.ended_at,
    updated_at = now();

-- 测试运行
INSERT INTO wcs_command_execution (
    warehouse_id, task_id, subtask_id, instruction_id, domain, device_id, command_type, idempotency_key, status,
    request_json, response_json, error_code, error_message, trace_id, correlation_id, started_at, ended_at, created_at, updated_at
)
VALUES
    (
        1, 1001, 2001, 3001, 'S7', 'PLC-01', 'WRITE_POINT', 'W1-T1001-S2001-I3001-CMD-001', 'DONE',
        '{"point":"DB1.0","value":1}', '{"ack":true,"done":true}', null, null, 'TRACE-1001', 'CORR-1001',
        now() - interval '5 seconds', now() - interval '1 second', now(), now()
    ),
    (
        1, 1002, 2002, 3002, 'RCS', 'AGV-07', 'MOVE', 'W1-T1002-S2002-I3002-CMD-001', 'ERROR',
        '{"from":"A01","to":"B03"}', '{"code":"RCS-500"}', 'RCS-500', 'RCS temporary failure',
        'TRACE-1002', 'CORR-1002', now() - interval '8 seconds', now() - interval '2 seconds', now(), now()
    )
    ON DUPLICATE KEY UPDATE
                         status = VALUES(status),
                         response_json = VALUES(response_json),
                         error_code = VALUES(error_code),
                         error_message = VALUES(error_message),
                         ended_at = VALUES(ended_at),
                         updated_at = now();
