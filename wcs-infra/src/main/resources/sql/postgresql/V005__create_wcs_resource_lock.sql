create table if not exists wcs_resource_lock (
  id bigserial primary key,
  warehouse_id bigint not null,
  lock_key varchar(128) not null,
  owner_type varchar(32) not null,
  owner_id varchar(64) not null,
  status varchar(32) not null,
  expire_at timestamp with time zone null,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint uk_lock_key unique (warehouse_id, lock_key)
);

create index if not exists idx_lock_warehouse_owner
  on wcs_resource_lock (warehouse_id, owner_type, owner_id);

create index if not exists idx_lock_warehouse_expire
  on wcs_resource_lock (warehouse_id, expire_at);
