-- V2: Merchant Table
CREATE TABLE merchants (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL UNIQUE,
    api_key_hash VARCHAR(255) NOT NULL,
    webhook_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);
