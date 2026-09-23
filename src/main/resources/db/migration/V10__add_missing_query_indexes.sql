-- V10: Query indexes observed from repository access patterns.
-- FCM token is intentionally excluded.

CREATE INDEX IF NOT EXISTS idx_auth_session_active_user_person_revoked
    ON security.auth_session(active_user_person_id, revoked);

CREATE INDEX IF NOT EXISTS idx_course_schedule_status
    ON catalog.course(schedule_id, status);

CREATE INDEX IF NOT EXISTS idx_course_next_schedule
    ON catalog.course(next_schedule_id);

CREATE INDEX IF NOT EXISTS idx_course_price_course
    ON catalog.course_price(course_id);

CREATE INDEX IF NOT EXISTS idx_coach_timesheet_class_session
    ON training.coach_timesheet(class_session_id);

CREATE INDEX IF NOT EXISTS idx_class_session_course_session_date
    ON training.class_session(course_id, session_date);

CREATE INDEX IF NOT EXISTS idx_class_session_date_time
    ON training.class_session(session_date, start_time);

CREATE INDEX IF NOT EXISTS idx_person_upper_person_code
    ON core.person(upper(person_code));

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_person_full_name_trgm
    ON core.person USING gin (lower(full_name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_person_person_code_trgm
    ON core.person USING gin (lower(person_code) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_users_phone_number_trgm
    ON security.users USING gin (phone_number gin_trgm_ops);
