---
id: task-009
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/specs/spec-006-frontend-checkout.md
  - spec/specs/spec-002-3ds-mfa-auth-engine.md
---

# Build StepThreeDs and Wire 3DS Redirect in CheckoutShell

Criar o componente de redirecionamento para o ACS do banco e integrar o fluxo CHALLENGE_PENDING no `CheckoutShell`.

## Local Context

**Files to create:**
- `frontend/components/checkout/StepThreeDs.tsx`

**Files to modify:**
- `frontend/components/checkout/CheckoutShell.tsx` (replace THREE_DS placeholder, add redirect logic on HTTP 202)

**Local dependencies:**
- `frontend/components/shared/LoadingSpinner.tsx` (task-004)
- `frontend/components/shared/ErrorState.tsx` (task-004)
- `frontend/lib/utils/idempotency.ts`: `clearCheckoutSession` is NOT called here (called in StepSuccess)

## Scope

1. **StepThreeDs.tsx** (`'use client'`)
   - Props: `acsUrl: string | null`.
   - On mount (`useEffect`):
     1. If `acsUrl` is null: renders `<ErrorState message="URL de autenticação não encontrada" />` — no redirect.
     2. If `acsUrl` is valid: after 500ms delay (`setTimeout`), set `window.location.href = acsUrl`.
        - The 500ms delay ensures `sessionStorage` writes (done in `CheckoutShell`) complete before navigation.
   - While waiting (before redirect fires): renders `<LoadingSpinner size="lg" />` + text "Redirecionando para autenticação...".
   - Clean up `setTimeout` on unmount.

2. **CheckoutShell.tsx** — add HTTP 202 handling to `onPaymentResult`:
   - When `response.status === 'CHALLENGE_PENDING'`:
     1. `sessionStorage.setItem('checkout_transaction_id', response.transaction_id)`.
     2. `sessionStorage.setItem('checkout_challenge_id', response.challenge_id ?? '')`.
     3. Set `step = 'THREE_DS'`.
     4. Set internal state `acsUrl = response.acs_url ?? null`.
   - Replace THREE_DS placeholder `<div>` with `<StepThreeDs acsUrl={acsUrl} />`.

## Acceptance Criteria and Tests

- Success: With mock `acsUrl = 'https://bank.example/acs'`, component shows spinner for ~500ms then triggers navigation.
- Success: `sessionStorage.checkout_transaction_id` and `sessionStorage.checkout_challenge_id` are written BEFORE `StepThreeDs` mounts (written in `CheckoutShell` before step transition).
- Success: With `acsUrl = null`, `ErrorState` renders immediately with no redirect attempt.
- Success: `setTimeout` is cleared on unmount (no memory leak).
- Success: `pnpm build` passes.
- Failure: Navigation fires before sessionStorage keys are set → increase delay or reorder operations.
- Tests: Manual test with a mock URL that logs navigation; no automated tests required.

## Constraints and Negative Instructions

- Do NOT open the ACS URL in a new tab (`window.open`); use `window.location.href` for same-frame redirect.
- Do NOT use Next.js `router.push` for the ACS redirect — it's an external URL not within the Next.js router.
- Do NOT call `clearCheckoutSession()` here; session cleanup is done in `StepSuccess` after successful completion.
- The 500ms delay is a workaround for sessionStorage timing; do NOT remove it without verifying the write completes before redirect in all browsers.
