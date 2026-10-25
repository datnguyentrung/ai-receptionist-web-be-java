CREATE TABLE finance.wallet_top_up_request (
    top_up_request_id UUID PRIMARY KEY,
    client_request_id UUID NOT NULL,
    wallet_id UUID NOT NULL REFERENCES finance.wallet(wallet_id),
    requested_by_user_id UUID NOT NULL REFERENCES security.users(user_id),
    reviewed_by_user_id UUID REFERENCES security.users(user_id),
    wallet_transaction_id UUID UNIQUE REFERENCES finance.wallet_transaction(wallet_transaction_id),
    mode VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount NUMERIC(19,0) NOT NULL,
    note VARCHAR(500),
    rejection_reason VARCHAR(500),
    proof_object_path VARCHAR(500),
    proof_sha256 CHAR(64),
    submitted_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_wallet_top_up_client_request UNIQUE (client_request_id),
    CONSTRAINT uk_wallet_top_up_proof_sha256 UNIQUE (proof_sha256),
    CONSTRAINT ck_wallet_top_up_mode CHECK (mode IN ('GUARDIAN_REQUEST', 'STAFF_DIRECT')),
    CONSTRAINT ck_wallet_top_up_status CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_wallet_top_up_amount CHECK (amount > 0 AND amount <= 100000000),
    CONSTRAINT ck_wallet_top_up_proof_pair CHECK (
        (proof_object_path IS NULL AND proof_sha256 IS NULL)
        OR (proof_object_path IS NOT NULL AND proof_sha256 IS NOT NULL)
    )
);

CREATE INDEX idx_wallet_top_up_status_created
    ON finance.wallet_top_up_request(status, created_at DESC);

CREATE INDEX idx_wallet_top_up_requester_created
    ON finance.wallet_top_up_request(requested_by_user_id, created_at DESC);

