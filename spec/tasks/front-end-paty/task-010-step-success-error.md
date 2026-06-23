---
id: task-010
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/specs/spec-006-frontend-checkout.md
---

# Build StepSuccess and StepError Terminal Screens

Criar as telas terminais de sucesso e erro do checkout e integrá-las ao `CheckoutShell`.

## Local Context

**Files to create:**
- `frontend/components/checkout/StepSuccess.tsx`
- `frontend/components/checkout/StepError.tsx`

**Files to modify:**
- `frontend/components/checkout/CheckoutShell.tsx` (replace SUCCESS and ERROR placeholder divs, wire retry logic)

**Local dependencies:**
- `lucide-react`: `CheckCircle`, `AlertCircle` icons
- `frontend/components/shared/CopyButton.tsx` (task-004)
- `frontend/components/ui/card.tsx` (task-003)
- `frontend/components/ui/button.tsx` (task-003)
- `frontend/lib/utils/formatters.ts`: `formatCurrency`, `truncateUUID` (task-005)
- `frontend/lib/utils/idempotency.ts`: `clearCheckoutSession` (task-005)
- `frontend/types/payment.ts`: `PaymentResponse` (task-005)

## Scope

### StepSuccess.tsx

1. Props: `response: PaymentResponse`.
2. On mount (`useEffect`): call `clearCheckoutSession()` to remove all checkout sessionStorage keys.
3. Renders:
   - `CheckCircle` icon (48px) in `text-success`.
   - Heading "Pagamento confirmado" (`text-[20px] font-semibold text-textPrimary`, matching design.json h3).
   - Row: "Transação" label + `truncateUUID(response.transaction_id)` + `<CopyButton value={response.transaction_id} />`.
   - Row: "Valor" label + `formatCurrency(response.amount, response.currency)`.
   - Row: "Cartão" label + `response.masked_card` (only rendered if `masked_card` is present).
   - Row: "Data" label + `new Date(response.updated_at).toLocaleString('pt-BR')`.
4. Layout: centered card with `p-8` padding (matching design.json `cardPadding: 32px`), `gap-6` between rows.

### StepError.tsx

1. Props: `errorType: 'DECLINED' | 'FAILED' | 'NETWORK' | 'UNKNOWN_TIMEOUT'`, `message?: string`, `onRetry?: () => void`.
2. Renders `AlertCircle` icon (48px) in `text-error`.
3. Heading and sub-text by `errorType`:
   - `'DECLINED'`: heading "Pagamento recusado", text "Verifique os dados do cartão ou tente com outro método." — NO retry button.
   - `'FAILED'`: heading "Erro no processamento", text "Tente novamente." — retry button rendered.
   - `'NETWORK'`: heading "Erro de conexão", text "Verifique sua conexão e tente novamente." — retry button rendered.
   - `'UNKNOWN_TIMEOUT'`: heading "Pagamento em verificação", text "Você receberá uma confirmação em breve." — NO retry button.
4. Retry button (secondary variant) labeled "Tentar novamente" — rendered only when `onRetry` is defined AND `errorType` is `'FAILED'` or `'NETWORK'`.

### CheckoutShell.tsx modifications

- Replace SUCCESS placeholder with `<StepSuccess response={paymentResponse!} />`.
- Replace ERROR placeholder with `<StepError errorType={errorType!} onRetry={handleRetry} />`.
- `handleRetry()`: resets to `step = 'FORM'`, clears `errorType`, generates a NEW `Idempotency-Key` (remove old from sessionStorage so `getOrCreateCheckoutKey` creates fresh one).

## Acceptance Criteria and Tests

- Success: `StepSuccess` renders all fields; `checkout_idempotency_key` removed from sessionStorage on mount.
- Success: `StepSuccess` does NOT render masked_card row when `masked_card` is undefined.
- Success: `StepError` with `errorType='DECLINED'` shows NO retry button.
- Success: `StepError` with `errorType='FAILED'` shows retry button; clicking it transitions back to FORM.
- Success: `StepError` with `errorType='UNKNOWN_TIMEOUT'` shows NO retry button.
- Success: `pnpm build` passes.
- Failure: sessionStorage not cleared on success → adds `clearCheckoutSession` to `useEffect`.
- Tests: Manual browser verification; no automated tests required.

## Constraints and Negative Instructions

- Do NOT show a retry button for `'DECLINED'` or `'UNKNOWN_TIMEOUT'` errors.
- Do NOT redirect to an external URL from these components.
- `handleRetry` in CheckoutShell MUST generate a new idempotency key (remove the old one first).
- Do NOT render the `masked_card` field with the text "undefined" or "null" — check for presence before rendering.
