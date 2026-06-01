# 020 - Structured Audit Logging

## Description
Implement structured audit logging for all sensitive or security-relevant actions, with masking and tamper-detection checksum.

## Acceptance Criteria
- Audit entries are complete, immutable, and PI fields masked.
- Every log event includes a checksum field.

## Implementation Steps
- Implement audit logging utility and integrate into sensitive flows.