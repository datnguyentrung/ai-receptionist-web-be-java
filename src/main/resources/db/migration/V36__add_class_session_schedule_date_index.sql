create index if not exists idx_class_session_schedule_date
    on training.class_session (course_schedule_id, session_date);
