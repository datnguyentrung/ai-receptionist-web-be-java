-- Version 16: COURSE_SCHEDULE is the dated operating timetable of a course. CLASS_SESSION
-- remains the dated lesson generated from it.
CREATE TABLE catalog.course_schedule (
    course_schedule_id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES catalog.course(course_id),
    class_schedule_id UUID NOT NULL REFERENCES catalog.class_schedule(schedule_id),
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_course_schedule_period CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_course_schedule_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX idx_course_schedule_course_period
    ON catalog.course_schedule(course_id, start_date, end_date, status);
CREATE INDEX idx_course_schedule_class_schedule
    ON catalog.course_schedule(class_schedule_id);

-- Preserve the current and pending operating plans. Current plans start when
-- their course was created, as agreed for this breaking migration.
INSERT INTO catalog.course_schedule (
    course_schedule_id, course_id, class_schedule_id, start_date, end_date, status, created_at, updated_at
)
SELECT gen_random_uuid(), c.course_id, c.schedule_id, c.created_at::date,
       CASE WHEN c.next_schedule_effective_from IS NULL THEN NULL ELSE c.next_schedule_effective_from - INTERVAL '1 day' END,
       'ACTIVE', c.created_at, c.updated_at
FROM catalog.course c;

INSERT INTO catalog.course_schedule (
    course_schedule_id, course_id, class_schedule_id, start_date, end_date, status, created_at, updated_at
)
SELECT gen_random_uuid(), c.course_id, c.next_schedule_id, c.next_schedule_effective_from,
       NULL, 'ACTIVE', c.created_at, c.updated_at
FROM catalog.course c
WHERE c.next_schedule_id IS NOT NULL;

-- Retain historical enrollment schedules that predate the current plan.
INSERT INTO catalog.course_schedule (
    course_schedule_id, course_id, class_schedule_id, start_date, end_date, status, created_at, updated_at
)
SELECT gen_random_uuid(), cp.course_id, e.class_schedule_id, e.start_date, e.end_date,
       'INACTIVE', e.created_at, e.updated_at
FROM training.student_enrollment e
JOIN finance.course_purchase cp ON cp.course_purchase_id = e.course_purchase_id
WHERE NOT EXISTS (
    SELECT 1 FROM catalog.course_schedule cs
    WHERE cs.course_id = cp.course_id AND cs.class_schedule_id = e.class_schedule_id
);

CREATE TABLE training.student_enrollment_schedule (
    student_enrollment_schedule_id UUID PRIMARY KEY,
    student_enrollment_id UUID NOT NULL REFERENCES training.student_enrollment(student_enrollment_id),
    course_schedule_id UUID NOT NULL REFERENCES catalog.course_schedule(course_schedule_id),
    CONSTRAINT uk_student_enrollment_schedule UNIQUE (student_enrollment_id, course_schedule_id)
);
CREATE INDEX idx_student_enrollment_schedule_course_schedule
    ON training.student_enrollment_schedule(course_schedule_id);

INSERT INTO training.student_enrollment_schedule (
    student_enrollment_schedule_id, student_enrollment_id, course_schedule_id
)
SELECT gen_random_uuid(), e.student_enrollment_id, cs.course_schedule_id
FROM training.student_enrollment e
JOIN finance.course_purchase cp ON cp.course_purchase_id = e.course_purchase_id
JOIN LATERAL (
    SELECT candidate.course_schedule_id
    FROM catalog.course_schedule candidate
    WHERE candidate.course_id = cp.course_id
      AND candidate.class_schedule_id = e.class_schedule_id
    ORDER BY (candidate.start_date <= e.start_date) DESC, candidate.start_date DESC
    LIMIT 1
) cs ON TRUE;

ALTER TABLE training.class_session ADD COLUMN course_schedule_id UUID;
UPDATE training.class_session session
SET course_schedule_id = selected.course_schedule_id
FROM LATERAL (
    SELECT cs.course_schedule_id
    FROM catalog.course_schedule cs
    WHERE cs.course_id = session.course_id
      AND cs.class_schedule_id = session.schedule_snapshot_id
    ORDER BY (cs.start_date <= session.session_date) DESC, cs.start_date DESC
    LIMIT 1
) selected;
-- A snapshot always exists after V13; this fallback protects anomalous legacy rows.
UPDATE training.class_session session
SET course_schedule_id = selected.course_schedule_id
FROM LATERAL (
    SELECT cs.course_schedule_id FROM catalog.course_schedule cs
    WHERE cs.course_id = session.course_id
    ORDER BY cs.start_date DESC LIMIT 1
) selected
WHERE session.course_schedule_id IS NULL;
ALTER TABLE training.class_session
    ALTER COLUMN course_schedule_id SET NOT NULL,
    ADD CONSTRAINT fk_class_session_course_schedule
        FOREIGN KEY (course_schedule_id) REFERENCES catalog.course_schedule(course_schedule_id);
CREATE INDEX idx_class_session_course_schedule_date
    ON training.class_session(course_schedule_id, session_date);
DROP INDEX IF EXISTS uk_class_session_course_date_active;
CREATE UNIQUE INDEX uk_class_session_course_schedule_date_active
    ON training.class_session(course_schedule_id, session_date) WHERE status <> 'CANCELLED';

-- Convert staff assignments to their course's active schedule for the assigned
-- period. Existing references retain the assignment id.
ALTER TABLE training.course_staff_assignment ADD COLUMN course_schedule_id UUID;
UPDATE training.course_staff_assignment assignment
SET course_schedule_id = selected.course_schedule_id
FROM LATERAL (
    SELECT cs.course_schedule_id FROM catalog.course_schedule cs
    WHERE cs.course_id = assignment.course_id
      AND cs.start_date <= assignment.start_date
      AND (cs.end_date IS NULL OR cs.end_date >= assignment.start_date)
    ORDER BY cs.start_date DESC LIMIT 1
) selected;
ALTER TABLE training.course_staff_assignment
    ALTER COLUMN course_schedule_id SET NOT NULL,
    ADD CONSTRAINT fk_course_staff_assignment_course_schedule
        FOREIGN KEY (course_schedule_id) REFERENCES catalog.course_schedule(course_schedule_id);
ALTER TABLE training.course_staff_assignment DROP CONSTRAINT IF EXISTS uk_course_staff_assignment_course_staff_type;
ALTER TABLE training.course_staff_assignment DROP COLUMN course_id;
ALTER TABLE training.course_staff_assignment
    ADD CONSTRAINT uk_course_staff_assignment_schedule_staff_type
        UNIQUE (course_schedule_id, staff_person_id, assignment_type);
CREATE INDEX idx_course_staff_assignment_schedule_period
    ON training.course_staff_assignment(course_schedule_id, assignment_type, start_date, end_date, assignment_status);

ALTER TABLE training.student_enrollment DROP COLUMN class_schedule_id;
ALTER TABLE catalog.course DROP CONSTRAINT IF EXISTS ck_course_next_schedule_pair;
ALTER TABLE catalog.course DROP COLUMN next_schedule_id;
ALTER TABLE catalog.course DROP COLUMN next_schedule_effective_from;
ALTER TABLE catalog.course DROP COLUMN schedule_id;
DROP INDEX IF EXISTS catalog.idx_course_schedule_status;
DROP INDEX IF EXISTS catalog.idx_course_next_schedule;
DROP INDEX IF EXISTS catalog.idx_course_next_schedule_effective;
