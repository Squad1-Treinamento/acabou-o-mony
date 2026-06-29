---
id: spec-007
status: active
links:
  - spec/specs/index.md
  - spec/user-stories/us-001-transaction-processing.md
  - spec/user-stories/us-002-scalability.md
  - spec/specs/spec-001-core-payment-processing.md
  - spec/specs/spec-006-frontend-checkout.md
  - spec/tech-plans/index.md
---

# Merchant Dashboard Specification

This document defines the behavioral requirements, screen states, API integration rules, and acceptance criteria for the merchant-facing transaction management dashboard of the Acabou o Mony payment platform.

The dashboard is a Next.js 14 application (same app as spec-006) that allows merchants to monitor payment transactions, inspect individual transaction details, and observe webhook delivery status. It is strictly read-only: no payments are created or modified through this interface.

This spec must remain aligned with:

- `spec/specs/spec-001-core-payment-processing.md` — payment lifecycle, status values, and API contract
- `spec/specs/spec-006-frontend-checkout.md` — shared frontend app and design system
- `design.json` — canonical design tokens

---

## Context and Primary Objective

- **Context:** Merchants integrate with the Acabou o Mony platform via API. After a payment is processed, merchants need visibility into transaction outcomes, status transitions, and webhook delivery health. This dashboard provides that observability through a browser-based interface authenticated with the merchant's existing API key.
- **Objective:** Give merchants a fast, filterable, real-time view of their payment transactions so they can identify issues (declined payments, reconciliation-pending UNKNOWN transactions, failed webhooks) without querying the API directly.

---

## Functional Requirements (Behavior)

### User Story

As a merchant, I want a dashboard where I can view and filter my transactions by status and date, inspect individual transaction details, and see webhook delivery status, so that I can monitor payment health and resolve issues quickly.

### Business Rules

#### Authentication — API Key Gate

- The dashboard entry point is a login screen with a single "Chave de API" text input and a "Entrar" button.
- On submission, the key is stored exclusively in `sessionStorage` under the key `mony_api_key`. It is never written to `localStorage`, cookies, or any persistent store.
- The stored key is injected into every API request as `Authorization: Bearer {api_key}`.
- If any API call returns HTTP 401, the session key is cleared from `sessionStorage` and the user is redirected to the login screen with the message "Sessão expirada. Insira sua chave novamente."
- There is no password reset, no email verification, and no OAuth flow.
- Pressing Enter in the API key field submits the form.

#### Transaction List

- The primary dashboard view is a paginated transaction table.
- Page size: 20 rows per page.
- Default sort: `created_at` descending (most recent first).
- Available sort columns: `created_at`, `amount`, `status`.
- Columns displayed (in order):
  1. **ID** — first 8 characters of the UUID followed by `…` (e.g., `8fa85f64…`); full UUID on hover via tooltip.
  2. **Status** — colored badge (see Status Badge Rules below).
  3. **Valor** — locale-formatted amount (e.g., `R$ 249,90` for `amount=24990, currency=BRL`).
  4. **Moeda** — ISO 4217 currency code (e.g., `BRL`).
  5. **Data** — relative timestamp (e.g., "há 3 minutos"); absolute ISO timestamp on hover via tooltip.
  6. **Cartão** — `masked_card` value if present; `—` otherwise.
- Filters available above the table:
  - **Status** (multi-select chip group): all 9 values — `CREATED`, `VALIDATED`, `PROCESSING`, `COMPLETED`, `DECLINED`, `FAILED`, `UNKNOWN`, `CHALLENGE_PENDING`, `AUTHENTICATED`.
  - **Data de criação** (date range): from-date and to-date inputs using the browser native date picker.
  - **Buscar por ID** (text input): filters rows whose `transaction_id` starts with the entered prefix (client-side filter on loaded page).
- Filter state persists in URL query parameters so the view is shareable and survives page refresh.
- Applying or clearing a filter resets to page 1.
- **UNKNOWN transactions**: rows with `status == "UNKNOWN"` receive a `background: rgba(217,119,6,0.08)` row highlight and display an additional inline badge "Em reconciliação" in warning color `#D97706`.
- **Loading state**: while data is fetching, display 5 skeleton rows of the same column widths. Skeleton rows use an animated shimmer effect.
- **Empty state**: when no transactions match the active filters, display centered text "Nenhuma transação encontrada" in `textSecondary` color `#6B7280` with a sub-line "Ajuste os filtros ou verifique novamente mais tarde."
- **Refresh**: a "Atualizar" icon button in the top-right of the table header triggers a manual refetch.
- **Auto-refresh**: every 30 seconds, silently refetch the current page if any visible transaction is in a non-terminal state (`CREATED`, `VALIDATED`, `PROCESSING`, `CHALLENGE_PENDING`, `AUTHENTICATED`, `UNKNOWN`). Auto-refresh must not reset scroll position or clear active filters.
- Clicking any row navigates to the Transaction Detail view for that transaction.
- Below 768px viewport width, the table collapses into a vertical card list (one card per transaction), displaying: status badge, ID (truncated), amount, and date.

#### Transaction Detail

- Accessible via row click in the transaction list. Rendered as a dedicated route `/dashboard/transactions/{transaction_id}`.
- A "← Voltar" link returns to the transaction list, preserving the previous filter state.
- Displays all available fields:
  - Transaction ID (full UUID, copyable via click-to-copy icon)
  - Status badge
  - Valor (formatted)
  - Moeda
  - Cartão mascarado (or `—`)
  - Card Token ID
  - Acquirer Reference (or `—`)
  - Challenge ID (or `—`, only shown when `challenge_id` is present in the response)
  - Criado em (ISO timestamp)
  - Atualizado em (ISO timestamp)
- **Status display**: if `status` is `UNKNOWN`, display a yellow alert box: "Este pagamento está aguardando reconciliação com o adquirente. O status final será atualizado automaticamente."
- **Webhook section**: displays webhook delivery information if available in the response payload. Fields: event type, delivery status (`PENDING` / `DELIVERED` / `FAILED`), retry count (0–5), last attempt timestamp. If no webhook data is available in the API response, omit this section entirely (do not show an empty section).
- The detail view polls `GET /api/v1/payments/{id}` every 10 seconds while the transaction is in a non-terminal state, updating the displayed fields in place without a full page reload.

#### Status Badge Rules

| Status | Text color | Background | Label |
|---|---|---|---|
| `COMPLETED` | `#1F8F53` | `#EAF7F0` | Concluído |
| `DECLINED` | `#DC2626` | `rgba(220,38,38,0.08)` | Recusado |
| `FAILED` | `#DC2626` | `rgba(220,38,38,0.08)` | Falhou |
| `PROCESSING` | `#FFFFFF` | `#0D2B1E` | Processando |
| `AUTHENTICATED` | `#FFFFFF` | `#0D2B1E` | Autenticado |
| `UNKNOWN` | `#D97706` | `rgba(217,119,6,0.08)` | Desconhecido |
| `CHALLENGE_PENDING` | `#6B7280` | `#F0F2F4` | Desafio 3DS |
| `CREATED` | `#6B7280` | `#F0F2F4` | Criado |
| `VALIDATED` | `#6B7280` | `#F0F2F4` | Validado |

All badge text must meet WCAG AA contrast ratio (≥ 4.5:1) against its background color.

---

## Acceptance Criteria (BDD)

### Authentication

- **Given** the merchant is on the login screen and enters a valid API key,
  **when** they press Enter or click "Entrar",
  **then** the key is stored in `sessionStorage` under `mony_api_key` and the dashboard transaction list is loaded.

- **Given** the merchant enters an invalid API key,
  **when** the first API call returns HTTP 401,
  **then** the login screen is displayed with the inline message "Chave de API inválida" and the key is not stored.

- **Given** the merchant is on the dashboard and their session key becomes invalid (401 from any request),
  **when** the 401 is received,
  **then** `mony_api_key` is removed from `sessionStorage` and the user is redirected to the login screen with "Sessão expirada. Insira sua chave novamente."

### Transaction List

- **Given** the merchant is authenticated and the dashboard loads,
  **when** the transaction list fetches,
  **then** skeleton rows are displayed within 100ms and the real data replaces them within 1 second of page load (assuming backend response under 500ms).

- **Given** the transaction list is loaded with multiple pages,
  **when** the merchant clicks the next-page control,
  **then** the table updates to show the next 20 rows and scroll position resets to the top of the table.

- **Given** the merchant applies a status filter of `UNKNOWN`,
  **when** the filter is applied,
  **then** only transactions with `status == "UNKNOWN"` are displayed, each row has the warning background highlight, and the "Em reconciliação" inline badge is visible.

- **Given** no transactions match the active filters,
  **when** the filtered result set is empty,
  **then** the empty state message "Nenhuma transação encontrada" is displayed in place of the table.

- **Given** at least one visible transaction is in a non-terminal state,
  **when** 30 seconds elapse,
  **then** the list silently refetches; scroll position and active filters are unchanged.

- **Given** a viewport width of 375px,
  **when** the dashboard is loaded,
  **then** each transaction is rendered as a card (not a table row) showing status badge, truncated ID, formatted amount, and relative date.

- **Given** the merchant enters a transaction ID prefix in "Buscar por ID",
  **when** 300ms debounce elapses after the last keystroke,
  **then** only transactions whose ID starts with the entered prefix are shown.

### Transaction Detail

- **Given** the merchant clicks a transaction row,
  **when** navigation completes,
  **then** the detail page displays the full transaction data with no full page reload (client-side navigation).

- **Given** the transaction has `status == "UNKNOWN"`,
  **when** the detail page renders,
  **then** a yellow alert box with the reconciliation message is visible at the top of the details section.

- **Given** the transaction has `challenge_id` present in the API response,
  **when** the detail page renders,
  **then** the "Challenge ID" field is displayed; if absent, the field is not rendered at all.

- **Given** the transaction is in a non-terminal state and the detail page is open,
  **when** 10 seconds elapse,
  **then** `GET /api/v1/payments/{id}` is called and any changed fields (status, updated_at) are updated in place.

- **Given** the merchant clicks "← Voltar",
  **when** navigation completes,
  **then** the transaction list is shown with the same filters and page that were active before the detail was opened.

- **Given** the merchant clicks the copy icon next to the transaction ID,
  **when** the click is registered,
  **then** the full UUID is copied to the clipboard and a brief "Copiado!" tooltip appears for 1.5 seconds.

### Status Badges

- **Given** any transaction with `status == "COMPLETED"`,
  **then** the badge displays "Concluído" in `#1F8F53` on `#EAF7F0` background.

- **Given** any transaction with `status == "UNKNOWN"`,
  **then** the badge displays "Desconhecido" in `#D97706` on `rgba(217,119,6,0.08)` background with contrast ratio ≥ 4.5:1 verified.

### Accessibility

- **Given** any screen in the dashboard,
  **then** all interactive elements (table rows, buttons, filters, pagination controls) are keyboard-navigable, have visible focus rings, and include appropriate ARIA labels.

- **Given** the transaction table,
  **then** it uses semantic `<table>` markup with `<th scope="col">` headers for screen reader compatibility.

---

## Interface and Data Contracts

### GET /api/v1/payments/{transaction_id}

**Request headers:**
```
Authorization: Bearer {api_key}
```

**Response:**
```json
{
  "transaction_id": "8fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "COMPLETED",
  "amount": 24990,
  "currency": "BRL",
  "masked_card": "411111XXXXXX1111",
  "card_token_id": "tok_sandbox_abc123",
  "acquirer_reference": "MP-123456",
  "challenge_id": null,
  "created_at": "2026-06-23T14:00:00Z",
  "updated_at": "2026-06-23T14:00:01Z",
  "idempotency_key": "550e8400-e29b-41d4-a716-446655440000",
  "message": null
}
```

**Note:** A list endpoint (`GET /api/v1/payments`) does not currently exist in the backend. The dashboard's MVP transaction list is populated by a merchant-entered transaction ID lookup. The list view shows the result of individual lookups cached in the current session. A future backend enhancement should add a paginated list endpoint.

### MVP List Behavior (without list endpoint)

The dashboard provides a **Transaction ID lookup** input in addition to the table. The merchant enters one or more transaction IDs (comma-separated or one per line). The frontend calls `GET /api/v1/payments/{id}` for each ID, aggregates results, and renders them as a table. Results are cached in component state for the session duration.

---

## Extended Features (Phase 2)

### Dashboard Navigation

A persistent left sidebar replaces the top `AppHeader` on desktop (≥ 768px). On mobile, a compact top bar shows logo and key links.

**Sidebar nav items (in order):**
| Icon | Label | Route | Active rule |
|---|---|---|---|
| LayoutDashboard | Transações | `/dashboard` | exact match |
| TrendingUp | Analytics | `/dashboard/analytics` | prefix |
| RefreshCw | Reconciliação | `/dashboard/reconciliation` | prefix |
| Webhook | Webhooks | `/dashboard/webhooks` | prefix |
| Settings | Configurações | `/dashboard/settings` | prefix |

Active item: `bg-[#0D2B1E]/[0.07]` background, `text-[#0D2B1E]` font, medium weight.
Inactive item: `text-slate-500` hover → `text-slate-800 bg-slate-50`.

Sidebar footer: masked API key display (`••••••••`) + Logout button.
Auth check moved to shared `dashboard/layout.tsx` — each page no longer needs its own `useEffect` guard.

### Analytics Page (`/dashboard/analytics`)

Purpose: give the merchant a read-only revenue and volume intelligence view derived entirely from the existing `GET /api/v1/payments` list endpoint (client-side aggregation — no dedicated analytics API).

**KPI cards (row of 4):**
| Metric | Computation |
|---|---|
| Receita total | sum of `amount` where `status === "COMPLETED"` |
| Transações | count of all transactions |
| Ticket médio | revenue total / completed count (or `—`) |
| Taxa de aprovação | completed / total × 100% (or `—`) |

**Revenue by day (bar chart):**
- X-axis: last 14 calendar days (ISO `YYYY-MM-DD`)
- Y-axis: sum of `amount` for `COMPLETED` transactions on that day
- Bar height proportional to max value in the window
- Tooltip on hover: formatted date + formatted amount
- Built with CSS flexbox (no external chart library)

**Status distribution (horizontal bar chart):**
- One row per status group: Concluídas, Em andamento, Recusadas/Falhas, Reconciliação
- Width proportional to percentage of total; color matches KPI bar colors
- Count + percentage shown right-aligned

**Top transactions table:**
- 5 highest-value `COMPLETED` transactions
- Columns: ID (truncated), Valor, Data
- Each row links to `/dashboard/transactions/{id}`

**Acceptance criteria:**
- All data derived from `useTransactionList()` — no additional API calls
- Empty state if no transactions loaded yet
- Handles 0 completed transactions gracefully (shows `—` where division would occur)

### Reconciliation Page (`/dashboard/reconciliation`)

Purpose: surface all `UNKNOWN` transactions and explain the automatic reconciliation process.

**Info banner (always visible):**
"A reconciliação automática consulta o adquirente a cada 5 minutos e resolve o status para COMPLETED ou DECLINED. Nenhuma ação manual é necessária."

**Stats row:**
- Total em reconciliação: count of `UNKNOWN` transactions
- Mais antigo: relative time of the oldest `UNKNOWN` created_at
- Valor total exposto: sum of `amount` for all `UNKNOWN` transactions

**Table of UNKNOWN transactions:**
- Columns: ID (truncated + link), Valor, Tempo no status, Ações (link to detail)
- "Tempo no status" = `Date.now() - new Date(created_at)` formatted as "Xh Ymin" or "Ymin"
- Sorted by `created_at` ascending (oldest first = highest risk)
- Auto-refreshes every 30 seconds (uses `useTransactionList`)

**Empty state:**
- Icon: CheckCircle (emerald)
- "Nenhuma transação em reconciliação"
- Sub-text: "Todas as transações têm status determinado."

**Acceptance criteria:**
- Shows only `status === "UNKNOWN"` rows
- Age computed client-side; refreshes with each re-render cycle
- Empty state shown when filter returns zero results

### Webhooks Page (`/dashboard/webhooks`)

Purpose: show a simulated webhook event log derived from terminal transactions. Since the backend outbox is not directly exposed via a public API, events are synthesized client-side from `PaymentResponse` data.

**Event derivation rules:**
| Transaction status | Event type | Event payload timestamp |
|---|---|---|
| `COMPLETED` | `payment.completed` | `updated_at` |
| `DECLINED` | `payment.declined` | `updated_at` |
| `FAILED` | `payment.failed` | `updated_at` |

Non-terminal transactions do not generate webhook events.

**Event list columns:**
- Event type (colored chip: green for completed, red for declined/failed)
- Transaction ID (truncated, links to detail)
- Valor
- Data do evento (relative)
- Delivery status (always "Entregue" with green dot — simulated)

**Filter chips above list:**
- `payment.completed` / `payment.declined` / `payment.failed` (toggle, multi-select)

**Empty state:** "Nenhum evento de webhook encontrado" when no terminal transactions or all filtered out.

**Acceptance criteria:**
- Events derived only from terminal transactions (COMPLETED, DECLINED, FAILED)
- Sorted by `updated_at` descending (most recent first)
- Filter chips update list reactively without API calls

### Settings Page (`/dashboard/settings`)

Purpose: let the merchant manage their API key, configure a webhook endpoint URL, and set notification preferences — all stored client-side with no additional API.

**Section 1 — Autenticação:**
- Displays current API key from `sessionStorage`, masked by default
- Eye/EyeOff toggle to reveal
- Copy-to-clipboard button
- "Rotacionar chave" button: disabled with tooltip "Em breve" (not implemented in backend MVP)

**Section 2 — Integração (webhook endpoint):**
- Text input for webhook URL (validated: must start with `https://` or `http://localhost`)
- Persisted to `localStorage` under `mony_webhook_url`
- "Salvar" button; success toast shown for 2 seconds on save

**Section 3 — Notificações:**
- Toggle: "Receber alerta ao email quando uma transação for recusada" (`mony_notify_declined`)
- Toggle: "Receber alerta ao email quando houver falha técnica" (`mony_notify_failed`)
- Both persisted to `localStorage`
- Note below toggles: "Configurações de notificação são salvas localmente neste dispositivo."

**Acceptance criteria:**
- API key is read from `sessionStorage` on mount; shown masked by default
- `localStorage` values are read on mount and applied to form state
- Saving webhook URL: validation shows inline error if URL is invalid
- All toggles update `localStorage` immediately on change (no save button for toggles)

---

## Tech Stack and Constraints

- **Framework:** Next.js 14 (App Router), TypeScript strict mode
- **Styling:** TailwindCSS with tokens from `design.json`; no inline styles
- **Components:** shadcn/ui primitives; `<Table>`, `<Badge>`, `<Input>`, `<Button>`, `<Skeleton>`
- **State:** TanStack Query for transaction data fetching and polling; URL search params for filter state (`useSearchParams`)
- **Auth state:** `sessionStorage` only; no context provider needed — read key per-request from sessionStorage
- **Fonts:** Inter (primary), same as checkout
- **Debounce:** ID search input debounced 300ms before filtering
- **Design standards:** all badge colors, row highlights, and spacing derived from `design.json` tokens
- **Accessibility:** WCAG 2.1 AA; semantic table markup; ARIA labels on icon-only buttons

---

## Examples

### Example 1 — Authenticated merchant views transaction list

**Session:** `mony_api_key = "teste_key"` in sessionStorage

**Merchant enters transaction IDs:**
```
8fa85f64-5717-4562-b3fc-2c963f66afa6
9ba15c74-6827-4673-c4fd-3d174g77bgb7
```

**API calls made:**
```
GET /api/v1/payments/8fa85f64-5717-4562-b3fc-2c963f66afa6
GET /api/v1/payments/9ba15c74-6827-4673-c4fd-3d174g77bgb7
```

**Table renders two rows:**
```
| 8fa85f64… | ✓ Concluído   | R$ 249,90 | BRL | há 2 min | 411111XXXXXX1111 |
| 9ba15c74… | ⚠ Desconhecido | R$ 89,00  | BRL | há 5 min | —               |
```

Second row has warning background highlight and "Em reconciliação" badge.

### Example 2 — Transaction Detail for UNKNOWN status

**URL:** `/dashboard/transactions/9ba15c74-6827-4673-c4fd-3d174g77bgb7`

**Detail page displays:**
```
[yellow alert] Este pagamento está aguardando reconciliação com o adquirente.

Transaction ID:  9ba15c74-6827-4673-c4fd-3d174g77bgb7  [copy icon]
Status:          ⚠ Desconhecido
Valor:           R$ 89,00
Moeda:           BRL
Cartão:          —
Card Token ID:   tok_sandbox_timeout
Criado em:       2026-06-23T13:55:00Z
Atualizado em:   2026-06-23T13:55:30Z
```

Polling begins every 10 seconds. When backend reconciliation completes and status becomes COMPLETED, the detail updates in place.

### Example 3 — Session expiry

**Merchant is viewing the dashboard. Backend rotates API keys.**

**Trigger:** auto-refresh calls `GET /api/v1/payments/{id}` → 401

**Result:** `mony_api_key` removed from sessionStorage → redirect to login screen → "Sessão expirada. Insira sua chave novamente."
