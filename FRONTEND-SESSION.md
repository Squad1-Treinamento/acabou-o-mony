# Frontend MVP — Session Summary

**Date:** 2026-06-29  
**Branch:** `frontend-mvp`  
**Stack:** Vite + React 19 + TypeScript + Tailwind CSS + Axios

---

## Tasks Completed (04–08)

| Task | Files Created | Files Modified |
|---|---|---|
| **04 — Login Screen** | `src/components/LoginForm.tsx` | `src/App.tsx` |
| **05 — Payment Form** | `src/utils/uuid.ts`, `src/components/PaymentForm.tsx` | `src/App.tsx` |
| **06 — Transaction List** | `src/components/TransactionList.tsx` | `src/App.tsx` |
| **07 — Transaction Details** | `src/components/TransactionDetails.tsx` | `src/App.tsx` |
| **08 — Idempotency Test** | — | `src/services/api.ts`, `src/components/PaymentForm.tsx`, `src/App.tsx` |

> **Note on Task 08:** Instead of a separate "Idempotency Test" tab (which wouldn't exist in production), the functionality was integrated into the Payment Form via an **"Advanced Mode" toggle**. When enabled, the idempotency key becomes editable and two submit buttons appear for duplicate testing.

---

## Architecture Overview

```
frontend/
├── src/
│   ├── components/
│   │   ├── LoginForm.tsx          # Mock login (merchant_id + api_key)
│   │   ├── PaymentForm.tsx        # Payment form + advanced mode toggle
│   │   ├── TransactionList.tsx    # Transaction table with status filter
│   │   └── TransactionDetails.tsx # Single transaction view
│   ├── context/
│   │   └── AuthContext.tsx        # Mock auth (login/logout/isAuthenticated)
│   ├── services/
│   │   └── api.ts                 # Axios client with auth interceptor
│   ├── types/
│   │   ├── auth.ts                # AuthContextType interface
│   │   └── payment.ts            # PaymentRequest/Response, TransactionDetails, etc.
│   ├── utils/
│   │   └── uuid.ts                # UUID v4 + idempotency key generator
│   ├── App.tsx                    # Tab navigation (Payment | Transactions)
│   ├── main.tsx                   # Entry point, wraps App with AuthProvider
│   └── index.css                  # Tailwind directives only
├── tailwind.config.js             # Custom Mercado Livre colors
├── vite.config.ts                 # Vite proxy → localhost:8080
├── tsconfig.app.json              # strict, verbatimModuleSyntax
└── package.json
```

---

## Key Design Decisions

### Type-only imports
`tsconfig.app.json` has `verbatimModuleSyntax: true`. All type-only imports must use `import type`. Files like `payment.ts` export **only types** (interfaces), so any import from it must be `import type`. Same applies to `FormEvent` from React.

### Mercado Livre Visual Identity
Custom Tailwind colors in `tailwind.config.js`:

| Token | Hex | Usage |
|---|---|---|
| `ml-blue` | `#3483FA` | Header bg, secondary buttons, links |
| `ml-blue-dark` | `#1259C3` | Hover state for blue elements |
| `ml-yellow` | `#FFE600` | Primary CTA buttons (Submit, Login) |
| `ml-yellow-dark` | `#E5CF00` | Hover for yellow buttons |
| `ml-green` | `#00A650` | COMPLETED status, success messages |
| `ml-red` | `#F23D3D` | DECLINED/FAILED status, errors |
| `ml-orange` | `#FF7733` | PROCESSING/CHALLENGE_PENDING status |
| `ml-bg` | `#EDEDED` | Page background |
| `ml-surface` | `#FFFFFF` | Card backgrounds |
| `ml-border` | `#E5E5E5` | Borders and dividers |

### Cleaned up default Vite CSS
`src/index.css` contained conflicting custom CSS (`#root { width: 1126px }`, `h1 { font-size: 56px }`, etc.) that overrode Tailwind utilities. Replaced with just the three `@tailwind` directives.

### Tab Navigation (no React Router)
Simple state-based tab switching in `App.tsx` (`type Tab = 'payment' | 'transactions' | 'details'`).

---

## Commands

```bash
npm run dev          # Start dev server (localhost:5173 → proxies /api → localhost:8080)
npm run type-check   # tsc --noEmit
npm run lint         # oxlint
npm run build        # tsc -b && vite build
```

---

## Backend Dependencies

The frontend expects:
- Backend on `http://localhost:8080`
- `POST /api/v1/payments` — Create payment
- `GET /api/v1/payments?merchant_id={id}` — List transactions
- `GET /api/v1/payments/{transaction_id}` — Get single transaction
- Auth via `Authorization: Bearer {api_key}` header

---

## Known Lint Warning (pre-existing)

```
src/context/AuthContext.tsx:29:17:
  warning react(only-export-components):
  Fast refresh only works when a file only exports components.
```

This is because `AuthContext.tsx` exports both `AuthProvider` (component) and `useAuth` (hook) from the same file — a common React pattern. Safe to ignore.

---

## Next Steps

1. Real authentication (when backend supports it)
2. Refunds UI
3. Webhooks configuration
4. Transaction search
5. End-to-end testing with backend
