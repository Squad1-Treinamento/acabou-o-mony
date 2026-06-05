---
id: task-018
status: not-started
links:
  - spec/tech-plans/plan-001-core-payment-processing.md 
  - spec/specs/spec-001-core-payment-processing.md 
---

# 018 - Risk Evaluation

## Description
Implement merchant and transaction risk evaluation: e.g., velocity, amount, reputation checks.

## Acceptance Criteria
- All high-risk transactions are flagged for step-up (3DS) or rejected.
- Rules are configurable/testable.

## Implementation Steps
- Implement risk scorer/configuration pattern.