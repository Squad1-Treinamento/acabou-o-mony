---
id: task-018
status: planned
links:
  - spec/specs/spec-007-merchant-dashboard.md
  - spec/tasks/front-end-paty/task-017-dashboard-sidebar-layout.md
---

# Analytics Page

Criar a página `/dashboard/analytics` com KPIs de receita, gráfico de volume diário (últimos 14 dias), distribuição de status e tabela de maiores transações. Toda computação é client-side sobre os dados de `useTransactionList()`.

## Local Context

- **Branch:** `front-end-paty`
- **Spec:** spec-007 §Analytics Page
- **File to create:** `frontend/app/(dashboard)/dashboard/analytics/page.tsx`
- **No new components needed** — tudo inline na page (chart é simples CSS flex)

## Data Computations

```typescript
// Revenue total: sum amount where status === "COMPLETED"
// Ticket médio: revenue / completedCount (show "—" if 0)
// Taxa de aprovação: completedCount / total * 100 (show "—" if 0 total)

// Daily revenue (last 14 days):
function getDailyRevenue(txs: PaymentResponse[], days = 14) {
  const result: Record<string, number> = {};
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    result[d.toISOString().slice(0, 10)] = 0;
  }
  txs.filter(t => t.status === "COMPLETED").forEach(t => {
    const day = t.created_at.slice(0, 10);
    if (day in result) result[day] += t.amount;
  });
  return Object.entries(result).map(([date, amount]) => ({ date, amount }));
}
```

## Components to Build (inline in page)

### RevenueChart
CSS flexbox bar chart:
- `div className="flex items-end gap-1 h-28"` as container
- Each bar: `div` with percentage height = `(amount / max) * 100%`
- Bar color: `#0D2B1E` normal, `#1a4532` on group-hover
- Label below: `MM-DD` (slice(5) of ISO date)
- `minHeight: amount > 0 ? "3px" : "0"`

### StatusDistribution
Horizontal bars for status groups: Concluídas (emerald), Em andamento (#0D2B1E), Recusadas/Falhas (red), Reconciliação (amber).
- `div` with percentage width (status count / total)
- Label + count + percentage right-aligned

### TopTransactions
Table of 5 highest-value COMPLETED transactions:
- Columns: ID (truncated + Link to /dashboard/transactions/{id}), Valor, Data

## Page Layout

```
Page header: "Analytics" title + "Atualizar" button
4 KPI stat cards (same StatCard style as dashboard home but without tooltips or filter click)
Revenue chart card (full width or 2/3)
Status distribution card (1/3 or full width)
Top transactions table card
```

## Acceptance Criteria and Tests

- Success: KPIs update reactively when `useTransactionList` data changes
- Success: Chart shows exactly 14 bars (including days with R$ 0)
- Success: 0 completed txs → ticket médio shows "—", taxa shows "—"
- Success: Top transactions table shows up to 5 rows; empty state if none completed
- Success: Chart renders without errors when `transactions` is empty array
- Failure: Any external chart library import (recharts, chart.js, etc.)
- Failure: Any API call beyond `useTransactionList()`

## Constraints

- No external chart library — pure CSS/SVG only
- Do NOT add new npm dependencies
- Reuse `formatCurrency`, `formatRelativeTime` from `@/lib/utils/formatters`
- Reuse `useTransactionList` from `@/hooks/useTransactionList`
