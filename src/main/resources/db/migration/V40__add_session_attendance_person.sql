ALTER TABLE training.session_attendance
    ADD COLUMN person_id uuid;

UPDATE training.session_attendance attendance
SET person_id = enrollment.student_person_id
FROM training.student_enrollment enrollment
WHERE attendance.student_enrollment_id = enrollment.student_enrollment_id;

UPDATE training.session_attendance attendance
SET person_id = assignment.staff_person_id
FROM training.course_staff_assignment assignment
WHERE attendance.course_staff_assignment_id = assignment.course_staff_assignment_id;

ALTER TABLE training.session_attendance
    ALTER COLUMN person_id SET NOT NULL,
    ADD CONSTRAINT fk_session_attendance_person
        FOREIGN KEY (person_id) REFERENCES core.person(person_id),
    ADD CONSTRAINT uk_session_attendance_session_person
        UNIQUE (class_session_id, person_id);

CREATE INDEX idx_session_attendance_person
    ON training.session_attendance(person_id);
