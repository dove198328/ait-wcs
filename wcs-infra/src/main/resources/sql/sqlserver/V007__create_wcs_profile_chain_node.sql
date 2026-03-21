if object_id('wcs_profile_chain_node', 'U') is null
begin
    create table wcs_profile_chain_node (
        id bigint identity(1,1) primary key,
        warehouse_id bigint not null,
        chain_name varchar(64) not null,
        node_order int not null,
        bean_name varchar(128) not null,
        enabled bit not null default 1,
        created_at datetime not null default getdate(),
        updated_at datetime not null default getdate(),
        constraint uk_chain_node unique (warehouse_id, chain_name, node_order)
    );
end;

if not exists (select 1 from sys.indexes where name = 'idx_chain_warehouse_chain' and object_id = object_id('wcs_profile_chain_node'))
    create index idx_chain_warehouse_chain on wcs_profile_chain_node (warehouse_id, chain_name, node_order);

if not exists (select 1 from sys.indexes where name = 'idx_chain_warehouse_bean' and object_id = object_id('wcs_profile_chain_node'))
    create index idx_chain_warehouse_bean on wcs_profile_chain_node (warehouse_id, bean_name);
