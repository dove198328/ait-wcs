create table if not exists wcs_command_execution (
  id bigserial primary key,
  warehouse_id bigint not null,
  task_id bigint null,
  subtask_id bigint null,
  instruction_id bigint null,
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
  constraint uk_cmd_idempotency unique (warehouse_id, idempotency_key)
);

create index if not exists idx_cmd_warehouse_status
  on wcs_command_execution (warehouse_id, status);

create index if not exists idx_cmd_warehouse_device
  on wcs_command_execution (warehouse_id, device_id, created_at desc);

create index if not exists idx_cmd_warehouse_instruction
  on wcs_command_execution (warehouse_id, instruction_id);

create index if not exists idx_cmd_warehouse_task
  on wcs_command_execution (warehouse_id, task_id);
