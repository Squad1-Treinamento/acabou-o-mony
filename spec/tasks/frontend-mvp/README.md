# Frontend MVP - Complete Guide

## Overview

Simple single-page React application for the "Acabou o Mony" payment gateway. Built to consume existing backend APIs without modifications.

## What's Included

### 5 Functional Screens (Tabs)
1. **Login** - Mock authentication (no real backend auth)
2. **Payment Processing** - Create and submit payments
3. **Transaction List** - View all transactions with filtering
4. **Transaction Details** - View single transaction details
5. **Idempotency Test** - Test duplicate request handling

### Tech Stack
- React 18 + TypeScript
- Vite (build tool)
- Tailwind CSS (styling)
- Axios (HTTP client)
- React Context (state management)

## Quick Start

```bash
# 1. Navigate to project root
cd acabou-o-mony

# 2. Create frontend project
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install

# 3. Install dependencies
npm install axios
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p

# 4. Follow tasks 01-08 in order
# See spec/tasks/frontend-mvp/01-project-setup.md

# 5. Start development
npm run dev
# Opens http://localhost:5173
```

## Task Checklist

- [ ] **Task 01** - Project Setup (2h)
  - Vite + React + TypeScript
  - Tailwind CSS
  - Axios
  - Project structure

- [ ] **Task 02** - Auth Context (1h)
  - React Context for credentials
  - Mock login/logout

- [ ] **Task 03** - API Client (1.5h)
  - Axios instance
  - TypeScript types
  - Error handling

- [ ] **Task 04** - Login Screen (1.5h)
  - Login form
  - Validation
  - Mock authentication

- [ ] **Task 05** - Payment Form (2.5h)
  - Payment creation
  - Idempotency key generation
  - 3DS support

- [ ] **Task 06** - Transaction List (2h)
  - Fetch transactions
  - Table display
  - Status filtering

- [ ] **Task 07** - Transaction Details (1.5h)
  - Single transaction view
  - Refresh status
  - Back navigation

- [ ] **Task 08** - Idempotency Test (2h)
  - Duplicate request testing
  - Side-by-side comparison
  - Header inspection

**Total: 12-16 hours**

## API Endpoints Used

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

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── LoginForm.tsx
│   │   ├── PaymentForm.tsx
│   │   ├── TransactionList.tsx
│   │   ├── TransactionDetails.tsx
│   │   └── IdempotencyTest.tsx
│   ├── context/
│   │   └── AuthContext.tsx
│   ├── services/
│   │   └── api.ts
│   ├── types/
│   │   ├── auth.ts
│   │   └── payment.ts
│   ├── utils/
│   │   └── uuid.ts
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.js
└── .env
```

## Validation Commands

```bash
# Type checking
npm run type-check

# Linting
npm run lint

# Development server
npm run dev

# Production build
npm run build

# Preview production build
npm run preview
```

## Prerequisites

Before starting, ensure:
- [ ] Backend running on `http://localhost:8080`
- [ ] Node.js 18+ installed
- [ ] Backend specs read (spec-001 through spec-005)

## Common Issues

### Backend Connection Refused
```bash
# Make sure backend is running
cd backend
uvicorn app.main:app --reload
```

### CORS Errors
- Vite proxy should handle this automatically
- Check `vite.config.ts` proxy configuration

### Type Errors
```bash
# Run type checking
npm run type-check
```

## Testing Checklist

### Login Flow
- [ ] Login form displays
- [ ] Validation works (empty fields)
- [ ] Login stores credentials
- [ ] Dashboard shows after login
- [ ] Logout clears credentials

### Payment Flow
- [ ] Form displays with all fields
- [ ] Idempotency key auto-generates
- [ ] Submit creates payment
- [ ] Success response displays
- [ ] Error handling works
- [ ] 3DS challenge link opens

### Transaction List
- [ ] Transactions load on mount
- [ ] Table displays data
- [ ] Status filter works
- [ ] Click row navigates to details
- [ ] Empty state displays correctly

### Transaction Details
- [ ] Details load by ID
- [ ] All fields display
- [ ] Refresh button works
- [ ] Back button returns to list
- [ ] 404 handling works

### Idempotency Test
- [ ] First request succeeds
- [ ] Second request returns cached response
- [ ] X-Idempotent-Replayed header shows "true"
- [ ] Transaction IDs match
- [ ] Reset button works

## Next Steps After MVP

1. **Real Authentication**
   - Add login endpoint to backend
   - Store JWT token
   - Add token refresh logic

2. **Additional Features**
   - Refunds UI
   - Webhooks configuration
   - Transaction search
   - Export functionality

3. **Improvements**
   - Dark mode
   - Accessibility (WCAG)
   - Internationalization (i18n)
   - Unit tests
   - E2E tests

## Support

- **Spec**: `spec/specs/frontend-web-app.md`
- **Backend Specs**: `spec/specs/spec-001-*.md` through `spec-005-*.md`
- **Tasks**: `spec/tasks/frontend-mvp/01-*.md` through `08-*.md`

## Success Criteria

- [ ] Mock login works
- [ ] Payment form submits to backend
- [ ] Transaction list fetches data
- [ ] Transaction details displays
- [ ] Idempotency test shows duplicate detection
- [ ] 3DS flow opens ACS URL
- [ ] All errors handled gracefully
- [ ] No console errors
- [ ] Responsive on desktop

---

**Status**: Ready to implement  
**Estimated Time**: 12-16 hours  
**Last Updated**: 2025-01-20
