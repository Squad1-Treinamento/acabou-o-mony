---
id: task-007
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/specs/spec-006-frontend-checkout.md
---

# Build CheckoutShell and StepPaymentForm

Criar o componente container do checkout (máquina de estados de step) e o formulário de pagamento do Step 1.

## Local Context

**Files to create:**
- `frontend/components/checkout/CheckoutShell.tsx`
- `frontend/components/checkout/StepPaymentForm.tsx`

**Local dependencies:**
- `react-hook-form`, `zod`, `@hookform/resolvers/zod`
- `frontend/hooks/usePayment.ts` (task-006)
- `frontend/components/shared/StepIndicator.tsx` (task-004)
- `frontend/components/shared/SecurityBadge.tsx` (task-004)
- `frontend/components/ui/button.tsx`, `input.tsx`, `card.tsx`, `label.tsx` (task-003)
- `frontend/lib/utils/formatters.ts`: `formatCurrency` (task-005)
- `frontend/lib/utils/idempotency.ts`: `getOrCreateCheckoutKey` (task-005)
- `frontend/types/payment.ts` (task-005)

## Scope

### CheckoutShell.tsx (`'use client'`)

1. Internal state: `step` of type `'FORM' | 'PROCESSING' | 'THREE_DS' | 'SUCCESS' | 'ERROR'`.
2. Internal state: `errorType` of type `'DECLINED' | 'FAILED' | 'NETWORK' | 'UNKNOWN_TIMEOUT' | null`.
3. Internal state: `paymentResponse: PaymentResponse | null`.
4. Props: `amount: number`, `currency: string`, `cardTokenId?: string`, `maskedCard?: string`.
5. On mount (`useEffect`, empty deps): read `sessionStorage.getItem('checkout_transaction_id')`. If present AND `window.location.search` includes `3ds_complete=true`, set `step = 'PROCESSING'` and set `transactionId` state.
6. Consumes `useCreatePayment`, `useGetPayment`, `usePaymentTimeout`.
7. Renders `StepIndicator` at top with `steps={['Dados', 'Processando', 'Confirmação']}` and `currentStep` mapped from step state (FORM=0, PROCESSING=1, THREE_DS=1, SUCCESS=2, ERROR=2).
8. Below StepIndicator: renders the active step component. Step components are NOT rendered here yet (tasks 008–010); add placeholders with `<div>` for each step.
9. `onFormSubmit(values)`: calls `createPayment` mutation with `getOrCreateCheckoutKey()`, transitions to `'PROCESSING'`.
10. `onPaymentResult(response)`: transitions to `'SUCCESS'`, `'ERROR'` (DECLINED/FAILED), or `'THREE_DS'` (CHALLENGE_PENDING) based on response status.
11. Handles `usePaymentTimeout` — transitions to `'ERROR'` with `errorType='UNKNOWN_TIMEOUT'` when timeout fires.

### StepPaymentForm.tsx (`'use client'`)

1. Props: `amount: number`, `currency: string`, `maskedCard?: string`, `defaultCardTokenId?: string`, `onSubmit: (values: { card_token_id: string; customer_email?: string }) => void`.
2. Zod schema:
   - `card_token_id`: `z.string().min(1, 'Campo obrigatório').max(100, 'Máximo 100 caracteres')`
   - `customer_email`: `z.string().email('E-mail inválido').optional().or(z.literal(''))`
3. Payment summary card: formatted amount (`formatCurrency(amount, currency)`), currency label.
4. Masked card display (read-only `<p>` tag, only rendered when `maskedCard` prop is defined).
5. `card_token_id` Input with Label "Token do cartão". Shows inline error message below on invalid.
6. `customer_email` Input with Label "E-mail (opcional)". Shows inline error on blur when invalid.
7. `SecurityBadge` below inputs.
8. Submit Button "Pagar" — disabled when `card_token_id` is empty or form has validation errors.

## Acceptance Criteria and Tests

- Success: `CheckoutShell` renders `StepIndicator` with step 0 active on initial load.
- Success: On mount with `checkout_transaction_id` in sessionStorage + `?3ds_complete=true` in URL, shell transitions to `'PROCESSING'`.
- Success: `StepPaymentForm` submit button is disabled with empty `card_token_id`.
- Success: `StepPaymentForm` shows "E-mail inválido" on blur with malformed email.
- Success: `StepPaymentForm` calls `onSubmit` with correct values when valid.
- Success: `pnpm build` passes.
- Failure: Step indicator shows wrong step number → verify currentStep mapping.
- Tests: No automated tests required; verify in browser with DevTools.

## Constraints and Negative Instructions

- `CheckoutShell` must NOT call `getPayment` directly; use `useGetPayment` hook.
- Step component placeholders must be `<div>` elements — do NOT import step components from tasks 008–010 (not yet created).
- Do NOT skip the sessionStorage check on mount (3DS return path depends on it).
- `StepPaymentForm` must be a pure controlled component; no internal API calls.
- Do NOT use `defaultValues` with server data; read from props only.
