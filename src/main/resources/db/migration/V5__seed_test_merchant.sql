-- V5: Seed a test merchant for development and testing purposes
--
-- Test Credentials (for use in API client like Postman):
-- Merchant ID: 00000000-0000-0000-0000-000000000001
-- API Key:     test_api_key_123
-- Webhook URL: http://localhost:8081/webhook-receiver (example)
-- Webhook Secret: test_webhook_secret_456
--
-- The api_key_hash below is an example hash. A real one would be generated
-- by the application using a secure hashing algorithm like Argon2.
-- This one corresponds to 'test_api_key_123' with a known salt.
INSERT INTO merchants (id, merchant_id, api_key_hash, webhook_url, created_at, webhook_secret)
VALUES (
    'a1b2c3d4-e5f6-7890-1234-567890abcdef', -- Primary Key (UUID)
    '00000000-0000-0000-0000-000000000001', -- Public Merchant ID (UUID)
    '$argon2id$v=19$m=65536,t=3,p=1$oW/wOBElj4alzieOmM9+3Q$rRSuhf3eEA+dWk/rfXVGsRjHw8riRuJH+jAq8xD+afc', -- Hashed API Key for 'teste_key'
    'https://webhook.site/851873e9-cba1-42cc-be81-1392527b1300', -- Example Webhook URL
    NOW(), -- Creation Timestamp
    'test_webhook_secret_456' -- Webhook Secret
);