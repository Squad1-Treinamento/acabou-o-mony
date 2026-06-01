---
id: task-016
status: in-progress
---

# 016 - Outbox Pattern (Persistence)

## Description
Implement transactional outbox persistence for payment outcomes and webhook workflow.

## Acceptance Criteria
- Outbox events are written atomically with payment state changes.
- No lost or duplicate webhook events.

## Implementation Steps
- Integrate outbox transaction and test atomicity.