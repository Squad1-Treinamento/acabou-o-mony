# 014 - Reconciliation Worker

## Description
Implement a scheduled background worker that checks unsettled payments for confirmed status resolutions with the acquirer/provider.

## Acceptance Criteria
- All pending/UNKNOWN payments are regularly checked and resolved.
- All status transitions are logged and auditable.

## Implementation Steps
- Implement scheduled job and status resolution.