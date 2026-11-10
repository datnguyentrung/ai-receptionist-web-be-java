create schema if not exists iot;

create table iot.managed_device (
    device_id uuid primary key,
    device_code varchar(128) not null unique,
    display_name varchar(150) not null,
    device_type varchar(32) not null,
    lifecycle_status varchar(32) not null default 'REGISTERED',
    branch_id bigint references core.branch(branch_id),
    fog_gateway_id uuid references iot.managed_device(device_id),
    paired_kiosk_id uuid references iot.managed_device(device_id),
    assigned_user_id uuid,
    group_code varchar(100), area_label varchar(150), location_label varchar(150),
    credential_hash varchar(255), credential_version integer not null default 0,
    credential_revoked_at timestamptz,
    provisioning_token_hash varchar(255), provisioning_expires_at timestamptz,
    desired_config jsonb not null default '{}'::jsonb, desired_config_version integer not null default 1,
    applied_config_version integer not null default 0,
    hardware_version varchar(100), firmware_version varchar(100),
    last_seen_at timestamptz, last_health varchar(32) not null default 'UNKNOWN',
    last_error_code varchar(128), last_reported_state jsonb not null default '{}'::jsonb,
    registry_version bigint not null default 1,
    activated_at timestamptz, decommissioned_at timestamptz,
    created_at timestamptz not null, updated_at timestamptz not null,
    constraint ck_managed_device_type check (device_type in ('FOG_GATEWAY','KIOSK','ESP32')),
    constraint ck_managed_device_lifecycle check (lifecycle_status in ('REGISTERED','PROVISIONED','ACTIVE','MAINTENANCE','SUSPENDED','DECOMMISSIONED'))
);
create index idx_managed_device_gateway on iot.managed_device(fog_gateway_id, lifecycle_status);
create index idx_managed_device_seen on iot.managed_device(lifecycle_status, last_seen_at);

create table iot.device_event (
    device_event_id uuid primary key,
    device_id uuid not null references iot.managed_device(device_id) on delete cascade,
    event_kind varchar(32) not null,
    event_code varchar(100) not null,
    severity varchar(16), alert_status varchar(32), message varchar(1000) not null,
    details jsonb not null default '{}'::jsonb,
    created_by_user_id uuid, occurred_at timestamptz not null,
    acknowledged_by_user_id uuid, acknowledged_at timestamptz,
    resolved_by_user_id uuid, resolved_at timestamptz, resolution_note varchar(1000)
);
create index idx_device_event_list on iot.device_event(device_id, occurred_at desc);
create unique index uk_device_open_alert on iot.device_event(device_id, event_code)
    where event_kind = 'ALERT' and alert_status in ('OPEN','ACKNOWLEDGED');
