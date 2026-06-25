# Frontend MVP - Task Overview

**Branch**: `frontend-mvp`  
**Status**: Planning  
**Estimated Effort**: 12-16 hours

## Goal

Build a **single-page application** with mock login and 5 functional screens (tabs) to interact with the existing payment backend.

## Prerequisites

- Backend running on `http://localhost:8080`
- Node.js 18+ installed
- Backend specs: spec-001 through spec-005

## Task Breakdown

### Setup (2 hours)
- [ ] `01-project-setup.md` - Vite + React + TypeScript + Tailwind

### Core Features (10-14 hours)
- [ ] `02-auth-context.md` - Mock authentication context
- [ ] `03-api-client.md` - Axios client with auth headers
- [ ] `04-login-screen.md` - Mock login form
- [ ] `05-payment-form.md` - Payment processing screen
- [ ] `06-transaction-list.md` - Transaction list screen
- [ ] `07-transaction-details.md` - Transaction details screen
- [ ] `08-idempotency-test.md` - Idempotency testing screen

## Total: 12-16 hours

## Success Criteria

- [ ] Mock login works (stores credentials)
- [ ] Payment form submits to backend
- [ ] Transaction list fetches data
- [ ] Transaction details shows single transaction
- [ ] Idempotency test shows duplicate detection
- [ ] 3DS flow opens ACS URL
- [ ] All errors handled gracefully

## Validation After Each Task

```bash
npm run type-check
npm run lint
npm run dev  # Manual testing in browser
```

## Architecture

**Single Page Application**:
- One `App.tsx` with tab navigation
- No React Router (keep it simple)
- Conditional rendering based on active tab
- AuthContext for global state

## API Endpoints Used

```
POST   /api/v1/payments
GET    /api/v1/payments?merchant_id={id}
GET    /api/v1/payments/{transaction_id}
```

## Next Steps After MVP

1. Add real authentication (when backend supports it)
2. Add refunds UI
3. Add webhooks configuration
4. Add transaction search
