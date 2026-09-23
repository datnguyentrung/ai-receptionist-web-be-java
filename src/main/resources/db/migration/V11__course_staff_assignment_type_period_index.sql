-- V11: Index for course staff lookups by course + assignment type + effective period.
-- Used by the course list primary-coach batch query and the class session calendar
-- primary-coach range query. Split out of V10 because V10 was already applied.

CREATE INDEX IF NOT EXISTS idx_course_staff_assignment_course_type_period
    ON training.course_staff_assignment(course_id, assignment_type, start_date, end_date, assignment_status);
