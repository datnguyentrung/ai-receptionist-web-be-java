CREATE INDEX IF NOT EXISTS idx_class_session_course_status
    ON training.class_session(course_id, status);
