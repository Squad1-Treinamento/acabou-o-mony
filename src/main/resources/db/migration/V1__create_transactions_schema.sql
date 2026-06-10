-- V1: Payment Core Tables (transactions, audit_logs, outbox_events)
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payload_hash VARCHAR(255) NOT NULL,
    masked_card VARCHAR(255),
    card_token_id VARCHAR(255),
    acquirer_reference VARCHAR(255),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(merchant_id, idempotency_key),
    CHECK (status IN ('CREATED', 'VALIDATED', 'CHALLENGE_PENDING', 'AUTHENTICATED', 'PROCESSING', 'UNKNOWN', 'COMPLETED', 'DECLINED', 'FAILED'))
);

-- Indexes for transactions table
CREATE INDEX idx_merchant_id ON transactions(merchant_id);
CREATE INDEX idx_status ON transactions(status);
CREATE INDEX idx_created_at ON transactions(created_at);
CREATE INDEX idx_merchant_id_created_at ON transactions(merchant_id, created_at);

-- Function for audit_logs immutability (must be created before trigger)
CREATE OR REPLACE FUNCTION raise_immutable_error()
RETURNS TRIGGER AS $$BEGIN
    RAISE EXCEPTION 'audit_logs table is immutable; UPDATE operations are not allowed';
END;$$ LANGUAGE plpgsql;

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    actor VARCHAR(50) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CHECK (new_status IN ('CREATED', 'VALIDATED', 'CHALLENGE_PENDING', 'AUTHENTICATED', 'PROCESSING', 'UNKNOWN', 'COMPLETED', 'DECLINED', 'FAILED')),
    CHECK (old_status IS NULL OR old_status IN ('CREATED', 'VALIDATED', 'CHALLENGE_PENDING', 'AUTHENTICATED', 'PROCESSING', 'UNKNOWN', 'COMPLETED', 'DECLINED', 'FAILED'))
);

-- Indexes for audit_logs table
CREATE INDEX idx_transaction_id ON audit_logs(transaction_id);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at); -- Renamed to avoid conflict with transactions index name

-- Trigger to prevent UPDATE on audit_logs (INSERT-ONLY table)
CREATE TRIGGER audit_logs_immutable BEFORE UPDATE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION raise_immutable_error();

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    delivered_at TIMESTAMP NULL,
    CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED')),
    CHECK (retry_count >= 0 AND retry_count <= 5)
);

-- Indexes for outbox_events table
CREATE INDEX idx_outbox_status ON outbox_events(status); -- Renamed to avoid conflict
CREATE INDEX idx_outbox_created_at ON outbox_events(created_at); -- Renamed to avoid conflict

