# 009 - DB Idempotency Enforcement

## Description
Implement database-level idempotency enforcement with unique constraints and fallback for duplicate recovery.

## Acceptance Criteria
- Unique index/constraint protects transactional idempotency.
- Fallback logic recovers and returns reference result for duplicates.

## Implementation Steps
- Add DB unique constraint and recovery logic.