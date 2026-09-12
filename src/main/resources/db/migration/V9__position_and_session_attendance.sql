CREATE TABLE core.position (
    position_id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO core.position (position_id, code, name, description, active, created_at, updated_at) VALUES
    ('00000000-0000-0000-0000-000000090001', 'COACH_JUNIOR', 'Coach Junior', 'Junior coach business position', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000090002', 'COACH_MIDDLE', 'Coach Middle', 'Middle coach business position', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000090003', 'COACH_SENIOR', 'Coach Senior', 'Senior coach business position', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000090004', 'ASSISTANT_1', 'Assistant 1', 'Assistant level 1 business position', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000090005', 'ASSISTANT_2', 'Assistant 2', 'Assistant level 2 business position', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000090006', 'ASSISTANT_3', 'Assistant 3', 'Assistant level 3 business position', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('00000000-0000-0000-0000-000000090007', 'MANAGER_1', 'Manager 1', 'Manager level 1 business position', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

ALTER TABLE core.person
    ADD COLUMN position_id UUID REFERENCES core.position(position_id);

CREATE INDEX idx_person_position ON core.person(position_id);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM training.course_staff_assignment
        GROUP BY course_id, staff_person_id, assignment_type
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate course_staff_assignment rows exist for course_id, staff_person_id, assignment_type';
    END IF;
END $$;

ALTER TABLE training.course_staff_assignment
    ADD CONSTRAINT uk_course_staff_assignment_course_staff_type
    UNIQUE (course_id, staff_person_id, assignment_type);

ALTER TABLE training.student_attendance RENAME TO session_attendance;
ALTER TABLE training.session_attendance RENAME COLUMN student_attendance_id TO session_attendance_id;

ALTER TABLE training.session_attendance
    ALTER COLUMN student_enrollment_id DROP NOT NULL;

ALTER TABLE training.session_attendance
    ADD CONSTRAINT ck_session_attendance_exactly_one_participant CHECK (
        (student_enrollment_id IS NOT NULL) <> (course_staff_assignment_id IS NOT NULL)
    );

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'student_attendance_pkey'
          AND conrelid = 'training.session_attendance'::regclass
    ) THEN
        ALTER TABLE training.session_attendance
            RENAME CONSTRAINT student_attendance_pkey TO session_attendance_pkey;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_attendance_session_enrollment'
          AND conrelid = 'training.session_attendance'::regclass
    ) THEN
        ALTER TABLE training.session_attendance
            RENAME CONSTRAINT uk_attendance_session_enrollment TO uk_session_attendance_session_enrollment;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_attendance_course_staff_assignment'
          AND conrelid = 'training.session_attendance'::regclass
    ) THEN
        ALTER TABLE training.session_attendance
            RENAME CONSTRAINT fk_student_attendance_course_staff_assignment TO fk_session_attendance_course_staff_assignment;
    END IF;
END $$;

ALTER TABLE training.session_attendance
    ADD CONSTRAINT uk_session_attendance_session_staff_assignment
    UNIQUE (class_session_id, course_staff_assignment_id);

ALTER INDEX IF EXISTS training.idx_attendance_enrollment RENAME TO idx_session_attendance_enrollment;
ALTER INDEX IF EXISTS training.idx_student_attendance_session RENAME TO idx_session_attendance_session;

CREATE INDEX idx_session_attendance_course_staff_assignment
    ON training.session_attendance(course_staff_assignment_id);

UPDATE security.permission
SET code = replace(code, 'STUDENT_ATTENDANCE', 'SESSION_ATTENDANCE'),
    model = 'SESSION_ATTENDANCE',
    description = replace(description, 'Student Attendance', 'Session Attendance')
WHERE code LIKE 'STUDENT_ATTENDANCE_%';

COMMENT ON TABLE core.position IS
    'Business position or rank of a person, separate from security roles.';
COMMENT ON COLUMN core.person.position_id IS
    'Optional business position or rank of the person.';
COMMENT ON TABLE training.session_attendance IS
    'Attendance for exactly one participant in a class session: either a student enrollment or a course staff assignment.';
COMMENT ON COLUMN training.session_attendance.student_enrollment_id IS
    'Student enrollment participant; null when attendance belongs to staff.';
COMMENT ON COLUMN training.session_attendance.course_staff_assignment_id IS
    'Course staff assignment participant; only assistant coach attendance is created by application rules.';
COMMENT ON CONSTRAINT ck_session_attendance_exactly_one_participant ON training.session_attendance IS
    'Exactly one participant FK must be present.';
