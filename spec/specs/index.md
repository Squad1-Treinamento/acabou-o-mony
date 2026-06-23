# Specs Index

Use this index to locate the smallest spec that describes behavior or
rules needed for implementation.

- spec-001-core-payment-processing.md — behavioral spec, business rules, and validation criteria for the Core Payment Processing Service
- spec-002-3ds-mfa-auth-engine.md — 3DS / MFA Auth Engine: challenge flow, Redis session state, JWT, callback to core
- spec-003-entry-point-with-ngrok-and-nginx.md — local entry point with ngrok and nginx
- spec-004-cache-and-idempotency-layer.md — cache e idempotency layer functional requirements
- spec-005-3ds-core-payment-integration.md — bidirectional integration between Core Payment (Spring MVC/JPA) and 3DS Engine: session creation, idempotent callback, async Spring Event finalization
- spec-006-frontend-checkout.md — customer-facing multi-step checkout flow: payment form, processing, 3DS challenge redirect, success and error terminal screens
- spec-007-merchant-dashboard.md — merchant-facing transaction dashboard: API key auth, transaction list with filters and auto-refresh, detail view, status badges, webhook delivery status
