# Frontend Web Application Specification

**Status**: Draft  
**Owner**: Frontend Team  
**Last Updated**: 2025-01-20

## Overview

Single-page web application for the "Acabou o Mony" payment gateway. Provides merchant interface for payment processing, transaction monitoring, and 3DS authentication testing.

## Context

Backend is **complete** with:
- Core Payment Processing API (spec-001)
- 3DS/MFA Auth Engine (spec-002)
- Entry Point (ngrok + Nginx) (spec-003)
- Cache & Idempotency Layer (spec-004)
- 3DS-Core Integration (spec-005)

Frontend must consume these existing APIs without modifications.

## Technology Stack

- **Framework**: React 18 + TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS
- **HTTP Client**: Axios
- **State**: React Context (no Redux needed for MVP)
- **Routing**: React Router v6 (single-page app)

## Architecture Decisions

### Single Page Application (SPA)
- All screens in one page with conditional rendering
- No complex routing (keep it simple)
- Mock authentication (no real backend auth exists)

### API Base URL
```typescript
// Development
const API_BASE_URL = 'http://localhost:8080/api/v1';

// Production (via ngrok)
const API_BASE_URL = process.env.VITE_API_URL || 'http://localhost:8080/api/v1';
```

## Screens (Single Page with Tabs)

### 1. Mock Login Screen
**Purpose**: Simulate merchant authentication (no real backend auth)

**Fields**:
- Merchant ID (text input, e.g., "m_123")
- API Key (password input, e.g., "sk_test_abc123")
- Login button

**Behavior**:
- Store merchant_id and api_key in React Context
- No actual API call (backend has no auth endpoint)
- Show validation error if fields are empty
- On success, show main dashboard

**Acceptance Criteria**:
- [ ] Input fields for merchant_id and api_key
- [ ] "Login" button disabled if fields empty
- [ ] Store credentials in context on submit
- [ ] Navigate to dashboard after login

---

### 2. Payment Processing Screen
**Purpose**: Create and test payment transactions

**API Endpoint**: `POST /api/v1/payments`

**Form Fields**:
- Amount (number, in cents, e.g., 10000 = R$100.00)
- Currency (dropdown: BRL, USD)
- Card Token (text input, e.g., "tok_visa_approved")
- Customer ID (text input, optional)
- Idempotency Key (auto-generated UUID, read-only)

**Actions**:
- "Submit Payment" button
- "Generate New Idempotency Key" button

**Response Display**:
- Transaction ID
- Status (CREATED, VALIDATED, PROCESSING, COMPLETED, DECLINED, FAILED)
- Challenge ID (if 3DS required)
- ACS URL (if 3DS required)
- Error message (if failed)

**3DS Flow**:
- If status = `CHALLENGE_PENDING`, show:
  - "Open 3DS Challenge" button (opens ACS URL in new tab)
  - "Check Status" button (polls transaction status)

**Acceptance Criteria**:
- [ ] Form with all required fields
- [ ] Auto-generate idempotency key on mount
- [ ] Send POST request with Authorization header
- [ ] Display response (success or error)
- [ ] If CHALLENGE_PENDING, show ACS URL link
- [ ] Handle 400, 401, 409, 503 errors gracefully

---

### 3. Transaction List Screen
**Purpose**: View all transactions for the merchant

**API Endpoint**: `GET /api/v1/payments?merchant_id={merchant_id}`

**Display**:
- Table with columns:
  - Transaction ID
  - Amount
  - Currency
  - Status
  - Created At
  - Actions (View Details)

**Filters**:
- Status dropdown (ALL, COMPLETED, DECLINED, FAILED, PROCESSING)
- Date range (optional for MVP)

**Acceptance Criteria**:
- [ ] Fetch transactions on mount
- [ ] Display in table format
- [ ] Filter by status
- [ ] Click row to view details
- [ ] Handle empty state (no transactions)

---

### 4. Transaction Details Screen
**Purpose**: View single transaction details

**API Endpoint**: `GET /api/v1/payments/{transaction_id}`

**Display**:
- Transaction ID
- Merchant ID
- Amount
- Currency
- Status
- Masked Card (e.g., "411111XXXXXX1111")
- Acquirer Reference (Mercado Pago ID)
- Created At
- Updated At
- Challenge ID (if 3DS was used)

**Actions**:
- "Back to List" button
- "Refresh Status" button (re-fetch transaction)

**Acceptance Criteria**:
- [ ] Fetch transaction by ID
- [ ] Display all fields
- [ ] Handle 404 (transaction not found)
- [ ] Refresh button updates data

---

### 5. Idempotency Test Screen
**Purpose**: Test idempotency behavior (duplicate requests)

**Form**:
- Same as Payment Processing Screen
- **But**: Allow manual idempotency key input
- "Submit Payment" button
- "Submit Again (Same Key)" button

**Display**:
- First request response
- Second request response
- Header `X-Idempotent-Replayed` value

**Acceptance Criteria**:
- [ ] Allow manual idempotency key
- [ ] Submit same request twice
- [ ] Show both responses side-by-side
- [ ] Verify second response has `X-Idempotent-Replayed: true`

---

## API Integration

### Authentication
All requests must include:
```typescript
headers: {
  'Authorization': `Bearer ${apiKey}`,
  'Content-Type': 'application/json'
}
```

### Endpoints Used

```typescript
// Payment Processing
POST   /api/v1/payments
GET    /api/v1/payments?merchant_id={id}
GET    /api/v1/payments/{transaction_id}

// 3DS (read-only, for display)
// No direct calls to 3DS engine from frontend
// ACS URL is returned in payment response
```

### Request/Response Types

```typescript
// POST /api/v1/payments
interface PaymentRequest {
  merchant_id: string;
  amount: number;           // cents
  currency: string;         // "BRL" | "USD"
  card_token: string;
  customer_id?: string;
  idempotency_key: string;  // UUID
}

interface PaymentResponse {
  transaction_id: string;
  status: PaymentStatus;
  amount: number;
  currency: string;
  challenge_id?: string;    // if 3DS required
  acs_url?: string;         // if 3DS required
  created_at: string;       // ISO8601
}

type PaymentStatus = 
  | "CREATED"
  | "VALIDATED"
  | "CHALLENGE_PENDING"
  | "AUTHENTICATED"
  | "PROCESSING"
  | "COMPLETED"
  | "DECLINED"
  | "FAILED"
  | "UNKNOWN";

// GET /api/v1/payments/{id}
interface TransactionDetails {
  id: string;
  merchant_id: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  masked_card: string;
  acquirer_reference?: string;
  challenge_id?: string;
  created_at: string;
  updated_at: string;
}
```

## Component Structure

```
src/
├── components/
│   ├── LoginForm.tsx           # Mock login
│   ├── PaymentForm.tsx         # Payment processing
│   ├── TransactionList.tsx     # Transaction table
│   ├── TransactionDetails.tsx  # Single transaction view
│   ├── IdempotencyTest.tsx     # Idempotency testing
│   └── Layout.tsx              # Tab navigation
├── context/
│   └── AuthContext.tsx         # Store merchant_id + api_key
├── services/
│   └── api.ts                  # Axios client
├── types/
│   └── payment.ts              # TypeScript interfaces
├── App.tsx                     # Main component with tabs
└── main.tsx
```

## State Management

### AuthContext
```typescript
interface AuthContextType {
  merchantId: string | null;
  apiKey: string | null;
  login: (merchantId: string, apiKey: string) => void;
  logout: () => void;
  isAuthenticated: boolean;
}
```

### Local Component State
- Form inputs (controlled components)
- Loading states
- Error messages
- Transaction list data

## Error Handling

### HTTP Status Codes
- **400 Bad Request**: Show validation error message
- **401 Unauthorized**: Show "Invalid API Key" + logout
- **409 Conflict**: Show "Duplicate request detected"
- **503 Service Unavailable**: Show "Service temporarily unavailable"

### Network Errors
- Timeout: Show "Request timeout, please retry"
- Connection refused: Show "Cannot connect to server"

## UI/UX Guidelines

### Layout
- Single page with tab navigation (no page reloads)
- Tabs: Login | Payment | Transactions | Details | Idempotency Test
- Responsive (mobile-first, but desktop-optimized)

### Colors (Tailwind)
- Primary: `blue-600` (buttons, links)
- Success: `green-600` (COMPLETED status)
- Warning: `yellow-600` (PROCESSING status)
- Error: `red-600` (FAILED, DECLINED status)
- Neutral: `gray-600` (text, borders)

### Forms
- Input fields with labels
- Validation errors below inputs (red text)
- Disabled submit button while loading
- Loading spinner on buttons

### Tables
- Striped rows (`odd:bg-gray-50`)
- Hover effect (`hover:bg-gray-100`)
- Sticky header (if many rows)

## Validation Commands

```bash
# Install dependencies
cd frontend
npm install

# Type checking
npm run type-check

# Linting
npm run lint

# Development server
npm run dev
# Opens http://localhost:5173

# Build
npm run build

# Preview production build
npm run preview
```

## Out of Scope (MVP)

- ❌ Real authentication (backend has no auth endpoints)
- ❌ User registration
- ❌ Password reset
- ❌ Merchant settings UI
- ❌ Refunds UI
- ❌ Webhooks UI
- ❌ Analytics/charts
- ❌ Export transactions (CSV/PDF)
- ❌ Dark mode
- ❌ Internationalization (i18n)
- ❌ Accessibility (WCAG compliance)
- ❌ Unit tests (focus on integration with backend)

## Dependencies

- Backend must be running on `http://localhost:8080`
- ngrok tunnel must be active (for production testing)
- Redis must be running (for idempotency)
- PostgreSQL must be running (for transaction storage)

## Success Criteria

- [ ] Mock login stores credentials in context
- [ ] Payment form submits to backend API
- [ ] Transaction list fetches and displays data
- [ ] Transaction details shows single transaction
- [ ] Idempotency test shows duplicate detection
- [ ] 3DS flow opens ACS URL in new tab
- [ ] All HTTP errors handled gracefully
- [ ] Responsive layout works on desktop
- [ ] No console errors in browser

## Next Steps After MVP

1. Real authentication (when backend adds auth)
2. Refunds UI
3. Webhooks configuration
4. Transaction search/filters
5. Export functionality
6. Dark mode
7. Accessibility improvements
