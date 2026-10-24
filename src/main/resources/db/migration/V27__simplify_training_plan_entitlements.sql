-- A plan grants quotas by session type.  It no longer has a home course,
-- nor an on-demand/FLEX entitlement; selected schedules are the entitlement.

UPDATE catalog.training_plan_requirement
SET session_type = 'STANDARD'
WHERE session_type = 'OPEN';

UPDATE catalog.training_plan_access_rule
SET session_type = 'STANDARD'
WHERE session_type = 'OPEN';

UPDATE catalog.course_schedule
SET session_type = 'STANDARD'
WHERE session_type = 'OPEN';

UPDATE training.class_session
SET schedule_session_type = 'STANDARD'
WHERE schedule_session_type = 'OPEN';

ALTER TABLE catalog.training_plan_access_rule
    DROP CONSTRAINT IF EXISTS ck_training_plan_access_session_type;
ALTER TABLE catalog.training_plan_access_rule
    DROP CONSTRAINT IF EXISTS ck_training_plan_access_mode;
ALTER TABLE catalog.training_plan_access_rule
    DROP CONSTRAINT IF EXISTS uk_training_plan_access_rule;
ALTER TABLE catalog.training_plan_access_rule
    DROP COLUMN IF EXISTS access_mode;

-- The legacy model created one identical course/session-type rule for every
-- HOME, RECURRING and FLEX mode.  Those modes are now one entitlement, so
-- retain one representative row before enforcing the new natural key.
DELETE FROM catalog.training_plan_access_rule AS duplicate_rule
USING catalog.training_plan_access_rule AS retained_rule
WHERE duplicate_rule.training_plan_id = retained_rule.training_plan_id
  AND duplicate_rule.target_course_id = retained_rule.target_course_id
  AND duplicate_rule.session_type = retained_rule.session_type
  AND duplicate_rule.ctid > retained_rule.ctid;

ALTER TABLE catalog.training_plan_access_rule
    ADD CONSTRAINT uk_training_plan_access_rule
        UNIQUE (training_plan_id, target_course_id, session_type);
ALTER TABLE catalog.training_plan_access_rule
    ADD CONSTRAINT ck_training_plan_access_session_type
        CHECK (session_type IN ('REQUIRED', 'STANDARD'));

DROP INDEX IF EXISTS catalog.idx_training_plan_access_target;
CREATE INDEX idx_training_plan_access_target
    ON catalog.training_plan_access_rule(target_course_id, session_type);

ALTER TABLE catalog.training_plan_requirement
    DROP CONSTRAINT IF EXISTS ck_training_plan_requirement_session_type;
ALTER TABLE catalog.training_plan_requirement
    ADD CONSTRAINT ck_training_plan_requirement_session_type
        CHECK (session_type IN ('REQUIRED', 'STANDARD'));

ALTER TABLE catalog.course_schedule
    DROP CONSTRAINT IF EXISTS ck_course_schedule_session_type;
ALTER TABLE catalog.course_schedule
    ADD CONSTRAINT ck_course_schedule_session_type
        CHECK (session_type IN ('REQUIRED', 'STANDARD'));

ALTER TABLE training.class_session
    DROP CONSTRAINT IF EXISTS ck_class_session_schedule_session_type;
ALTER TABLE training.class_session
    ADD CONSTRAINT ck_class_session_schedule_session_type
        CHECK (schedule_session_type IN ('REQUIRED', 'STANDARD'));

ALTER TABLE finance.course_purchase
    DROP CONSTRAINT IF EXISTS fk_course_purchase_home_course;
DROP INDEX IF EXISTS finance.idx_course_purchase_home_course;
ALTER TABLE finance.course_purchase
    DROP COLUMN IF EXISTS home_course_id;

ALTER TABLE training.student_session_booking
    DROP CONSTRAINT IF EXISTS ck_student_session_booking_source;
ALTER TABLE training.student_session_booking
    ADD CONSTRAINT ck_student_session_booking_source
        CHECK (source IN ('RECURRING', 'MAKEUP', 'ADMIN'));

ALTER TABLE training.student_enrollment_schedule
    DROP CONSTRAINT IF EXISTS uk_student_enrollment_schedule;
ALTER TABLE training.student_enrollment_schedule
    ADD CONSTRAINT uk_student_enrollment_schedule
        UNIQUE (student_enrollment_id, course_schedule_id, effective_from);

-- V20 introduced an item-based schedule-change request model.  The fixed
-- schedule workflow replaces it with a request for one target schedule.
-- Existing requests are intentionally not retained (no compatibility
-- backfill is required for the current data set).
DROP TABLE IF EXISTS training.enrollment_schedule_change_item;
DROP TABLE IF EXISTS training.enrollment_schedule_change_request;

CREATE TABLE training.enrollment_schedule_change_request (
    enrollment_schedule_change_request_id UUID PRIMARY KEY,
    student_enrollment_schedule_id UUID NOT NULL REFERENCES training.student_enrollment_schedule(student_enrollment_schedule_id),
    target_course_schedule_id UUID NOT NULL REFERENCES catalog.course_schedule(course_schedule_id),
    effective_from DATE NOT NULL,
    note VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    requested_by_user_id UUID NOT NULL REFERENCES security.users(user_id),
    reviewed_by_user_id UUID REFERENCES security.users(user_id),
    reviewed_at TIMESTAMP,
    review_note VARCHAR(1000),
    CONSTRAINT ck_enrollment_schedule_change_request_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);
CREATE INDEX idx_enrollment_schedule_change_request_pending
    ON training.enrollment_schedule_change_request(status, effective_from);
