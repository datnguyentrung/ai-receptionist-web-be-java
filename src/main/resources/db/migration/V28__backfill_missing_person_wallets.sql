-- PersonService provisions a zero-balance active wallet in the same transaction
-- as every new Person. Repair profiles created before that invariant existed so
-- finance workflows can safely lock a wallet by person_id.
INSERT INTO finance.wallet (
    wallet_id,
    person_id,
    balance,
    status,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    person.person_id,
    0,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM core.person AS person
LEFT JOIN finance.wallet AS wallet
    ON wallet.person_id = person.person_id
WHERE wallet.wallet_id IS NULL;
