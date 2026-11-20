CREATE TABLE training.student_lifecycle_event (
    event_id UUID PRIMARY KEY,
    student_person_id UUID NOT NULL REFERENCES core.person(person_id),
    event_type VARCHAR(30) NOT NULL,
    event_date DATE NOT NULL,
    event_status VARCHAR(20) NOT NULL DEFAULT 'VALID',
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    voided_by UUID,
    voided_at TIMESTAMP,
    void_reason VARCHAR(255),
    CONSTRAINT ck_student_lifecycle_event_type CHECK (
        event_type IN ('NEW', 'LEAVE', 'REACTIVATED')
    ),
    CONSTRAINT ck_student_lifecycle_event_status CHECK (
        event_status IN ('VALID', 'VOIDED')
    )
);

CREATE INDEX idx_student_lifecycle_event_student
    ON training.student_lifecycle_event(student_person_id);

CREATE INDEX idx_student_lifecycle_event_type_date
    ON training.student_lifecycle_event(event_type, event_date);
