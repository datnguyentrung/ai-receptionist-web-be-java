alter table iot.managed_device add column if not exists provisioning_public_id varchar(32);
alter table iot.managed_device add column if not exists hardware_id varchar(128);
create unique index if not exists uk_managed_device_provisioning_public_id
  on iot.managed_device(provisioning_public_id) where provisioning_public_id is not null;
create unique index if not exists uk_managed_device_hardware_id
  on iot.managed_device(hardware_id) where hardware_id is not null;
alter table iot.managed_device add constraint fk_managed_device_assigned_user
  foreign key (assigned_user_id) references security.users(user_id);
