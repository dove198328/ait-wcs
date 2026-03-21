create table if not exists wcs_warehouse_profile (
  id bigserial primary key,
  warehouse_id bigint not null,
  warehouse_type varchar(32) not null,
  enabled_plugins_json jsonb not null,
  params_json jsonb not null default '{}'::jsonb,
  version int not null default 1,
  active boolean not null default true,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  constraint uk_profile_warehouse unique (warehouse_id)
);
