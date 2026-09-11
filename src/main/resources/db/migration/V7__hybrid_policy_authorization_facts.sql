-- V7: Business facts needed by hybrid RBAC + policy-based authorization.

-- Preserve custom role-permission mappings by renaming the existing permission rows in-place.
-- PermissionSynchronizer removes codes that no longer exist in PermissionDefinition, so these
-- updates must happen before the application starts with the new COURSE_STAFF_ASSIGNMENT codes.
UPDATE security.permission
SET code = 'COURSE_STAFF_ASSIGNMENT_READ', model = 'COURSE_STAFF_ASSIGNMENT',
    description = 'Course Staff Assignment Read'
WHERE code = 'COACH_ASSIGNMENT_READ';
UPDATE security.permission
SET code = 'COURSE_STAFF_ASSIGNMENT_CREATE', model = 'COURSE_STAFF_ASSIGNMENT',
    description = 'Course Staff Assignment Create'
WHERE code = 'COACH_ASSIGNMENT_CREATE';
UPDATE security.permission
SET code = 'COURSE_STAFF_ASSIGNMENT_UPDATE', model = 'COURSE_STAFF_ASSIGNMENT',
    description = 'Course Staff Assignment Update'
WHERE code = 'COACH_ASSIGNMENT_UPDATE';
UPDATE security.permission
SET code = 'COURSE_STAFF_ASSIGNMENT_DELETE', model = 'COURSE_STAFF_ASSIGNMENT',
    description = 'Course Staff Assignment Delete'
WHERE code = 'COACH_ASSIGNMENT_DELETE';
ALTER TABLE training.coach_assignment RENAME TO course_staff_assignment;
ALTER TABLE training.course_staff_assignment RENAME COLUMN coach_assignment_id TO course_staff_assignment_id;
ALTER TABLE training.course_staff_assignment RENAME COLUMN coach_person_id TO staff_person_id;
ALTER TABLE training.course_staff_assignment RENAME COLUMN assigned_date TO start_date;
ALTER TABLE training.course_staff_assignment RENAME COLUMN coach_assignment_status TO assignment_status;

ALTER TABLE training.student_attendance RENAME COLUMN coach_assignment_id TO course_staff_assignment_id;
ALTER TABLE training.coach_timesheet RENAME COLUMN coach_assignment_id TO course_staff_assignment_id;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'coach_assignment_pkey'
          AND conrelid = 'training.course_staff_assignment'::regclass
    ) THEN
        ALTER TABLE training.course_staff_assignment
            RENAME CONSTRAINT coach_assignment_pkey TO course_staff_assignment_pkey;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_attendance_coach_assignment'
          AND conrelid = 'training.student_attendance'::regclass
    ) THEN
        ALTER TABLE training.student_attendance
            RENAME CONSTRAINT fk_student_attendance_coach_assignment
            TO fk_student_attendance_course_staff_assignment;
    END IF;
END $$;

ALTER TABLE training.course_staff_assignment
    ADD COLUMN assignment_type VARCHAR(30);

-- Legacy coach_assignment rows do not distinguish primary, assistant or manager duties,
-- so every existing row is backfilled as PRIMARY_COACH.
UPDATE training.course_staff_assignment
SET assignment_type = 'PRIMARY_COACH'
WHERE assignment_type IS NULL;

ALTER TABLE training.course_staff_assignment
    ALTER COLUMN assignment_type SET NOT NULL;

ALTER TABLE training.course_staff_assignment
    ADD CONSTRAINT ck_course_staff_assignment_type CHECK (
        assignment_type IN ('PRIMARY_COACH', 'ASSISTANT_COACH', 'TEACHING_ASSISTANT', 'MANAGER')
    );

ALTER TABLE training.course_staff_assignment
    ADD CONSTRAINT ck_course_staff_assignment_period CHECK (
        end_date IS NULL OR end_date >= start_date
    );

ALTER TABLE training.student_enrollment
    ADD CONSTRAINT ck_student_enrollment_period CHECK (end_date >= start_date);

ALTER TABLE training.class_session
    ADD COLUMN attendance_reopened_until TIMESTAMP;

CREATE INDEX idx_course_staff_assignment_person_course_period
    ON training.course_staff_assignment(staff_person_id, course_id, start_date, end_date);

CREATE INDEX idx_student_enrollment_student_period
    ON training.student_enrollment(student_person_id, start_date, end_date);

CREATE INDEX idx_user_person_user_relationship_person_active
    ON core.user_person(user_id, relationship_type, person_id, active);

CREATE INDEX idx_student_attendance_session
    ON training.student_attendance(class_session_id);

DROP INDEX IF EXISTS training.idx_coach_assignment_course;
CREATE INDEX idx_course_staff_assignment_course
    ON training.course_staff_assignment(course_id);

COMMENT ON TABLE training.course_staff_assignment IS
    'Staff assignment to a course, used as a business fact for policy authorization.';
COMMENT ON COLUMN training.course_staff_assignment.staff_person_id IS
    'Staff person assigned to the course.';
COMMENT ON COLUMN training.course_staff_assignment.assignment_type IS
    'Operational assignment type; roles and permissions still control allowed actions.';
COMMENT ON COLUMN training.class_session.attendance_reopened_until IS
    'Temporary attendance reopen deadline. Null means no active reopen window.';
