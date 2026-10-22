-- Every training-plan requirement is now fulfilled by a schedule selected
-- during purchase. The old fulfillment_mode split is no longer part of the contract.
ALTER TABLE catalog.training_plan_requirement
    DROP CONSTRAINT IF EXISTS ck_training_plan_requirement_fulfillment;

ALTER TABLE catalog.training_plan_requirement
    DROP CONSTRAINT IF EXISTS uk_training_plan_requirement;

ALTER TABLE catalog.training_plan_requirement
    DROP COLUMN IF EXISTS fulfillment_mode;

ALTER TABLE catalog.training_plan_requirement
    DROP COLUMN IF EXISTS home_course_only;

ALTER TABLE catalog.training_plan_requirement
    ADD CONSTRAINT uk_training_plan_requirement
        UNIQUE (training_plan_id, session_type);
