-- Preserve a deterministic bridge from every historical price to exactly one
-- plan.  Equal price is deliberately not used as a grouping key: cross-course
-- access must be configured explicitly after the migration.
ALTER TABLE catalog.training_plan
    ADD COLUMN legacy_course_price_id UUID UNIQUE REFERENCES catalog.course_price(course_price_id);

INSERT INTO catalog.training_plan (
    training_plan_id, name, level, duration_months, included_session_count,
    base_price, final_price, status, created_at, updated_at, legacy_course_price_id
)
SELECT gen_random_uuid(), course.name || ' (legacy)', course.level,
       price.duration_months, price.session_count, price.base_price, price.final_price,
       price.status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, price.course_price_id
FROM catalog.course_price price
JOIN catalog.course course ON course.course_id = price.course_id;

-- Legacy plans retain their historical operating course as HOME and recurring
-- access.  Administrators can subsequently combine equivalent offerings by
-- creating a new shared plan and explicit access rules.
INSERT INTO catalog.training_plan_requirement (
    training_plan_requirement_id, training_plan_id, session_type, fulfillment_mode,
    sessions_per_week, home_course_only
)
SELECT gen_random_uuid(), plan.training_plan_id, 'STANDARD', 'RECURRING', 1, TRUE
FROM catalog.training_plan plan
WHERE plan.legacy_course_price_id IS NOT NULL;

INSERT INTO catalog.training_plan_access_rule (
    training_plan_access_rule_id, training_plan_id, target_course_id, session_type, access_mode
)
SELECT gen_random_uuid(), plan.training_plan_id, price.course_id, 'STANDARD', mode.access_mode
FROM catalog.training_plan plan
JOIN catalog.course_price price ON price.course_price_id = plan.legacy_course_price_id
CROSS JOIN (VALUES ('HOME'), ('RECURRING'), ('FLEX')) AS mode(access_mode);

UPDATE finance.course_purchase purchase
SET training_plan_id = plan.training_plan_id,
    home_course_id = price.course_id
FROM catalog.course_price price
JOIN catalog.training_plan plan ON plan.legacy_course_price_id = price.course_price_id
WHERE purchase.course_price_id = price.course_price_id;

UPDATE training.student_enrollment_schedule link
SET effective_from = enrollment.start_date,
    effective_to = enrollment.end_date,
    status = CASE WHEN enrollment.status IN ('COMPLETED', 'EXPIRED', 'CANCELLED') THEN 'ENDED' ELSE 'ACTIVE' END
FROM training.student_enrollment enrollment
WHERE enrollment.student_enrollment_id = link.student_enrollment_id;

-- Fail fast rather than silently serving a partially migrated entitlement.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM finance.course_purchase WHERE training_plan_id IS NULL OR home_course_id IS NULL) THEN
        RAISE EXCEPTION 'Training plan backfill left course purchases without a plan or home course';
    END IF;
END $$;
