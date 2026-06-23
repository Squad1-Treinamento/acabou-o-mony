---
id: task-006
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/specs/spec-006-frontend-checkout.md
---

# Build usePayment Hook with TanStack Query

Criar o hook `usePayment` que encapsula a mutation de criação de pagamento, o polling de status e o timeout de 120s usando TanStack Query.

## Local Context

**Files to create:**
- `frontend/hooks/usePayment.ts`

**Files to read BEFORE starting:**
- `core-payment/src/main/java/com/acabouomony/payment/web/PaymentController.java` — verificar se `GET /api/v1/payments/{id}` existe. Se não existir, implementar o hook com um stub que retorna dados mockados e marcar a função `getPayment` como TODO.

**Local dependencies:**
- `@tanstack/react-query`: `useMutation`, `useQuery`
- `frontend/lib/api/payments.ts`: `createPayment`, `getPayment`
- `frontend/types/payment.ts`: `PaymentRequest`, `PaymentResponse`, `TERMINAL_STATUSES`

## Scope

1. Read `PaymentController.java` to confirm `GET /api/v1/payments/{id}` exists. Log a comment in the file if it is missing and use a mock stub.

2. **`useCreatePayment()`**
   - Returns a TanStack `useMutation` wrapping `createPayment()`.
   - `mutationFn`: calls `createPayment(req, idempotencyKey)`.
   - Does NOT manage checkout step transitions directly — exposes `mutate`, `data`, `error`, `isPending`.

3. **`useGetPayment(transactionId: string | null, enabled: boolean)`**
   - Returns a TanStack `useQuery`.
   - `queryFn`: calls `getPayment(transactionId!)` (only called when `enabled` is true).
   - `enabled`: `!!transactionId && enabled`.
   - `staleTime`: `0` (always re-fetch).
   - `refetchInterval`: function receiving current data — returns `false` if `data?.status` is in `TERMINAL_STATUSES`, else returns `2000` (2 seconds).
   - `retry`: `false` (do not retry on error; surface errors immediately).

4. **`usePaymentTimeout(isPolling: boolean): boolean`**
   - Uses `useEffect` + `setTimeout` of 120000ms.
   - Starts timer when `isPolling` becomes `true`; clears when `isPolling` becomes `false`.
   - Returns `true` when timeout fires (triggering UNKNOWN_TIMEOUT error state in CheckoutShell).
   - Resets to `false` when `isPolling` becomes `false`.

## Acceptance Criteria and Tests

- Success: `useCreatePayment` returns mutation with correct shape (`mutate`, `data`, `error`, `isPending`).
- Success: `useGetPayment` with a mock that returns `status: 'PROCESSING'` polls every 2s.
- Success: `useGetPayment` with a mock returning `status: 'COMPLETED'` stops polling (refetchInterval returns false).
- Success: `usePaymentTimeout` returns `true` after 120s when `isPolling=true` (manual test with `jest.useFakeTimers` or setTimeout log).
- Success: `pnpm build` passes.
- Failure: Hook manages step state internally → move step logic to CheckoutShell.
- Tests: No automated tests required; verify behavior manually or with React DevTools.

## Constraints and Negative Instructions

- Do NOT manage checkout step state (FORM/PROCESSING/SUCCESS/ERROR) inside this hook; that belongs in `CheckoutShell`.
- Do NOT set `retry: true`; payment errors should surface immediately.
- Do NOT import from `components/`; hooks must be free of UI dependencies.
- `useGetPayment` must be a no-op (disabled) when `transactionId` is null.
- `usePaymentTimeout` must clear its timer on unmount to prevent memory leaks.
