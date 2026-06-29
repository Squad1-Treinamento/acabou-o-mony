"use client";

import { useState } from "react";
import Link from "next/link";
import { RefreshCw } from "lucide-react";
import { useTransactionList } from "@/hooks/useTransactionList";
import { formatCurrency, formatRelativeTime, truncateUUID } from "@/lib/utils/formatters";
import type { PaymentResponse } from "@/types/payment";

type WebhookEventType = "payment.completed" | "payment.declined" | "payment.failed";

interface WebhookEvent {
  id: string;
  event: WebhookEventType;
  transaction_id: string;
  amount: number;
  currency: string;
  timestamp: string;
}

const EVENT_MAP: Partial<Record<string, WebhookEventType>> = {
  COMPLETED: "payment.completed",
  DECLINED:  "payment.declined",
  FAILED:    "payment.failed",
};

function deriveEvents(transactions: PaymentResponse[]): WebhookEvent[] {
  return transactions
    .filter((t) => t.status in EVENT_MAP)
    .map((t) => ({
      id: t.transaction_id,
      event: EVENT_MAP[t.status]!,
      transaction_id: t.transaction_id,
      amount: t.amount,
      currency: t.currency,
      timestamp: t.updated_at,
    }))
    .sort((a, b) => b.timestamp.localeCompare(a.timestamp));
}

const EVENT_STYLES: Record<WebhookEventType, { bg: string; text: string; dot: string; label: string }> = {
  "payment.completed": { bg: "#EAF7F0", text: "#1F8F53", dot: "bg-emerald-400", label: "payment.completed" },
  "payment.declined":  { bg: "rgba(220,38,38,0.08)", text: "#DC2626", dot: "bg-red-400",     label: "payment.declined"  },
  "payment.failed":    { bg: "rgba(220,38,38,0.08)", text: "#DC2626", dot: "bg-red-400",     label: "payment.failed"    },
};

const ALL_TYPES: WebhookEventType[] = ["payment.completed", "payment.declined", "payment.failed"];

export default function WebhooksPage() {
  const { data: transactions = [], isLoading, refetch } = useTransactionList();
  const [selected, setSelected] = useState<Set<WebhookEventType>>(new Set());

  const allEvents = deriveEvents(transactions);
  const filtered  = selected.size === 0
    ? allEvents
    : allEvents.filter((e) => selected.has(e.event));

  function toggleType(type: WebhookEventType) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(type)) next.delete(type);
      else next.add(type);
      return next;
    });
  }

  return (
    <div className="max-w-6xl mx-auto w-full px-6 py-8 space-y-6">

      {/* Page header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-lg font-semibold text-slate-900">Integrações</h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Eventos de webhook gerados por transações com status terminal
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

      {/* Filter chips */}
      <div className="flex flex-wrap gap-2">
        {ALL_TYPES.map((type) => {
          const style = EVENT_STYLES[type];
          const isOn  = selected.has(type);
          return (
            <button
              key={type}
              onClick={() => toggleType(type)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-mono font-medium border transition-all ${
                isOn
                  ? "border-transparent ring-2 ring-offset-1"
                  : "border-slate-200 opacity-60 hover:opacity-90"
              }`}
              style={
                isOn
                  ? { background: style.bg, color: style.text }
                  : {}
              }
            >
              <span
                className={`w-1.5 h-1.5 rounded-full ${style.dot}`}
              />
              {type}
            </button>
          );
        })}
        {selected.size > 0 && (
          <button
            onClick={() => setSelected(new Set())}
            className="px-3 py-1.5 rounded-full text-xs text-slate-400 hover:text-slate-700 border border-slate-200 transition-colors"
          >
            Limpar
          </button>
        )}
      </div>

      {/* Event list */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
          <div>
            <p className="text-sm font-semibold text-slate-900">Log de eventos</p>
            {!isLoading && (
              <p className="text-xs text-slate-400 mt-0.5">
                {filtered.length} evento{filtered.length !== 1 ? "s" : ""} · ordenados pelo mais recente
              </p>
            )}
          </div>
        </div>

        {isLoading ? (
          <div className="px-6 py-4 space-y-3">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-10 bg-slate-50 rounded-lg animate-pulse" />
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <div className="px-6 py-16 text-center">
            <p className="text-sm text-slate-400">
              {allEvents.length === 0
                ? "Nenhum evento de webhook encontrado."
                : "Nenhum evento corresponde ao filtro selecionado."}
            </p>
            {allEvents.length === 0 && (
              <p className="text-xs text-slate-300 mt-1">
                Eventos são gerados quando transações atingem status terminal (COMPLETED, DECLINED, FAILED).
              </p>
            )}
          </div>
        ) : (
          <table className="w-full">
            <thead>
              <tr className="border-b border-slate-100">
                <th className="text-left text-xs font-medium text-slate-400 uppercase tracking-wide px-6 py-3">
                  Evento
                </th>
                <th className="text-left text-xs font-medium text-slate-400 uppercase tracking-wide px-6 py-3">
                  Transação
                </th>
                <th className="text-right text-xs font-medium text-slate-400 uppercase tracking-wide px-6 py-3">
                  Valor
                </th>
                <th className="text-right text-xs font-medium text-slate-400 uppercase tracking-wide px-6 py-3">
                  Data
                </th>
                <th className="text-right text-xs font-medium text-slate-400 uppercase tracking-wide px-6 py-3">
                  Status
                </th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((ev) => {
                const style = EVENT_STYLES[ev.event];
                return (
                  <tr
                    key={ev.id}
                    className="border-b border-slate-50 last:border-0 hover:bg-slate-50 transition-colors"
                  >
                    <td className="px-6 py-3.5">
                      <span
                        className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-mono font-medium"
                        style={{ background: style.bg, color: style.text }}
                      >
                        <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${style.dot}`} />
                        {style.label}
                      </span>
                    </td>
                    <td className="px-6 py-3.5">
                      <Link
                        href={`/dashboard/transactions/${ev.transaction_id}`}
                        className="font-mono text-xs text-slate-600 hover:text-[#0D2B1E] transition-colors"
                      >
                        {truncateUUID(ev.transaction_id)}
                      </Link>
                    </td>
                    <td className="px-6 py-3.5 text-right">
                      <span className="text-sm font-semibold text-slate-900 tabular-nums">
                        {formatCurrency(ev.amount, ev.currency)}
                      </span>
                    </td>
                    <td className="px-6 py-3.5 text-right">
                      <span className="text-xs text-slate-400">
                        {formatRelativeTime(ev.timestamp)}
                      </span>
                    </td>
                    <td className="px-6 py-3.5 text-right">
                      <span className="inline-flex items-center gap-1 text-xs text-emerald-600">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 shrink-0" />
                        Entregue
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

    </div>
  );
}
