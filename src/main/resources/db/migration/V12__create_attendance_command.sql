create table if not exists training.attendance_command (
    request_id uuid primary key,
    command_type varchar(64) not null,
    status varchar(32) not null,
    payload text,
    result text,
    error_code varchar(128),
    error_title varchar(255),
    error_detail text,
    created_at timestamp not null,
    updated_at timestamp not null,
    completed_at timestamp
);

create index if not exists idx_attendance_command_status_created
    on training.attendance_command (status, created_at);
