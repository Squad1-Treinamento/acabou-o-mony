---
id: task-014
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/specs/spec-007-merchant-dashboard.md
---

# Wire Dashboard Routes: Login, Dashboard, and Transaction Detail

Criar as páginas de rota do dashboard (login, lista de transações, detalhe) e os componentes `ApiKeyForm` e `TransactionLookup`.

## Local Context

**Files to create:**
- `frontend/app/(dashboard)/login/page.tsx`
- `frontend/app/(dashboard)/dashboard/page.tsx`
- `frontend/app/(dashboard)/dashboard/transactions/[id]/page.tsx`
- `frontend/components/dashboard/ApiKeyForm.tsx`
- `frontend/components/dashboard/TransactionLookup.tsx`

**Local dependencies:**
- `next/navigation`: `useRouter`, `useSearchParams`
- `frontend/hooks/useTransactions.ts` (task-012)
- `frontend/components/dashboard/FilterBar.tsx`, `TransactionTable.tsx`, `TransactionDetail.tsx` (tasks 012–013)
- `frontend/components/ui/input.tsx`, `button.tsx`, `card.tsx` (task-003)
- `frontend/lib/api/client.ts`: `getApiKey` (task-005)
- `frontend/lib/api/payments.ts`: `getPayment` (for key validation) (task-005)

## Scope

### ApiKeyForm.tsx (`'use client'`)

1. Props: none.
2. Local state: `apiKey: string`, `error: string | null`, `isLoading: boolean`.
3. Input labeled "Chave de API" (`type="password"` to hide the key visually, `autoComplete="off"`).
4. "Entrar" button — disabled while `isLoading`.
5. `onSubmit`: store key in `sessionStorage.setItem('mony_api_key', apiKey)`, then call `getPayment('test')` to validate (will 401 if invalid). On 401: clear key, set `error = 'Chave de API inválida'`. On success/404 (key works): `router.push('/dashboard')`.
6. Press Enter in input submits the form.
7. Error message rendered below input in `text-error text-[13px]`.

### `app/(dashboard)/login/page.tsx` (`'use client'`)

- On mount: if `sessionStorage.getItem('mony_api_key')` exists, redirect to `/dashboard`.
- Renders centered layout with `ApiKeyForm` and a product heading "Acabou o Mony" above the form.
- Max width `480px` centered, matching design.json `contentWidth`.

### TransactionLookup.tsx (`'use client'`)

1. Props: `onSearch: (ids: string[]) => void`.
2. `<textarea>` labeled "IDs de transação" with placeholder "Cole um ou mais IDs, separados por vírgula ou nova linha".
3. "Buscar" button — on click: parses textarea value by splitting on commas and newlines, trims whitespace, deduplicates, calls `onSearch(parsedIds)`.
4. Renders below the page header.

### `app/(dashboard)/dashboard/page.tsx` (`'use client'`)

1. On mount: if no `mony_api_key` in sessionStorage, redirect to `/login`.
2. State: `transactionIds: string[]`, `sort`, `page` (defaults: sort by `created_at desc`, page 1).
3. Filter state managed in URL search params (`useSearchParams` + `useRouter`):
   - Params: `statuses` (comma-separated), `dateFrom`, `dateTo`, `idPrefix`, `page`.
4. Passes IDs to `useTransactions(transactionIds)`.
5. Applies `filterTransactions`, `sortTransactions`, `paginateTransactions` to results.
6. Renders:
   - Page heading "Transações".
   - `<TransactionLookup onSearch={setTransactionIds} />`.
   - `<FilterBar filters={filters} onChange={updateFilters} />`.
   - Refresh button (top-right): calls `refetch()` from `useTransactions`.
   - `<TransactionTable transactions={paginatedResults} isLoading={isLoading} onRowClick={(id) => router.push(`/dashboard/transactions/${id}?${currentParams}`)} />`.

### `app/(dashboard)/dashboard/transactions/[id]/page.tsx` (`'use client'`)

1. Reads `params.id` (transaction UUID).
2. Reads `useSearchParams()` to build `backHref = '/dashboard?' + currentParams` (preserves filters).
3. On mount: if no `mony_api_key` in sessionStorage, redirect to `/login`.
4. Renders `<TransactionDetail transactionId={params.id} backHref={backHref} />`.

## Acceptance Criteria and Tests

- Success: Visit `/login` → enter `teste_key` → redirected to `/dashboard`.
- Success: Visit `/login` with invalid key → "Chave de API inválida" appears; no redirect.
- Success: `/dashboard` without `mony_api_key` in sessionStorage → redirected to `/login`.
- Success: Enter transaction IDs in `TransactionLookup` → table populates.
- Success: Apply status filter → table updates; filter state reflected in URL params.
- Success: Click a row → navigates to `/dashboard/transactions/{id}`.
- Success: "← Voltar" from detail page returns to `/dashboard` with same filters active.
- Success: `pnpm build` passes.
- Failure: Filter state lost on navigation → verify URL params are used (not component state).
- Tests: Manual browser e2e verification.

## Constraints and Negative Instructions

- API key must be stored in `sessionStorage` only; NOT `localStorage`, NOT cookies.
- Do NOT store transaction IDs in URL params (can be long and sensitive); use component state.
- Filter state MUST persist in URL params for shareability and back-navigation.
- `ApiKeyForm` must NOT log the key to the console.
- Do NOT validate the API key format client-side; rely on server 401 response.
