if object_id('wcs_warehouse_profile', 'U') is null
begin
    create table wcs_warehouse_profile (
        id bigint identity(1,1) primary key,
        warehouse_id bigint not null,
        warehouse_type varchar(32) not null,
        enabled_plugins_json nvarchar(max) not null,
        params_json nvarchar(max) not null default '{}',
        version int not null default 1,
        active bit not null default 1,
        created_at datetime not null default getdate(),
        updated_at datetime not null default getdate(),
        constraint uk_profile_warehouse unique (warehouse_id)
    );
end;
