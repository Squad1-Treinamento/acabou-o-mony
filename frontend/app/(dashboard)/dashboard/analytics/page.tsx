"use client";

import Link from "next/link";
import { RefreshCw } from "lucide-react";
import { useTransactionList } from "@/hooks/useTransactionList";
import { formatCurrency, formatRelativeTime, truncateUUID } from "@/lib/utils/formatters";
import type { PaymentResponse } from "@/types/payment";

// ── Data helpers ──────────────────────────────────────────────────────────────

function getDailyRevenue(txs: PaymentResponse[], days = 14) {
  const result: Record<string, number> = {};
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    result[d.toISOString().slice(0, 10)] = 0;
  }
  txs
    .filter((t) => t.status === "COMPLETED")
    .forEach((t) => {
      const day = t.created_at.slice(0, 10);
      if (day in result) result[day] += t.amount;
    });
  return Object.entries(result).map(([date, amount]) => ({ date, amount }));
}

// ── Chart components ──────────────────────────────────────────────────────────

function RevenueChart({ data }: { data: { date: string; amount: number }[] }) {
  const max = Math.max(...data.map((d) => d.amount), 1);
  return (
    <div className="flex items-end gap-1 h-28">
      {data.map(({ date, amount }) => (
        <div key={date} className="flex-1 flex flex-col items-center gap-1 group">
          <div
            title={`${date}: ${formatCurrency(amount, "BRL")}`}
            className="w-full rounded-sm transition-colors"
            style={{
              height: `${Math.max((amount / max) * 100, 0)}%`,
              minHeight: amount > 0 ? "3px" : "0",
              background: amount > 0 ? "#0D2B1E" : "transparent",
            }}
          />
          <span className="text-[9px] text-slate-300 tabular-nums">{date.slice(5)}</span>
        </div>
      ))}
    </div>
  );
}

function StatusBar({
  label,
  count,
  total,
  color,
}: {
  label: string;
  count: number;
  total: number;
  color: string;
}) {
  const pct = total > 0 ? (count / total) * 100 : 0;
  return (
    <div className="space-y-1.5">
      <div className="flex justify-between text-xs">
        <span className="text-slate-600">{label}</span>
        <span className="text-slate-400 tabular-nums">
          {count} · {pct.toFixed(0)}%
        </span>
      </div>
      <div className="h-1.5 bg-slate-100 rounded-full overflow-hidden">
        <div
          className="h-full rounded-full transition-all"
          style={{ width: `${pct}%`, backgroundColor: color }}
        />
      </div>
    </div>
  );
}

// ── KPI card ──────────────────────────────────────────────────────────────────

function KpiCard({
  label,
  value,
  bar,
  sub,
}: {
  label: string;
  value: string;
  bar: string;
  sub?: string;
}) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 px-5 py-4">
      <div className={`h-0.5 w-8 rounded-full mb-4 ${bar}`} />
      <p className="text-2xl font-bold tabular-nums text-slate-900">{value}</p>
      <p className="text-xs text-slate-500 font-medium mt-1">{label}</p>
      {sub && <p className="text-[11px] text-slate-400 mt-0.5">{sub}</p>}
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

export default function AnalyticsPage() {
  const { data: transactions = [], isLoading, refetch } = useTransactionList();

  const completed  = transactions.filter((t) => t.status === "COMPLETED");
  const pending    = transactions.filter((t) =>
    ["CREATED", "VALIDATED", "PROCESSING", "CHALLENGE_PENDING", "AUTHENTICATED"].includes(t.status)
  );
  const failed     = transactions.filter((t) => ["DECLINED", "FAILED"].includes(t.status));
  const unknown    = transactions.filter((t) => t.status === "UNKNOWN");

  const revenue    = completed.reduce((s, t) => s + t.amount, 0);
  const avgTicket  = completed.length > 0 ? revenue / completed.length : null;
  const successRate = transactions.length > 0
    ? (completed.length / transactions.length) * 100
    : null;

  const dailyData  = getDailyRevenue(transactions);

  const topTxs = [...completed]
    .sort((a, b) => b.amount - a.amount)
    .slice(0, 5);

  return (
    <div className="max-w-6xl mx-auto w-full px-6 py-8 space-y-6">

      {/* Page header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-slate-900">Relatórios</h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Receita e volume calculados em tempo real
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

      {/* KPI cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <KpiCard
          label="Receita total"
          value={isLoading ? "—" : formatCurrency(revenue, "BRL")}
          bar="bg-emerald-500"
        />
        <KpiCard
          label="Transações"
          value={isLoading ? "—" : String(transactions.length)}
          bar="bg-[#0D2B1E]"
          sub={`${completed.length} concluídas`}
        />
        <KpiCard
          label="Ticket médio"
          value={isLoading ? "—" : avgTicket !== null ? formatCurrency(avgTicket, "BRL") : "—"}
          bar="bg-sky-500"
        />
        <KpiCard
          label="Taxa de aprovação"
          value={isLoading ? "—" : successRate !== null ? `${successRate.toFixed(0)}%` : "—"}
          bar="bg-violet-400"
        />
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">

        {/* Revenue bar chart */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 px-6 py-5">
          <div className="flex items-center justify-between mb-5">
            <div>
              <p className="text-sm font-semibold text-slate-900">Receita por dia</p>
              <p className="text-xs text-slate-400 mt-0.5">Últimos 14 dias — apenas pagamentos concluídos</p>
            </div>
          </div>
          {isLoading ? (
            <div className="h-28 bg-slate-50 rounded-lg animate-pulse" />
          ) : (
            <RevenueChart data={dailyData} />
          )}
        </div>

        {/* Status distribution */}
        <div className="bg-white rounded-xl border border-slate-200 px-6 py-5">
          <p className="text-sm font-semibold text-slate-900 mb-5">Distribuição de status</p>
          {isLoading ? (
            <div className="space-y-4">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="h-5 bg-slate-50 rounded animate-pulse" />
              ))}
            </div>
          ) : (
            <div className="space-y-4">
              <StatusBar label="Concluídas"       count={completed.length} total={transactions.length} color="#22c55e" />
              <StatusBar label="Em andamento"     count={pending.length}   total={transactions.length} color="#0D2B1E" />
              <StatusBar label="Recusadas/Falhas" count={failed.length}    total={transactions.length} color="#ef4444" />
              <StatusBar label="Reconciliação"    count={unknown.length}   total={transactions.length} color="#f59e0b" />
            </div>
          )}
        </div>
      </div>

      {/* Top transactions */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100">
          <p className="text-sm font-semibold text-slate-900">Maiores transações</p>
          <p className="text-xs text-slate-400 mt-0.5">Top 5 por valor — apenas concluídas</p>
        </div>

        {topTxs.length === 0 ? (
          <div className="px-6 py-12 text-center">
            <p className="text-sm text-slate-400">Nenhuma transação concluída ainda.</p>
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
                  Data
                </th>
              </tr>
            </thead>
            <tbody>
              {topTxs.map((tx, i) => (
                <tr
                  key={tx.transaction_id}
                  className={`border-b border-slate-50 last:border-0 hover:bg-slate-50 transition-colors ${
                    i === 0 ? "bg-emerald-50/40" : ""
                  }`}
                >
                  <td className="px-6 py-3">
                    <Link
                      href={`/dashboard/transactions/${tx.transaction_id}`}
                      className="font-mono text-xs text-slate-600 hover:text-[#0D2B1E] transition-colors"
                    >
                      {truncateUUID(tx.transaction_id)}
                    </Link>
                  </td>
                  <td className="px-6 py-3 text-right">
                    <span className="text-sm font-semibold text-slate-900 tabular-nums">
                      {formatCurrency(tx.amount, tx.currency)}
                    </span>
                  </td>
                  <td className="px-6 py-3 text-right">
                    <span className="text-xs text-slate-400">
                      {formatRelativeTime(tx.created_at)}
                    </span>
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
