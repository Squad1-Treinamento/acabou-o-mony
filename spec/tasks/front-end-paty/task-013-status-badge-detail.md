---
id: task-013
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/specs/spec-007-merchant-dashboard.md
---

# Build StatusBadge and TransactionDetail

Criar o badge de status com mapeamento de cores por tipo e o painel de detalhes de transação com polling e alertas de estado UNKNOWN.

## Local Context

**Files to create:**
- `frontend/components/dashboard/StatusBadge.tsx`
- `frontend/components/dashboard/TransactionDetail.tsx`

**Files to modify:**
- `frontend/components/dashboard/TransactionTable.tsx` — replace status placeholder `<span>` with `<StatusBadge>`
- `frontend/components/dashboard/TransactionCard.tsx` — replace status placeholder `<span>` with `<StatusBadge>`

**Local dependencies:**
- `frontend/components/ui/badge.tsx`, `alert.tsx`, `skeleton.tsx` (task-003)
- `frontend/components/shared/CopyButton.tsx`, `EmptyState.tsx`, `ErrorState.tsx` (task-004)
- `frontend/hooks/usePayment.ts`: `useGetPayment` (task-006)
- `frontend/types/payment.ts`: `PaymentStatus`, `PaymentResponse` (task-005)
- `frontend/lib/utils/formatters.ts`: `formatCurrency`, `truncateUUID` (task-005)
- `next/link` (built-in Next.js)

## Scope

### StatusBadge.tsx

1. Props: `status: PaymentStatus`, `className?: string`.
2. Color map (text + background, per spec-007):
   - `COMPLETED` → `text-success bg-successSoft`
   - `DECLINED`, `FAILED` → `text-error bg-error/8`
   - `PROCESSING`, `AUTHENTICATED` → `text-surface bg-primary`
   - `UNKNOWN` → `text-warning bg-warning/8`
   - `CHALLENGE_PENDING`, `CREATED`, `VALIDATED` → `text-textSecondary bg-divider`
3. Label map (pt-BR):
   - `COMPLETED`=Concluído, `DECLINED`=Recusado, `FAILED`=Falhou, `PROCESSING`=Processando, `AUTHENTICATED`=Autenticado, `UNKNOWN`=Desconhecido, `CHALLENGE_PENDING`=Desafio 3DS, `CREATED`=Criado, `VALIDATED`=Validado
4. Renders using shadcn `Badge` with `className` override applying the status colors.
5. Font: `text-[12px] font-medium`.

### TransactionDetail.tsx (`'use client'`)

1. Props: `transactionId: string`, `backHref: string`.
2. Fetches via `useGetPayment(transactionId, true)` — polling every 10s if non-terminal (override `refetchInterval` to 10000 for detail view).
3. **Loading state**: renders full skeleton (3–4 `<Skeleton>` rows) while `isLoading`.
4. **Error state**: renders `<ErrorState message="Erro ao carregar transação." />` if fetch fails.
5. **Content layout** (when data loaded):
   - "← Voltar" as `<Link href={backHref}>` using Next.js `Link`.
   - Title "Detalhes da transação" (`text-[20px] font-semibold`).
   - `<StatusBadge status={data.status} />`.
   - UNKNOWN alert: if `status === 'UNKNOWN'`, render shadcn `Alert` with yellow colors (`border-warning text-warning`) and message "Este pagamento está aguardando reconciliação com o adquirente. O status final será atualizado automaticamente."
   - Data rows (label + value pairs):
     - ID: `truncateUUID(transaction_id)` + `<CopyButton value={transaction_id} />`
     - Valor: `formatCurrency(amount, currency)`
     - Moeda: `currency`
     - Cartão: `masked_card` or `—`
     - Token: `card_token_id` (or `—`)
     - Referência: `acquirer_reference` (only when present)
     - Challenge ID: `challenge_id` (only when present — do NOT render the row when absent)
     - Criado em: `new Date(created_at).toLocaleString('pt-BR')`
     - Atualizado em: `new Date(updated_at).toLocaleString('pt-BR')`

## Acceptance Criteria and Tests

- Success: `StatusBadge` renders "Concluído" with green text and light green background for COMPLETED.
- Success: `StatusBadge` renders "Desconhecido" with warning color for UNKNOWN.
- Success: All 9 statuses render with correct labels and no undefined/null colors.
- Success: `TransactionDetail` shows skeleton while loading.
- Success: UNKNOWN alert box renders when status is UNKNOWN; absent for all other statuses.
- Success: `challenge_id` row is NOT rendered when field is null/undefined.
- Success: Polling in detail view updates status in place when backend changes (manual test).
- Success: "← Voltar" navigates back to correct URL (verify with browser back button equivalent).
- Success: `pnpm build` passes.
- Failure: Any badge color not meeting WCAG AA → adjust opacity or use solid color variant.
- Tests: Manual browser verification; no automated tests required.

## Constraints and Negative Instructions

- Do NOT hardcode hex colors in `StatusBadge`; use only Tailwind class names mapping to design.json tokens.
- Do NOT render the `acquirer_reference` or `challenge_id` field rows when the value is null/undefined/empty.
- `TransactionDetail` must NOT manage its own `QueryClient`; relies on the shared one from root layout.
- Do NOT use `target="_blank"` on the "← Voltar" link; navigate within same tab.
- Detail view polling interval is 10s (not 2s like checkout); do NOT reuse `useGetPayment` default — override `refetchInterval: 10000` in the hook call options.
