---
id: task-017
status: not-started
links:
  - spec/tech-plans/plan-001-core-payment-processing.md 
  - spec/specs/spec-001-core-payment-processing.md 
---

# 017 - Webhook Dispatch Worker

## Description
Develop background worker for reliable delivery and retry of outgoing webhooks to merchants or services.

## Acceptance Criteria
- All webhook events are attempted and retried with exponential backoff (or policy).
- Delivery/failure status is logged and tracked.

## Implementation Steps
- Add webhook dispatcher/worker implementation.