# Frontend Setup Guide

## Overview

This document provides a quick start guide for setting up the frontend application for the "Acabou o Mony" payment gateway.

## What Was Created

### Documentation
- **`spec/specs/frontend-web-app.md`** - Complete frontend specification
- **`spec/tasks/frontend-mvp/`** - 8 detailed implementation tasks
- **`spec/tasks/frontend-mvp/README.md`** - Quick reference guide

### Architecture
- **Single Page Application** (SPA) with tab navigation
- **Mock Authentication** (no real backend auth exists)
- **5 Functional Screens**: Login, Payment, Transactions, Details, Idempotency Test
- **Simple State Management**: React Context (no Redux)

## Quick Start

```bash
# 1. Create frontend project
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install

# 2. Install dependencies
npm install axios
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p

# 3. Follow implementation tasks
# See: spec/tasks/frontend-mvp/01-project-setup.md
```

## Implementation Tasks (12-16 hours)

1. **Project Setup** (2h) - Vite + React + TypeScript + Tailwind
2. **Auth Context** (1h) - Mock authentication state
3. **API Client** (1.5h) - Axios with auth headers
4. **Login Screen** (1.5h) - Mock login form
5. **Payment Form** (2.5h) - Payment processing
6. **Transaction List** (2h) - List with filtering
7. **Transaction Details** (1.5h) - Single transaction view
8. **Idempotency Test** (2h) - Duplicate request testing

## API Endpoints

The frontend consumes these existing backend endpoints:

```
POST   /api/v1/payments              # Create payment
GET    /api/v1/payments              # List transactions
GET    /api/v1/payments/{id}         # Get transaction details
```

## Test Credentials

```
Merchant ID: m_123
API Key: sk_test_abc123
```

## Key Features

### ✅ Payment Processing
- Create payments with card tokens
- Auto-generate idempotency keys
- Handle 3DS challenges
- Display payment status

### ✅ Transaction Management
- List all transactions
- Filter by status
- View transaction details
- Refresh transaction status

### ✅ Idempotency Testing
- Submit duplicate requests
- Verify cached responses
- Inspect response headers
- Validate duplicate detection

## Technology Stack

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool
- **Tailwind CSS** - Styling
- **Axios** - HTTP client
- **React Context** - State management

## File Structure

```
frontend/
├── src/
│   ├── components/          # UI components
│   │   ├── LoginForm.tsx
│   │   ├── PaymentForm.tsx
│   │   ├── TransactionList.tsx
│   │   ├── TransactionDetails.tsx
│   │   └── IdempotencyTest.tsx
│   ├── context/            # Global state
│   │   └── AuthContext.tsx
│   ├── services/           # API client
│   │   └── api.ts
│   ├── types/              # TypeScript types
│   │   ├── auth.ts
│   │   └── payment.ts
│   ├── utils/              # Utilities
│   │   └── uuid.ts
│   ├── App.tsx             # Main component
│   └── main.tsx            # Entry point
├── package.json
├── vite.config.ts
├── tsconfig.json
└── tailwind.config.js
```

## Prerequisites

- Node.js 18+
- Backend running on `http://localhost:8080`
- Backend specs: spec-001 through spec-005

## Validation

```bash
# Type checking
npm run type-check

# Linting
npm run lint

# Development server
npm run dev

# Production build
npm run build
```

## Next Steps

1. **Start with Task 01**: `spec/tasks/frontend-mvp/01-project-setup.md`
2. **Follow tasks sequentially**: Each task builds on the previous
3. **Test after each task**: Use validation commands
4. **Complete all 8 tasks**: Full MVP functionality

## Support

- **Main Spec**: `spec/specs/frontend-web-app.md`
- **Task Overview**: `spec/tasks/frontend-mvp/00-overview.md`
- **Quick Guide**: `spec/tasks/frontend-mvp/README.md`
- **Backend Specs**: `spec/specs/spec-001-*.md` through `spec-005-*.md`

## Success Criteria

- [ ] Mock login works
- [ ] Payment form submits to backend
- [ ] Transaction list displays data
- [ ] Transaction details shows full info
- [ ] Idempotency test verifies duplicate detection
- [ ] 3DS flow opens ACS URL
- [ ] All errors handled gracefully
- [ ] No console errors

---

**Ready to start?** Begin with `spec/tasks/frontend-mvp/01-project-setup.md`
