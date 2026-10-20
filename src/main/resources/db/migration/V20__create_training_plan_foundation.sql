-- Training plans are sellable entitlements.  Courses remain the operational
-- classes that own schedules, sessions and capacity.
CREATE TABLE catalog.training_plan (
    training_plan_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    level VARCHAR(20) NOT NULL,
    duration_months INTEGER NOT NULL,
    included_session_count INTEGER NOT NULL,
    base_price NUMERIC(19,0) NOT NULL,
    final_price NUMERIC(19,0) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_training_plan_duration CHECK (duration_months > 0),
    CONSTRAINT ck_training_plan_sessions CHECK (included_session_count > 0),
    CONSTRAINT ck_training_plan_base_price CHECK (base_price >= 0),
    CONSTRAINT ck_training_plan_final_price CHECK (final_price >= 0),
    CONSTRAINT ck_training_plan_level CHECK (level IN ('BASIC', 'ADVANCED', 'EXPERT')),
    CONSTRAINT ck_training_plan_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE catalog.training_plan_requirement (
    training_plan_requirement_id UUID PRIMARY KEY,
    training_plan_id UUID NOT NULL REFERENCES catalog.training_plan(training_plan_id),
    session_type VARCHAR(20) NOT NULL,
    fulfillment_mode VARCHAR(20) NOT NULL,
    sessions_per_week INTEGER NOT NULL,
    home_course_only BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_training_plan_requirement UNIQUE (training_plan_id, session_type, fulfillment_mode),
    CONSTRAINT ck_training_plan_requirement_session_type CHECK (session_type IN ('REQUIRED', 'STANDARD', 'OPEN')),
    CONSTRAINT ck_training_plan_requirement_fulfillment CHECK (fulfillment_mode IN ('RECURRING', 'FLEX_BOOKING')),
    CONSTRAINT ck_training_plan_requirement_count CHECK (sessions_per_week > 0)
);

CREATE TABLE catalog.training_plan_access_rule (
    training_plan_access_rule_id UUID PRIMARY KEY,
    training_plan_id UUID NOT NULL REFERENCES catalog.training_plan(training_plan_id),
    target_course_id UUID NOT NULL REFERENCES catalog.course(course_id),
    session_type VARCHAR(20) NOT NULL,
    access_mode VARCHAR(20) NOT NULL,
    CONSTRAINT uk_training_plan_access_rule UNIQUE (training_plan_id, target_course_id, session_type, access_mode),
    CONSTRAINT ck_training_plan_access_session_type CHECK (session_type IN ('REQUIRED', 'STANDARD', 'OPEN')),
    CONSTRAINT ck_training_plan_access_mode CHECK (access_mode IN ('HOME', 'RECURRING', 'FLEX'))
);
CREATE INDEX idx_training_plan_access_target ON catalog.training_plan_access_rule(target_course_id, access_mode);

CREATE TABLE catalog.training_plan_addon (
    training_plan_addon_id UUID PRIMARY KEY,
    training_plan_id UUID NOT NULL REFERENCES catalog.training_plan(training_plan_id),
    name VARCHAR(255) NOT NULL,
    extra_sessions_per_week INTEGER NOT NULL,
    price NUMERIC(19,0) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT ck_training_plan_addon_sessions CHECK (extra_sessions_per_week > 0),
    CONSTRAINT ck_training_plan_addon_price CHECK (price >= 0),
    CONSTRAINT ck_training_plan_addon_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

ALTER TABLE catalog.course_schedule
    ADD COLUMN session_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    ADD CONSTRAINT ck_course_schedule_session_type CHECK (session_type IN ('REQUIRED', 'STANDARD', 'OPEN'));

ALTER TABLE training.class_session
    ADD COLUMN schedule_session_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    ADD CONSTRAINT ck_class_session_schedule_session_type
        CHECK (schedule_session_type IN ('REQUIRED', 'STANDARD', 'OPEN'));

-- These links stay nullable through the compatibility period.  V21 backfills
-- legacy course-price purchases; V24 will make them mandatory and remove the
-- old course_price link after application cutover.
ALTER TABLE finance.course_purchase
    ADD COLUMN training_plan_id UUID REFERENCES catalog.training_plan(training_plan_id),
    ADD COLUMN home_course_id UUID REFERENCES catalog.course(course_id);
CREATE INDEX idx_course_purchase_training_plan ON finance.course_purchase(training_plan_id);
CREATE INDEX idx_course_purchase_home_course ON finance.course_purchase(home_course_id);

ALTER TABLE training.student_enrollment_schedule
    ADD COLUMN training_plan_requirement_id UUID REFERENCES catalog.training_plan_requirement(training_plan_requirement_id),
    ADD COLUMN effective_from DATE,
    ADD COLUMN effective_to DATE,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD CONSTRAINT ck_student_enrollment_schedule_period CHECK (effective_to IS NULL OR effective_to >= effective_from),
    ADD CONSTRAINT ck_student_enrollment_schedule_status CHECK (status IN ('ACTIVE', 'ENDED'));

CREATE TABLE finance.enrollment_addon_purchase (
    enrollment_addon_purchase_id UUID PRIMARY KEY,
    student_enrollment_id UUID NOT NULL REFERENCES training.student_enrollment(student_enrollment_id),
    training_plan_addon_id UUID NOT NULL REFERENCES catalog.training_plan_addon(training_plan_addon_id),
    wallet_transaction_id UUID NOT NULL REFERENCES finance.wallet_transaction(wallet_transaction_id),
    effective_week_start DATE NOT NULL,
    effective_to DATE NOT NULL,
    granted_session_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_enrollment_addon_purchase_transaction UNIQUE (wallet_transaction_id),
    CONSTRAINT ck_enrollment_addon_purchase_period CHECK (effective_to >= effective_week_start),
    CONSTRAINT ck_enrollment_addon_purchase_granted CHECK (granted_session_count > 0)
);
CREATE INDEX idx_enrollment_addon_purchase_effective
    ON finance.enrollment_addon_purchase(student_enrollment_id, effective_week_start, effective_to);

CREATE TABLE training.student_session_booking (
    student_session_booking_id UUID PRIMARY KEY,
    student_enrollment_id UUID NOT NULL REFERENCES training.student_enrollment(student_enrollment_id),
    class_session_id UUID NOT NULL REFERENCES training.class_session(class_session_id),
    training_plan_requirement_id UUID REFERENCES catalog.training_plan_requirement(training_plan_requirement_id),
    enrollment_addon_purchase_id UUID REFERENCES finance.enrollment_addon_purchase(enrollment_addon_purchase_id),
    leave_request_id UUID REFERENCES training.leave_request(leave_request_id),
    source VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP,
    settled_at TIMESTAMP,
    CONSTRAINT uk_student_session_booking UNIQUE (student_enrollment_id, class_session_id),
    CONSTRAINT ck_student_session_booking_source CHECK (source IN ('RECURRING', 'FLEX', 'MAKEUP', 'ADMIN')),
    CONSTRAINT ck_student_session_booking_status CHECK (status IN ('RESERVED', 'CANCELLED', 'SETTLED'))
);
CREATE INDEX idx_student_session_booking_session_status
    ON training.student_session_booking(class_session_id, status);
CREATE INDEX idx_student_session_booking_enrollment_status
    ON training.student_session_booking(student_enrollment_id, status);

CREATE TABLE training.enrollment_usage_entry (
    enrollment_usage_entry_id UUID PRIMARY KEY,
    student_enrollment_id UUID NOT NULL REFERENCES training.student_enrollment(student_enrollment_id),
    student_session_booking_id UUID REFERENCES training.student_session_booking(student_session_booking_id),
    leave_request_id UUID REFERENCES training.leave_request(leave_request_id),
    entry_type VARCHAR(20) NOT NULL,
    units INTEGER NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_enrollment_usage_entry_type CHECK (entry_type IN ('NORMAL_DEBIT', 'MAKEUP_GRANT', 'MAKEUP_REDEEM', 'REVERSAL')),
    CONSTRAINT ck_enrollment_usage_entry_units CHECK (units <> 0)
);
CREATE UNIQUE INDEX uk_enrollment_usage_normal_booking
    ON training.enrollment_usage_entry(student_session_booking_id)
    WHERE entry_type = 'NORMAL_DEBIT';
CREATE UNIQUE INDEX uk_enrollment_usage_makeup_leave
    ON training.enrollment_usage_entry(leave_request_id, entry_type)
    WHERE entry_type IN ('MAKEUP_GRANT', 'MAKEUP_REDEEM');

CREATE TABLE training.enrollment_schedule_change_request (
    enrollment_schedule_change_request_id UUID PRIMARY KEY,
    student_enrollment_id UUID NOT NULL REFERENCES training.student_enrollment(student_enrollment_id),
    effective_from DATE NOT NULL,
    reason VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    requested_by_user_id UUID NOT NULL REFERENCES security.users(user_id),
    reviewed_by_user_id UUID REFERENCES security.users(user_id),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_enrollment_schedule_change_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);
CREATE TABLE training.enrollment_schedule_change_item (
    enrollment_schedule_change_item_id UUID PRIMARY KEY,
    enrollment_schedule_change_request_id UUID NOT NULL REFERENCES training.enrollment_schedule_change_request(enrollment_schedule_change_request_id) ON DELETE CASCADE,
    training_plan_requirement_id UUID NOT NULL REFERENCES catalog.training_plan_requirement(training_plan_requirement_id),
    course_schedule_id UUID NOT NULL REFERENCES catalog.course_schedule(course_schedule_id),
    CONSTRAINT uk_enrollment_schedule_change_item UNIQUE (enrollment_schedule_change_request_id, training_plan_requirement_id, course_schedule_id)
);
