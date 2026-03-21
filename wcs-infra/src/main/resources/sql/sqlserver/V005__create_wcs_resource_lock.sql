if object_id('wcs_resource_lock', 'U') is null
begin
    create table wcs_resource_lock (
        id bigint identity(1,1) primary key,
        warehouse_id bigint not null,
        lock_key varchar(128) not null,
        owner_type varchar(32) not null,
        owner_id varchar(64) not null,
        status varchar(32) not null,
        expire_at datetime null,
        created_at datetime not null default getdate(),
        updated_at datetime not null default getdate(),
        constraint uk_lock_key unique (warehouse_id, lock_key)
    );
end;

if not exists (select 1 from sys.indexes where name = 'idx_lock_warehouse_owner' and object_id = object_id('wcs_resource_lock'))
    create index idx_lock_warehouse_owner on wcs_resource_lock (warehouse_id, owner_type, owner_id);

if not exists (select 1 from sys.indexes where name = 'idx_lock_warehouse_expire' and object_id = object_id('wcs_resource_lock'))
    create index idx_lock_warehouse_expire on wcs_resource_lock (warehouse_id, expire_at);
