---
id: us-004
status: active
links:
  - spec/user-stories/index.md
  - spec/specs/index.md
  - spec/tasks/index.md
---
# Live and Conversational Commerce Integration

## User Story
As a merchant using the "Acabou o Mony" platform, I want seamless integration with Live and Conversational Commerce platforms so that customers can complete transactions without leaving the host platform.

## Context
- Merchants rely on uninterrupted live and chat experiences to convert sales.
- The system must remain compatible with existing payment acquirers.

## Scope
- In scope: API support for live commerce and conversational commerce transaction flows.
- Out of scope: building partner apps, partner business contracts, end-user UI redesign.

## Acceptance Criteria (BDD)
- Given a customer is watching a live commerce stream, when they purchase, then they complete payment without platform redirection.
- Given a customer is using a conversational commerce interface, when they purchase, then the system supports in-chat payment completion.

## Definition of Done
- API integration is implemented, documented, and tested.
- Performance testing under integrated loads is completed and approved.
- Public-facing API documentation is available for integration partners.
