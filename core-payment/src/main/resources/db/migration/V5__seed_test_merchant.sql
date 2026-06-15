-- V5: Seed test merchant for local development and integration tests
-- Plain API key: teste_key
-- Hash: Argon2id, m=65536, t=3, p=1, salt=16B, hash=32B (Spring Security defaults)
INSERT INTO merchants (id, merchant_id, api_key_hash, webhook_url, created_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    '$argon2id$v=19$m=65536,t=3,p=1$h37ld8FE8C+GnfVf3nFyaA$KIjPhLpvE7WlHqADxsD73UgFoE4IiVOiJBjRAmPUE10',
    'http://localhost:9999/webhook',
    NOW()
) ON CONFLICT (merchant_id) DO NOTHING;
