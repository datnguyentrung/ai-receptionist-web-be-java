CREATE SCHEMA IF NOT EXISTS crm;

CREATE TABLE crm.lead (
    lead_id UUID PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    birth_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    assigned_user_id UUID REFERENCES security.users(user_id),
    next_follow_up_at TIMESTAMP,
    last_contacted_at TIMESTAMP,
    note VARCHAR(1000),
    converted_person_id UUID REFERENCES core.person(person_id),
    converted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_crm_lead_status CHECK (status IN (
        'NEW', 'CONTACTED', 'TRIAL_BOOKED', 'TRIAL_ATTENDED', 'CONVERTED', 'LOST'
    ))
);

-- Exact phone lookups and contact deduplication.
CREATE INDEX idx_crm_lead_phone
    ON crm.lead(phone_number);

-- Global follow-up queue: filter by workflow status, then order by due time.
CREATE INDEX idx_crm_lead_status_follow_up
    ON crm.lead(status, next_follow_up_at);

-- Per-counsellor follow-up queue; also covers the assigned_user_id foreign key.
CREATE INDEX idx_crm_lead_assigned_status_follow_up
    ON crm.lead(assigned_user_id, status, next_follow_up_at);

-- Supports the converted-person foreign-key lookup and reverse checks.
CREATE INDEX idx_crm_lead_converted_person
    ON crm.lead(converted_person_id);

CREATE TABLE crm.trial_registration (
    trial_registration_id UUID PRIMARY KEY,
    lead_id UUID NOT NULL REFERENCES crm.lead(lead_id) ON DELETE RESTRICT,
    class_session_id UUID REFERENCES training.class_session(class_session_id),
    coach_person_id UUID REFERENCES core.person(person_id),
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    checked_in_at TIMESTAMP,
    coach_feedback VARCHAR(1000),
    outcome_note VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_crm_trial_registration_status CHECK (status IN (
        'BOOKED', 'ATTENDED', 'NO_SHOW', 'CANCELLED'
    ))
);

-- Lists a lead's latest trial registrations and covers the lead foreign key.
CREATE INDEX idx_crm_trial_registration_lead_created
    ON crm.trial_registration(lead_id, created_at DESC);

-- Supports trial-registration dashboards filtered by status.
CREATE INDEX idx_crm_trial_registration_status_created
    ON crm.trial_registration(status, created_at DESC);

-- Foreign-key indexes used when loading sessions and coach information.
CREATE INDEX idx_crm_trial_registration_class_session
    ON crm.trial_registration(class_session_id);

CREATE INDEX idx_crm_trial_registration_coach_person
    ON crm.trial_registration(coach_person_id);
