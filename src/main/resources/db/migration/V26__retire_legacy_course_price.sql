-- CoursePrice is fully replaced by TrainingPlan.  Refuse to discard the
-- compatibility links if historical purchases have not been migrated.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM finance.course_purchase
        WHERE training_plan_id IS NULL OR home_course_id IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot retire course_price: every course purchase must have training_plan_id and home_course_id';
    END IF;
END $$;

UPDATE finance.wallet_transaction
SET type = 'TRAINING_PLAN_PURCHASE'
WHERE type = 'COURSE_PURCHASE';

ALTER TABLE finance.wallet_transaction DROP CONSTRAINT IF EXISTS ck_wallet_tx_type;
ALTER TABLE finance.wallet_transaction ADD CONSTRAINT ck_wallet_tx_type CHECK (type IN (
    'TOP_UP', 'TRAINING_PLAN_PURCHASE', 'TRAINING_ADDON_PURCHASE',
    'SUPPLY_PURCHASE', 'REFUND', 'MANUAL_ADJUSTMENT'
));

ALTER TABLE finance.course_purchase
    ALTER COLUMN training_plan_id SET NOT NULL,
    ALTER COLUMN home_course_id SET NOT NULL;

DROP INDEX IF EXISTS finance.idx_course_purchase_price;
ALTER TABLE finance.course_purchase DROP COLUMN course_price_id;
ALTER TABLE catalog.training_plan DROP COLUMN legacy_course_price_id;
DROP TABLE catalog.course_price;
