ALTER TABLE training.session_attendance
    DROP CONSTRAINT IF EXISTS ck_session_attendance_exactly_one_participant;

COMMENT ON TABLE training.session_attendance IS
    'Attendance for a person in a class session; regular attendance uses a student enrollment or staff assignment, while makeup attendance may be recorded by person only.';
