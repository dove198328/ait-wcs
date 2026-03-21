if object_id('wcs_command_execution', 'U') is null
begin
    create table wcs_command_execution (
        id bigint identity(1,1) primary key,
        warehouse_id bigint not null,
        task_id bigint null,
        subtask_id bigint null,
        instruction_id bigint null,
        domain varchar(32) not null,
        device_id varchar(64) not null,
        command_type varchar(64) not null,
        idempotency_key varchar(256) not null,
        status varchar(32) not null,
        request_json nvarchar(max) null,
        response_json nvarchar(max) null,
        error_code varchar(64) null,
        error_message varchar(512) null,
        trace_id varchar(64) null,
        correlation_id varchar(64) null,
        started_at datetime null,
        ended_at datetime null,
        created_at datetime not null default getdate(),
        updated_at datetime not null default getdate(),
        constraint uk_cmd_idempotency unique (warehouse_id, idempotency_key)
    );
end;

if not exists (select 1 from sys.indexes where name = 'idx_cmd_warehouse_status' and object_id = object_id('wcs_command_execution'))
    create index idx_cmd_warehouse_status on wcs_command_execution (warehouse_id, status);

if not exists (select 1 from sys.indexes where name = 'idx_cmd_warehouse_device' and object_id = object_id('wcs_command_execution'))
    create index idx_cmd_warehouse_device on wcs_command_execution (warehouse_id, device_id, created_at desc);

if not exists (select 1 from sys.indexes where name = 'idx_cmd_warehouse_instruction' and object_id = object_id('wcs_command_execution'))
    create index idx_cmd_warehouse_instruction on wcs_command_execution (warehouse_id, instruction_id);

if not exists (select 1 from sys.indexes where name = 'idx_cmd_warehouse_task' and object_id = object_id('wcs_command_execution'))
    create index idx_cmd_warehouse_task on wcs_command_execution (warehouse_id, task_id);
