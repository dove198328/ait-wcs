create table if not exists wcs_profile_chain_node (
  id bigserial primary key,
  warehouse_id bigint not null,
  chain_name varchar(64) not null,
  node_order int not null,
  bean_name varchar(128) not null,
  enabled boolean not null default true,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint uk_chain_node unique (warehouse_id, chain_name, node_order)
);

create index if not exists idx_chain_warehouse_chain
  on wcs_profile_chain_node (warehouse_id, chain_name, node_order);

create index if not exists idx_chain_warehouse_bean
  on wcs_profile_chain_node (warehouse_id, bean_name);
create table if not exists wcs_profile_chain_node (
  id bigserial primary key,
  warehouse_id bigint not null,
  chain_name varchar(64) not null,
  node_order int not null,
  bean_name varchar(128) not null,
  enabled boolean not null default true,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint uk_chain_node unique (warehouse_id, chain_name, node_order)
);

create index if not exists idx_chain_warehouse_chain
  on wcs_profile_chain_node (warehouse_id, chain_name, node_order);

create index if not exists idx_chain_warehouse_bean
  on wcs_profile_chain_node (warehouse_id, bean_name);
