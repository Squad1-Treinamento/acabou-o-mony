---
id: task-019
status: planned
links:
  - spec/specs/spec-007-merchant-dashboard.md
  - spec/tasks/front-end-paty/task-017-dashboard-sidebar-layout.md
---

# Reconciliation Page

Criar a página `/dashboard/reconciliation` que lista todas as transações em status `UNKNOWN`, explica o processo automático e mostra métricas de risco exposto.

## Local Context

- **Branch:** `front-end-paty`
- **Spec:** spec-007 §Reconciliation Page
- **File to create:** `frontend/app/(dashboard)/dashboard/reconciliation/page.tsx`

## Implementation Steps

### Info banner
Always visible, amber style:
```tsx
<div className="flex gap-3 rounded-xl bg-amber-50 border border-amber-200/60 px-4 py-3.5">
  <Info className="w-4 h-4 text-amber-500 mt-0.5 shrink-0" />
  <p className="text-sm text-amber-700">
    A reconciliação automática consulta o adquirente a cada 5 minutos e resolve 
    o status para COMPLETED ou DECLINED. Nenhuma ação manual é necessária.
  </p>
</div>
```

### Stats row (3 cards)
- **Total em reconciliação:** `unknownTxs.length`
- **Mais antigo:** `formatRelativeTime(oldest.created_at)` where oldest = min by created_at; "—" if none
- **Valor total exposto:** `formatCurrency(sum of unknownTxs amounts, "BRL")`

### "Tempo no status" computation
```typescript
function timeInStatus(created_at: string): string {
  const ms = Date.now() - new Date(created_at).getTime();
  const mins = Math.floor(ms / 60_000);
  const hours = Math.floor(mins / 60);
  if (hours > 0) return `${hours}h ${mins % 60}min`;
  return `${mins}min`;
}
```

### Table of UNKNOWN transactions
Native `<table>`:
- `<th>`: ID, Valor, Tempo no status, Ações
- Row: ID (truncated `truncateUUID`, links to `/dashboard/transactions/{id}`), `formatCurrency`, `timeInStatus`, "Ver detalhe →" link
- Sort: `created_at` ascending (oldest first)
- Amber tint on rows: `bg-amber-50/60`
- Auto-refresh: hook already polls every 30s via `useTransactionList`

### Empty state
```tsx
<div className="flex flex-col items-center justify-center py-20 text-center">
  <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center mb-4">
    <CheckCircle className="w-6 h-6 text-emerald-500" />
  </div>
  <p className="text-sm font-medium text-slate-700">Nenhuma transação em reconciliação</p>
  <p className="text-xs text-slate-400 mt-1">Todas as transações têm status determinado.</p>
</div>
```

## Acceptance Criteria and Tests

- Success: Only `status === "UNKNOWN"` rows shown in table
- Success: Stats row shows correct counts/amounts
- Success: Empty state shown when no UNKNOWN transactions
- Success: Table sorted oldest first
- Success: "Ver detalhe →" links point to correct `/dashboard/transactions/{id}` URLs
- Failure: Any transaction with non-UNKNOWN status appearing in the table

## Constraints

- No new API calls — uses `useTransactionList()` and filters client-side
- Do NOT add new npm dependencies
- `timeInStatus` must update on each render; no separate timer needed (re-renders happen via polling)
