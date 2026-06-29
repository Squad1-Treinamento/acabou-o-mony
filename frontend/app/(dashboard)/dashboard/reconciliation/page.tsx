"use client";

import Link from "next/link";
import { RefreshCw, Info, CheckCircle } from "lucide-react";
import { useTransactionList } from "@/hooks/useTransactionList";
import { formatCurrency, formatRelativeTime, truncateUUID } from "@/lib/utils/formatters";

function timeInStatus(createdAt: string): string {
  const ms = Date.now() - new Date(createdAt).getTime();
  const mins = Math.floor(ms / 60_000);
  const hours = Math.floor(mins / 60);
  if (hours > 0) return `${hours}h ${mins % 60}min`;
  if (mins > 0) return `${mins}min`;
  return "< 1min";
}

export default function ReconciliationPage() {
  const { data: transactions = [], isLoading, refetch } = useTransactionList();

  const unknownTxs = [...transactions]
    .filter((t) => t.status === "UNKNOWN")
    .sort((a, b) => a.created_at.localeCompare(b.created_at)); // oldest first

  const oldest = unknownTxs[0];
  const totalExposed = unknownTxs.reduce((s, t) => s + t.amount, 0);

  return (
    <div className="max-w-6xl mx-auto w-full px-6 py-8 space-y-6">

      {/* Page header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-slate-900">Reconciliação</h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Transações aguardando confirmação do adquirente
          </p>
        </div>
        <button
          onClick={() => refetch()}
          className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-700 transition-colors"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Atualizar
        </button>
      </div>

      {/* Info banner */}
      <div className="flex gap-3 rounded-xl bg-amber-50 border border-amber-200/60 px-4 py-3.5">
        <Info className="w-4 h-4 text-amber-500 mt-0.5 shrink-0" />
        <p className="text-sm text-amber-700 leading-relaxed">
          A reconciliação automática consulta o adquirente a cada 5 minutos e resolve o status
          para <span className="font-medium">COMPLETED</span> ou{" "}
          <span className="font-medium">DECLINED</span>. Nenhuma ação manual é necessária.
        </p>
      </div>

      {/* Stats row */}
      {!isLoading && (
        <div className="grid grid-cols-3 gap-3">
          <div className="bg-white rounded-xl border border-slate-200 px-5 py-4">
            <p className="text-2xl font-bold tabular-nums text-slate-900">{unknownTxs.length}</p>
            <p className="text-xs text-slate-500 font-medium mt-1">Em reconciliação</p>
          </div>
          <div className="bg-white rounded-xl border border-slate-200 px-5 py-4">
            <p className="text-2xl font-bold tabular-nums text-slate-900">
              {oldest ? formatRelativeTime(oldest.created_at) : "—"}
            </p>
            <p className="text-xs text-slate-500 font-medium mt-1">Mais antiga</p>
          </div>
          <div className="bg-white rounded-xl border border-slate-200 px-5 py-4">
            <p className="text-2xl font-bold tabular-nums text-slate-900">
              {unknownTxs.length > 0 ? formatCurrency(totalExposed, "BRL") : "—"}
            </p>
            <p className="text-xs text-slate-500 font-medium mt-1">Valor exposto</p>
          </div>
        </div>
      )}

      {/* Table / empty state */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100">
          <p className="text-sm font-semibold text-slate-900">Transações pendentes</p>
          {!isLoading && (
            <p className="text-xs text-slate-400 mt-0.5">
              {unknownTxs.length} transação{unknownTxs.length !== 1 ? "ões" : ""} em UNKNOWN · ordenadas pela mais antiga
            </p>
          )}
        </div>

        {isLoading ? (
          <div className="px-6 py-4 space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-10 bg-slate-50 rounded-lg animate-pulse" />
            ))}
          </div>
        ) : unknownTxs.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 text-center">
            <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center mb-4">
              <CheckCircle className="w-6 h-6 text-emerald-500" />
            </div>
            <p className="text-sm font-medium text-slate-700">Nenhuma transação em reconciliação</p>
            <p className="text-xs text-slate-400 mt-1">Todas as transações têm status determinado.</p>
          </div>
        ) : (
          <table className="w-full">
            <thead>
              <tr className="border-b border-slate-100">
                <th className="text-left text-xs font-medium text-slate-400 uppercase tracking-wide px-6 py-3">
                  ID
                </th>
                <th className="text-right text-xs font-medium text-slate-400 uppercase tracking-wide px-6 py-3">
                  Valor
                </th>
                <th className="text-right text-xs font-medium text-slate-400 uppercase tracking-wide px-6 py-3">
                  Tempo no status
                </th>
                <th className="text-right text-xs font-medium text-slate-400 uppercase tracking-wide px-6 py-3">
                  Ações
                </th>
              </tr>
            </thead>
            <tbody>
              {unknownTxs.map((tx) => (
                <tr
                  key={tx.transaction_id}
                  className="border-b border-slate-50 last:border-0 bg-amber-50/50 hover:bg-amber-50 transition-colors"
                >
                  <td className="px-6 py-3.5">
                    <Link
                      href={`/dashboard/transactions/${tx.transaction_id}`}
                      className="font-mono text-xs text-slate-600 hover:text-[#0D2B1E] transition-colors"
                    >
                      {truncateUUID(tx.transaction_id)}
                    </Link>
                  </td>
                  <td className="px-6 py-3.5 text-right">
                    <span className="text-sm font-semibold text-slate-900 tabular-nums">
                      {formatCurrency(tx.amount, tx.currency)}
                    </span>
                  </td>
                  <td className="px-6 py-3.5 text-right">
                    <span className="text-xs text-amber-600 font-medium tabular-nums">
                      {timeInStatus(tx.created_at)}
                    </span>
                  </td>
                  <td className="px-6 py-3.5 text-right">
                    <Link
                      href={`/dashboard/transactions/${tx.transaction_id}`}
                      className="text-xs text-slate-500 hover:text-[#0D2B1E] transition-colors"
                    >
                      Ver detalhe →
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

    </div>
  );
}
