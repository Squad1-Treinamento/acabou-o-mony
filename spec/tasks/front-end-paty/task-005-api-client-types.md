---
id: task-005
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/specs/spec-006-frontend-checkout.md
  - spec/specs/spec-007-merchant-dashboard.md
---

# Build API Client and TypeScript Types

Criar os tipos TypeScript do domínio de pagamentos, o cliente HTTP genérico com injeção de auth e tratamento de 401, as funções de API, e os utilitários de formatação e idempotência.

## Local Context

**Files to create:**
- `frontend/types/payment.ts`
- `frontend/types/merchant.ts`
- `frontend/lib/api/client.ts`
- `frontend/lib/api/payments.ts`
- `frontend/lib/utils/formatters.ts`
- `frontend/lib/utils/idempotency.ts`

**Environment variable used:** `NEXT_PUBLIC_API_URL` (from `.env.local`).

**Local dependencies:** none (pure TypeScript + Web APIs).

## Scope

1. **`types/payment.ts`**
   - `PaymentStatus`: string union type — `'CREATED' | 'VALIDATED' | 'PROCESSING' | 'COMPLETED' | 'DECLINED' | 'FAILED' | 'UNKNOWN' | 'CHALLENGE_PENDING' | 'AUTHENTICATED'`
   - `TERMINAL_STATUSES`: `readonly` array = `['COMPLETED', 'DECLINED', 'FAILED']`
   - `NON_TERMINAL_STATUSES`: `readonly` array = remaining 6 values
   - `PaymentMethod`: `{ card_token_id: string; masked_card?: string }`
   - `PaymentRequest`: `{ amount: number; currency: string; idempotency_key: string; payment_method: PaymentMethod; customer_email?: string }`
   - `PaymentResponse`: `{ transaction_id: string; status: PaymentStatus; amount: number; currency: string; masked_card?: string; challenge_id?: string; acs_url?: string; message?: string; created_at: string; updated_at: string; idempotency_key?: string }`
   - `ApiError`: `{ status: number; message: string; field?: string }`

2. **`types/merchant.ts`**
   - `MerchantSession`: `{ apiKey: string }`

3. **`lib/api/client.ts`**
   - `getApiKey(): string | null` — reads `sessionStorage.getItem('mony_api_key')`; returns `null` if not set or on server (guard with `typeof window === 'undefined'`).
   - `apiFetch(path: string, options?: RequestInit): Promise<Response>` — prepends `process.env.NEXT_PUBLIC_API_URL` to `path`; injects `Authorization: Bearer {apiKey}` and `Content-Type: application/json` headers; on 401 response: clears `sessionStorage.removeItem('mony_api_key')` and sets `window.location.href = '/login'`; returns the `Response` for all other status codes.

4. **`lib/api/payments.ts`**
   - `createPayment(req: PaymentRequest, idempotencyKey: string): Promise<PaymentResponse>` — `POST /api/v1/payments` with header `Idempotency-Key: {idempotencyKey}`; parses JSON; throws `ApiError` on 4xx/5xx (not 401 — handled by client.ts).
   - `getPayment(id: string): Promise<PaymentResponse>` — `GET /api/v1/payments/{id}`; parses JSON; throws `ApiError` on error.

5. **`lib/utils/formatters.ts`**
   - `formatCurrency(amount: number, currency: string): string` — divides by 100, uses `Intl.NumberFormat` with locale `pt-BR` and currency style. E.g.: `formatCurrency(24990, 'BRL')` → `'R$ 249,90'`.
   - `formatRelativeTime(isoString: string): string` — computes diff from `Date.now()`, uses `Intl.RelativeTimeFormat` locale `pt-BR`. Picks appropriate unit (minutes/hours/days). E.g.: 3 minutes ago → `'há 3 minutos'`.
   - `truncateUUID(uuid: string): string` — returns `uuid.slice(0, 8) + '…'`.

6. **`lib/utils/idempotency.ts`**
   - `generateIdempotencyKey(): string` — returns `'req_' + crypto.randomUUID()`.
   - `getOrCreateCheckoutKey(): string` — reads `sessionStorage.getItem('checkout_idempotency_key')`; if null, generates a new key, stores it, and returns it.
   - `clearCheckoutSession(): void` — removes `checkout_idempotency_key`, `checkout_transaction_id`, `checkout_challenge_id` from sessionStorage.

## Acceptance Criteria and Tests

- Success: `pnpm build` passes with `strict: true` — no `any`, no implicit nulls.
- Success: `formatCurrency(24990, 'BRL')` returns `'R$ 249,90'` (verify in console).
- Success: `formatCurrency(100, 'USD')` returns `'US$ 1,00'`.
- Success: `truncateUUID('8fa85f64-5717-4562-b3fc-2c963f66afa6')` returns `'8fa85f64…'`.
- Success: `apiFetch` on 401 redirects to `/login` (manual test with invalid key).
- Success: `TERMINAL_STATUSES` includes exactly `['COMPLETED', 'DECLINED', 'FAILED']`.
- Failure: Any `as any` cast → fix with proper typing.
- Tests: Manual console checks for formatters; no automated tests required for this task.

## Constraints and Negative Instructions

- Do NOT use `axios` or any HTTP library; use native `fetch` only.
- Do NOT store the API key in `localStorage`; use `sessionStorage` only.
- Do NOT call `window.*` at module level (SSR guard required: `typeof window !== 'undefined'`).
- Do NOT throw on 401 in `payments.ts`; `client.ts` handles it by redirect.
- `getApiKey()` must handle server-side rendering context (return `null` when `window` is undefined).
