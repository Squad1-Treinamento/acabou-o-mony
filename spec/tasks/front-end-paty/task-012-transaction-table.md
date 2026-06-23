---
id: task-012
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/specs/spec-007-merchant-dashboard.md
---

# Build TransactionTable, FilterBar, TransactionCard and useTransactions Hook

Criar o hook de busca de transações em paralelo, a barra de filtros, a tabela de transações com paginação e o card mobile.

## Local Context

**Files to create:**
- `frontend/hooks/useTransactions.ts`
- `frontend/components/dashboard/FilterBar.tsx`
- `frontend/components/dashboard/TransactionTable.tsx`
- `frontend/components/dashboard/TransactionCard.tsx`

**Local dependencies:**
- `@tanstack/react-query`: `useQueries`
- `frontend/lib/api/payments.ts`: `getPayment` (task-005)
- `frontend/types/payment.ts`: `PaymentStatus`, `PaymentResponse`, `NON_TERMINAL_STATUSES` (task-005)
- `frontend/components/shared/EmptyState.tsx`, `LoadingSpinner.tsx` (task-004)
- `frontend/components/ui/skeleton.tsx`, `table.tsx`, `badge.tsx` (task-003)
- `frontend/lib/utils/formatters.ts`: `formatCurrency`, `formatRelativeTime`, `truncateUUID` (task-005)
- Tailwind tokens from task-002

## Scope

### useTransactions.ts

1. Accepts `transactionIds: string[]`.
2. Uses `useQueries` to call `getPayment(id)` for each ID in parallel.
3. Returns `{ results: PaymentResponse[], isLoading: boolean, refetch: () => void }`.
4. `refetchInterval` per query: `30000` if `NON_TERMINAL_STATUSES.includes(data?.status)`, else `false`.
5. Pure filter function `filterTransactions(results, filters)` — exported separately:
   - `filters: { statuses?: PaymentStatus[]; dateFrom?: string; dateTo?: string; idPrefix?: string }`
   - Returns filtered array.
6. Pure sort function `sortTransactions(results, sort)` — exported separately:
   - `sort: { field: 'created_at' | 'amount' | 'status'; direction: 'asc' | 'desc' }`
7. Pure pagination function `paginateTransactions(results, page, pageSize = 20)` — exported separately.

### FilterBar.tsx (`'use client'`)

1. Props: `filters: FilterState`, `onChange: (filters: FilterState) => void`.
2. `FilterState` type: `{ statuses: PaymentStatus[]; dateFrom: string; dateTo: string; idPrefix: string }`.
3. Status chips: one per PaymentStatus value (9 total). Selected = `bg-primary text-white`. Unselected = `bg-divider text-textSecondary`.
4. Date inputs: `type="date"` for from/to.
5. ID search: `type="text"` input, debounced 300ms before calling `onChange`.
6. "Limpar filtros" button: resets all fields to empty/default.

### TransactionTable.tsx (`'use client'`)

1. Props: `transactions: PaymentResponse[]`, `isLoading: boolean`, `onRowClick: (id: string) => void`.
2. Receives pre-filtered, pre-sorted, pre-paginated data — does NOT filter internally.
3. Renders `<Table>` (shadcn) with columns: ID (truncated + Tooltip with full UUID), Status (StatusBadge placeholder `<span>` — StatusBadge created in task-013), Valor, Moeda, Data (relative + Tooltip with absolute), Cartão.
4. UNKNOWN rows: `className="bg-warning/8"` on `<tr>` + inline `<span className="text-warning text-[12px]">Em reconciliação</span>` after the badge.
5. Loading: renders 5 `<Skeleton>` rows with same column widths when `isLoading=true`.
6. Empty: renders `<EmptyState title="Nenhuma transação encontrada" subtitle="Ajuste os filtros ou verifique novamente mais tarde." />` when `transactions.length === 0` and not loading.
7. Mobile (`md:hidden` / `hidden md:table`): renders `<TransactionCard>` list below 768px, table above.
8. Sort: column header click cycles `asc → desc → asc`; active column indicated by arrow icon.
9. Pagination: prev/next/page number controls below table; 20 rows per page.

### TransactionCard.tsx

1. Props: `transaction: PaymentResponse`, `onClick: () => void`.
2. Renders: Card with StatusBadge placeholder `<span>`, truncated ID, `formatCurrency`, `formatRelativeTime`.
3. Full card is clickable (calls `onClick`).

## Acceptance Criteria and Tests

- Success: `useTransactions(['id1', 'id2'])` fires 2 parallel GET requests.
- Success: Auto-refresh fires every 30s when any result has non-terminal status; stops when all are terminal.
- Success: FilterBar chips correctly toggle status filters; `onChange` called with updated state.
- Success: ID search debounces 300ms (verify no call fires on every keystroke).
- Success: UNKNOWN rows have warning background and "Em reconciliação" text.
- Success: Loading state shows 5 skeleton rows.
- Success: Empty state appears when `transactions` is empty array.
- Success: Mobile card list renders below 768px (check with DevTools responsive mode).
- Success: `pnpm build` passes.
- Failure: Filter doesn't update table → verify `onChange` is wired correctly in parent.
- Tests: Manual browser verification; no automated tests required.

## Constraints and Negative Instructions

- `TransactionTable` must NOT call `getPayment` directly; receives pre-processed data via props.
- Filter, sort, and pagination logic must be pure functions (no side effects), testable in isolation.
- Do NOT use infinite scroll; pagination controls only.
- Do NOT add `StatusBadge` import yet (task-013); use `<span>{transaction.status}</span>` as placeholder.
- `useTransactions` must NOT throw on partial failures (some IDs may 404 — handle gracefully with `undefined` in results array).
