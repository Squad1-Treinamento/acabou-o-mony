---
id: task-020
status: planned
links:
  - spec/specs/spec-007-merchant-dashboard.md
  - spec/tasks/front-end-paty/task-017-dashboard-sidebar-layout.md
---

# Webhooks Page

Criar a página `/dashboard/webhooks` que exibe um log simulado de eventos de webhook derivados das transações em estado terminal (COMPLETED, DECLINED, FAILED).

## Local Context

- **Branch:** `front-end-paty`
- **Spec:** spec-007 §Webhooks Page
- **File to create:** `frontend/app/(dashboard)/dashboard/webhooks/page.tsx`

## Event Derivation

```typescript
type WebhookEventType = "payment.completed" | "payment.declined" | "payment.failed";

interface WebhookEvent {
  id: string;           // transaction_id
  event: WebhookEventType;
  transaction_id: string;
  amount: number;
  currency: string;
  timestamp: string;    // updated_at from PaymentResponse
}

const EVENT_MAP: Record<string, WebhookEventType> = {
  COMPLETED: "payment.completed",
  DECLINED:  "payment.declined",
  FAILED:    "payment.failed",
};

function deriveEvents(transactions: PaymentResponse[]): WebhookEvent[] {
  return transactions
    .filter(t => t.status in EVENT_MAP)
    .map(t => ({
      id: t.transaction_id,
      event: EVENT_MAP[t.status],
      transaction_id: t.transaction_id,
      amount: t.amount,
      currency: t.currency,
      timestamp: t.updated_at,
    }))
    .sort((a, b) => b.timestamp.localeCompare(a.timestamp)); // newest first
}
```

## Event Type Chip Colors

| Event | Chip bg | Chip text | Dot color |
|---|---|---|---|
| `payment.completed` | `#EAF7F0` | `#1F8F53` | `bg-emerald-400` |
| `payment.declined` | `rgba(220,38,38,0.08)` | `#DC2626` | `bg-red-400` |
| `payment.failed` | `rgba(220,38,38,0.08)` | `#DC2626` | `bg-red-400` |

## Page Layout

```
Page header: "Webhooks" + event count + "Atualizar" button
Filter chips: [payment.completed] [payment.declined] [payment.failed] (multi-select toggle)
Event list table:
  - Event type chip
  - Transaction ID (truncated, links to /dashboard/transactions/{id})
  - Valor
  - Data do evento (relative)
  - Status chip: "✓ Entregue" with green dot (always delivered in simulation)
```

### Filter logic
Selected event types filter the `events` array. No types selected = show all. `selectedTypes` is `Set<WebhookEventType>`.

## Acceptance Criteria and Tests

- Success: Only COMPLETED, DECLINED, FAILED transactions produce events
- Success: Events sorted newest first (by `updated_at`)
- Success: Filter chips toggle reactively without API calls
- Success: No filter selected = all events shown
- Success: Transaction ID links point to correct detail page
- Success: Empty state shown when `events.length === 0` after filter
- Failure: UNKNOWN, PROCESSING, or other non-terminal transactions appearing as events

## Constraints

- No new API calls — client-side derivation from `useTransactionList()`
- Do NOT add new npm dependencies
- "Entregue" status is always shown (simulation — no real webhook delivery data)
