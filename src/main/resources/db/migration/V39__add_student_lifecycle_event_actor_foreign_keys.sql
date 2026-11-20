ALTER TABLE training.student_lifecycle_event
    ADD CONSTRAINT fk_student_lifecycle_event_created_by
        FOREIGN KEY (created_by) REFERENCES security.users(user_id);

ALTER TABLE training.student_lifecycle_event
    ADD CONSTRAINT fk_student_lifecycle_event_voided_by
        FOREIGN KEY (voided_by) REFERENCES security.users(user_id);
