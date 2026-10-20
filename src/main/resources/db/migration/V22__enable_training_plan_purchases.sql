ALTER TABLE finance.course_purchase
    ALTER COLUMN course_price_id DROP NOT NULL;

ALTER TABLE finance.wallet_transaction DROP CONSTRAINT ck_wallet_tx_type;
ALTER TABLE finance.wallet_transaction ADD CONSTRAINT ck_wallet_tx_type CHECK (type IN (
    'TOP_UP', 'COURSE_PURCHASE', 'TRAINING_PLAN_PURCHASE', 'TRAINING_ADDON_PURCHASE',
    'SUPPLY_PURCHASE', 'REFUND', 'MANUAL_ADJUSTMENT'
));
