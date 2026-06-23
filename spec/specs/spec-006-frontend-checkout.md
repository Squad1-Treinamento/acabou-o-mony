---
id: spec-006
status: active
links:
  - spec/specs/index.md
  - spec/user-stories/us-001-transaction-processing.md
  - spec/user-stories/us-003-transaction-security.md
  - spec/user-stories/us-004-live-conversational-integration.md
  - spec/tech-plans/index.md
  - spec/specs/spec-001-core-payment-processing.md
  - spec/specs/spec-002-3ds-mfa-auth-engine.md
  - spec/specs/spec-005-3ds-core-payment-integration.md
---

# Frontend Checkout Flow Specification

This document defines the behavioral requirements, screen states, API integration rules, and acceptance criteria for the customer-facing multi-step checkout interface of the Acabou o Mony payment platform.

The checkout frontend is a Next.js 14 application that guides end customers through a complete payment flow: from payment summary and card token submission through acquirer processing, 3DS authentication when required, and terminal state display.

This spec must remain aligned with:

- `spec/specs/spec-001-core-payment-processing.md` — payment lifecycle and API contract
- `spec/specs/spec-002-3ds-mfa-auth-engine.md` — challenge session and JWT behavior
- `spec/specs/spec-005-3ds-core-payment-integration.md` — bidirectional integration contract
- `design.json` — canonical design tokens (colors, typography, spacing, motion)

---

## Context and Primary Objective

- **Context:** The backend payment processing infrastructure (core-payment, 3ds-engine, Nginx, Redis, PostgreSQL) is fully operational. This spec covers the customer-facing UI layer that merchants embed or link to, allowing end customers to authorize and complete payments without leaving the merchant's commerce context.
- **Objective:** Provide a conversion-optimized, accessible, mobile-first checkout experience that minimizes friction, communicates trust continuously, and handles all payment outcomes — including 3DS step-up authentication — without requiring the customer to understand the underlying complexity.

---

## Functional Requirements (Behavior)

### User Story

As a customer completing a purchase, I want a clear, guided checkout form that handles card payment, 3DS verification if required, and shows me a definitive outcome, so I can complete my purchase confidently in a single session.

### Business Rules

#### General

- The checkout is pre-configured with `amount`, `currency`, and `card_token_id` (pre-tokenized card reference provided by the merchant integration layer). The customer does not enter raw card numbers.
- A `masked_card` value (e.g., `411111XXXXXX1111`) MAY be passed in for display purposes only.
- `customer_email` is optional and, when present, must be a syntactically valid email address.
- One `Idempotency-Key` per checkout session is generated as `req_<uuid-v4>` and stored in `sessionStorage`. The same key is reused on any retry within the same session, ensuring safe re-submission.
- The checkout frontend never persists card data, PAN, or CVV at any point.

#### Step 1 — Payment Form

- Display the payment summary: formatted amount (locale-aware, e.g., `R$ 249,90`), currency label, and merchant name if provided.
- Display masked card for reference if `masked_card` is provided; this field is read-only.
- The `card_token_id` field is a text input accepting 1–100 characters. Label: "Token do cartão".
- Optional `customer_email` field with email format validation on blur.
- "Pagar" (Pay) button is disabled until `card_token_id` is non-empty.
- On submit, transition immediately to Step 2 (Processing).

#### Step 2 — Processing

- Display an animated loading indicator while `POST /api/v1/payments` is in flight.
- The request includes:
  - `Authorization: Bearer {api_key}` header
  - `Idempotency-Key: req_{uuid}` header
  - Body: `{ amount, currency, idempotency_key, payment_method: { card_token_id, masked_card? }, customer_email? }`
- On network error or 5xx response: surface a recoverable error state with a "Tentar novamente" button. Reuse the same `Idempotency-Key` on retry.
- On 400: surface a validation error with the server message. No retry — customer must correct input.
- On 401: surface "Autenticação inválida" error. No retry.
- On 409 (duplicate in-progress): display "Pagamento já em processamento" and transition to Step 3a polling using the `transaction_id` from the response.

#### Step 3a — 3DS Challenge (CHALLENGE_PENDING)

- Triggered by HTTP 202 response with `status == "CHALLENGE_PENDING"`.
- The response includes `challenge_id` and `acs_url`.
- Persist `transaction_id` and `challenge_id` to `sessionStorage` before redirecting.
- Redirect the browser top-level frame to `acs_url`.
- After the ACS flow completes, the bank redirects back. On landing, read `transaction_id` from `sessionStorage` and begin polling `GET /api/v1/payments/{transaction_id}` every 2 seconds.
- Polling terminates when `status` is one of: `COMPLETED`, `DECLINED`, `FAILED`.
- While polling: display Step 2 (Processing) loading state with message "Verificando autenticação...".
- Polling timeout: if no terminal state is reached within 120 seconds, surface an error state ("Tempo de verificação esgotado. Tente novamente.").

#### Step 3b — Success (COMPLETED)

- Display: checkmark icon (color `#1F8F53`), heading "Pagamento confirmado", transaction ID, formatted amount, currency, masked card (if available), and timestamp.
- No further actions available; session is complete.

#### Step 3c — Error (DECLINED / FAILED / UNKNOWN timeout)

- `DECLINED`: "Pagamento recusado. Verifique os dados do cartão ou tente com outro método." No retry button.
- `FAILED`: "Erro no processamento. Tente novamente." Retry button resets to Step 1 with a new `Idempotency-Key`.
- `UNKNOWN` timeout: "Pagamento em verificação. Você receberá uma confirmação em breve." No retry.

#### Step Indicator

- A linear step indicator with labeled circles: Step 1 (Dados), Step 2 (Processando), Step 3 (Confirmação).
- Completed steps: filled circle `#0D2B1E`, white checkmark icon.
- Active step: filled circle `#0D2B1E`, step number white.
- Inactive steps: circle border `#E5E7EB`, step number `#6B7280`.
- Connector line between steps: `#DADDE2`; completed connector: `#0D2B1E`.

---

## Acceptance Criteria (BDD)

### Happy Path — Low Risk Payment

- **Given** a checkout session is initialized with `amount=24990`, `currency="BRL"`, and a valid `card_token_id`,
  **when** the customer clicks "Pagar",
  **then** the UI transitions to Step 2 (Processing) immediately and `POST /api/v1/payments` is called with the correct headers and body.

- **Given** `POST /api/v1/payments` returns HTTP 200 with `status == "COMPLETED"`,
  **when** the response is received,
  **then** the UI transitions to the Success screen displaying `transaction_id`, formatted amount `R$ 249,90`, and masked card.

### 3DS Challenge Path

- **Given** `POST /api/v1/payments` returns HTTP 202 with `status == "CHALLENGE_PENDING"` and an `acs_url`,
  **when** the response is received,
  **then** `transaction_id` and `challenge_id` are written to `sessionStorage` and the browser is redirected to `acs_url` within 500ms.

- **Given** the customer completes 3DS on the bank's ACS page and is redirected back,
  **when** the checkout page re-mounts,
  **then** it reads `transaction_id` from `sessionStorage`, displays the Processing state with "Verificando autenticação...", and begins polling `GET /api/v1/payments/{transaction_id}` every 2 seconds.

- **Given** polling returns `status == "COMPLETED"`,
  **then** the UI transitions to the Success screen.

- **Given** polling returns `status == "DECLINED"`,
  **then** the UI displays the DECLINED error state with no retry option.

### Error States

- **Given** the network request fails (connection refused or timeout),
  **when** the failure is detected,
  **then** the UI displays "Erro de conexão" with a "Tentar novamente" button and reuses the same `Idempotency-Key`.

- **Given** `POST /api/v1/payments` returns HTTP 400,
  **when** the response is received,
  **then** the UI returns to Step 1 with the server-provided error message displayed beneath the relevant field.

- **Given** 120 seconds elapse with no terminal state from polling,
  **when** the timeout triggers,
  **then** the UI displays the UNKNOWN timeout state: "Pagamento em verificação. Você receberá uma confirmação em breve."

### Form Validation

- **Given** the `card_token_id` field is empty,
  **when** the customer attempts to submit,
  **then** the "Pagar" button remains disabled and an inline validation message "Campo obrigatório" appears on focus-out.

- **Given** `customer_email` is present and not a valid email format,
  **when** the field loses focus,
  **then** an inline error "E-mail inválido" appears beneath the field.

### Idempotency

- **Given** a checkout session has an `Idempotency-Key` stored in `sessionStorage`,
  **when** the customer retries (network error path),
  **then** the same key is sent in the `Idempotency-Key` header; no new key is generated.

### Accessibility

- **Given** any screen in the checkout flow,
  **then** all interactive elements have visible focus rings, color contrast ratios meet WCAG AA (≥ 4.5:1 for text, ≥ 3:1 for UI components), and the step indicator is navigable by keyboard.

### Responsiveness

- **Given** a viewport width of 375px,
  **then** the checkout form, step indicator, and all terminal screens render without horizontal scrolling and without overlapping elements.

---

## Interface and Data Contracts

### POST /api/v1/payments

**Request headers:**
```
Authorization: Bearer {api_key}
Content-Type: application/json
Idempotency-Key: req_{uuid-v4}
```

**Request body:**
```json
{
  "amount": 24990,
  "currency": "BRL",
  "idempotency_key": "550e8400-e29b-41d4-a716-446655440000",
  "payment_method": {
    "card_token_id": "tok_sandbox_abc123",
    "masked_card": "411111XXXXXX1111"
  },
  "customer_email": "cliente@example.com"
}
```

**Response — 200 COMPLETED:**
```json
{
  "transaction_id": "8fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "COMPLETED",
  "amount": 24990,
  "currency": "BRL",
  "masked_card": "411111XXXXXX1111",
  "created_at": "2026-06-23T14:00:00Z",
  "updated_at": "2026-06-23T14:00:01Z"
}
```

**Response — 202 CHALLENGE_PENDING:**
```json
{
  "transaction_id": "8fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "CHALLENGE_PENDING",
  "amount": 24990,
  "currency": "BRL",
  "challenge_id": "ch_abc123",
  "acs_url": "https://bank.example.com/acs/challenge?token=..."
}
```

**Response — 200 DECLINED:**
```json
{
  "transaction_id": "8fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "DECLINED",
  "amount": 24990,
  "currency": "BRL",
  "message": "Insufficient funds"
}
```

### GET /api/v1/payments/{transaction_id}

**Request headers:**
```
Authorization: Bearer {api_key}
```

**Response (polling):**
```json
{
  "transaction_id": "8fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "COMPLETED",
  "amount": 24990,
  "currency": "BRL",
  "masked_card": "411111XXXXXX1111",
  "updated_at": "2026-06-23T14:00:05Z"
}
```

Terminal states that stop polling: `COMPLETED`, `DECLINED`, `FAILED`.

---

## Tech Stack and Constraints

- **Framework:** Next.js 14 (App Router), TypeScript strict mode
- **Styling:** TailwindCSS with tokens mapped from `design.json`; no inline styles
- **Components:** shadcn/ui primitives overridden with design.json tokens
- **State:** TanStack Query for server state (payment mutation + polling); React Hook Form + Zod for form validation
- **Fonts:** Inter (primary), Geist (fallback), system-ui
- **Target browsers:** Chromium 120+, Firefox 121+, Safari 17+ (mobile Safari priority)
- **No raw card data:** `card_token_id` is a pre-tokenized opaque string; the frontend never handles PAN, CVV, or expiry
- **Design standards:** All visual decisions must derive from `design.json` tokens; no one-off hardcoded colors or sizes
- **Accessibility:** WCAG 2.1 AA minimum; focus management required on step transitions

---

## Examples

### Example 1 — Successful low-risk payment

**Input (Step 1 form):**
```
amount:        24990 (R$ 249,90)
currency:      BRL
card_token_id: tok_sandbox_success
masked_card:   411111XXXXXX1111
customer_email: (empty)
```

**Flow:** Step 1 → submit → Step 2 (loading ~300ms) → POST 200 COMPLETED → Success screen

**Success screen displays:**
```
✓ Pagamento confirmado
Transação: 8fa85f64-...
Valor: R$ 249,90
Cartão: 411111XXXXXX1111
```

### Example 2 — High-risk payment requiring 3DS

**Input:** same as above with `card_token_id: tok_sandbox_highrisk`

**Flow:** Step 1 → submit → Step 2 (loading) → POST 202 CHALLENGE_PENDING → redirect to acs_url → customer authenticates → return to checkout → Step 2 ("Verificando autenticação...") → polling → GET COMPLETED → Success screen

### Example 3 — Network retry

**Flow:** Step 1 → submit → Step 2 (loading) → network error → error state with "Tentar novamente" → click retry → Step 2 (loading, same Idempotency-Key) → POST 200 COMPLETED → Success screen
