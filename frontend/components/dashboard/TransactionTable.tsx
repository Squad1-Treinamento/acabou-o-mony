"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ChevronUp, ChevronDown, ChevronsUpDown } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { StatusBadge } from "./StatusBadge";
import { TransactionCard } from "./TransactionCard";
import { EmptyState } from "@/components/shared/EmptyState";
import { formatCurrency, formatRelativeTime } from "@/lib/utils/formatters";
import { sortTransactions, paginateTransactions, resolveDisplayStatus } from "@/hooks/useTransactions";
import type { PaymentResponse } from "@/types/payment";
import type { SortField, SortDirection } from "@/hooks/useTransactions";

const PAGE_SIZE = 20;

interface TransactionTableProps {
  transactions: PaymentResponse[];
  isLoading: boolean;
}

function SortIcon({ field, sortField, sortDir }: {
  field: SortField;
  sortField: SortField;
  sortDir: SortDirection;
}) {
  if (field !== sortField) return <ChevronsUpDown className="w-3 h-3 text-slate-300" />;
  return sortDir === "asc"
    ? <ChevronUp className="w-3 h-3 text-[#0D2B1E]" />
    : <ChevronDown className="w-3 h-3 text-[#0D2B1E]" />;
}

export function TransactionTable({ transactions, isLoading }: TransactionTableProps) {
  const router = useRouter();
  const [sortField, setSortField] = useState<SortField>("created_at");
  const [sortDir, setSortDir] = useState<SortDirection>("desc");
  const [page, setPage] = useState(1);

  function toggleSort(field: SortField) {
    if (field === sortField) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortField(field);
      setSortDir("desc");
    }
    setPage(1);
  }

  const sorted = sortTransactions(transactions, { field: sortField, direction: sortDir });
  const { items, totalPages, totalItems } = paginateTransactions(sorted, page, PAGE_SIZE);

  if (isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-11 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (transactions.length === 0) {
    return (
      <EmptyState
        title="Nenhuma transação encontrada"
        subtitle="Ajuste os filtros para ver outros resultados."
      />
    );
  }

  const thCls = "px-4 py-3 text-left text-[11px] font-semibold text-slate-400 uppercase tracking-wider";
  const tdCls = "px-4 py-3 text-sm text-slate-600";

  return (
    <div className="space-y-3">
      {/* Mobile */}
      <div className="flex flex-col gap-2 md:hidden">
        {items.map((tx) => (
          <TransactionCard key={tx.transaction_id} transaction={tx} />
        ))}
      </div>

      {/* Desktop */}
      <div className="hidden md:block overflow-hidden rounded-xl border border-slate-200">
        <table className="w-full border-collapse">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200">
              <th className={thCls}>ID</th>
              <th className={thCls}>
                <button
                  onClick={() => toggleSort("status")}
                  className="flex items-center gap-1.5 hover:text-slate-700 transition-colors"
                >
                  Status <SortIcon field="status" sortField={sortField} sortDir={sortDir} />
                </button>
              </th>
              <th className={thCls}>
                <button
                  onClick={() => toggleSort("amount")}
                  className="flex items-center gap-1.5 hover:text-slate-700 transition-colors"
                >
                  Valor <SortIcon field="amount" sortField={sortField} sortDir={sortDir} />
                </button>
              </th>
              <th className={thCls}>Chave</th>
              <th className={thCls}>
                <button
                  onClick={() => toggleSort("created_at")}
                  className="flex items-center gap-1.5 hover:text-slate-700 transition-colors"
                >
                  Quando <SortIcon field="created_at" sortField={sortField} sortDir={sortDir} />
                </button>
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {items.map((tx) => (
              <tr
                key={tx.transaction_id}
                onClick={() => router.push(`/dashboard/transactions/${tx.transaction_id}`)}
                className={`cursor-pointer transition-colors hover:bg-slate-50 ${
                  tx.status === "UNKNOWN" && resolveDisplayStatus(tx) === "UNKNOWN"
                    ? "bg-amber-50/50"
                    : "bg-white"
                }`}
              >
                <td className={tdCls}>
                  <Tooltip>
                    <TooltipTrigger>
                      <Link
                        href={`/dashboard/transactions/${tx.transaction_id}`}
                        onClick={(e) => e.stopPropagation()}
                        className="font-mono text-xs text-slate-500 hover:text-[#0D2B1E] transition-colors"
                      >
                        {tx.transaction_id.slice(0, 8)}…
                      </Link>
                    </TooltipTrigger>
                    <TooltipContent>{tx.transaction_id}</TooltipContent>
                  </Tooltip>
                  {tx.status === "UNKNOWN" && resolveDisplayStatus(tx) === "UNKNOWN" && (
                    <span className="ml-2 text-[10px] font-medium text-amber-600 bg-amber-50 px-1.5 py-0.5 rounded">
                      reconciliação
                    </span>
                  )}
                </td>
                <td className={tdCls}>
                  <StatusBadge status={resolveDisplayStatus(tx)} />
                </td>
                <td className={`${tdCls} font-semibold text-slate-900 tabular-nums`}>
                  {formatCurrency(tx.amount, tx.currency)}
                </td>
                <td className={`${tdCls} font-mono text-xs text-slate-400`}>
                  {tx.idempotency_key ? tx.idempotency_key.slice(0, 8) + "…" : <span className="text-slate-300">—</span>}
                </td>
                <td className={tdCls}>
                  <Tooltip>
                    <TooltipTrigger className="text-slate-500 text-xs">
                      {formatRelativeTime(tx.created_at)}
                    </TooltipTrigger>
                    <TooltipContent>
                      {new Date(tx.created_at).toLocaleString("pt-BR")}
                    </TooltipContent>
                  </Tooltip>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between pt-1">
          <span className="text-xs text-slate-400">
            {totalItems} registros · página {page} de {totalPages}
          </span>
          <div className="flex gap-1.5">
            <button
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page === 1}
              className="h-8 px-3 text-xs rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              Anterior
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={page === totalPages}
              className="h-8 px-3 text-xs rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
            >
              Próxima
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
