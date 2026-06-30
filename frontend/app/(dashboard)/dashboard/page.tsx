"use client";

import { useState, useCallback } from "react";
import { RefreshCw } from "lucide-react";
import { TransactionTable } from "@/components/dashboard/TransactionTable";
import { FilterBar } from "@/components/dashboard/FilterBar";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { useTransactionList } from "@/hooks/useTransactionList";
import { filterTransactions, resolveDisplayStatus } from "@/hooks/useTransactions";
import type { TransactionFilters } from "@/hooks/useTransactions";
import type { PaymentStatus } from "@/types/payment";

const STATUS_GROUP: Record<string, PaymentStatus[]> = {
  completed: ["COMPLETED"],
  pending: ["CREATED", "VALIDATED", "PROCESSING", "CHALLENGE_PENDING", "AUTHENTICATED"],
  failed: ["DECLINED", "FAILED"],
  unknown: ["UNKNOWN"],
};

// ── KPI tooltip content ──────────────────────────────────────────────────────

function KpiCompleted() {
  return (
    <div className="space-y-2 py-0.5 w-60">
      <div className="flex items-center gap-2">
        <span className="w-2 h-2 rounded-full bg-emerald-400 shrink-0" />
        <p className="text-xs font-semibold">Pagamento concluído</p>
      </div>
      <p className="text-xs opacity-70 leading-relaxed">
        O adquirente confirmou a captura dos fundos. O valor foi liquidado com sucesso na conta do lojista.
      </p>
      <p className="text-[11px] opacity-50 font-mono">status = COMPLETED</p>
    </div>
  );
}

function KpiPending() {
  return (
    <div className="space-y-2 py-0.5 w-64">
      <div className="flex items-center gap-2">
        <span className="w-2 h-2 rounded-full bg-[#4a9d72] shrink-0" />
        <p className="text-xs font-semibold">Em processamento</p>
      </div>
      <p className="text-xs opacity-70 leading-relaxed">
        Pagamentos ativos no fluxo — aguardando validação, captura pelo adquirente ou autenticação 3DS pelo portador.
      </p>
      <div className="text-[11px] opacity-50 font-mono space-y-0.5">
        <p>CREATED → VALIDATED → PROCESSING</p>
        <p>CHALLENGE_PENDING → AUTHENTICATED</p>
      </div>
    </div>
  );
}

function KpiFailed() {
  return (
    <div className="space-y-2 py-0.5 w-60">
      <div className="flex items-center gap-2">
        <span className="w-2 h-2 rounded-full bg-red-400 shrink-0" />
        <p className="text-xs font-semibold">Não aprovado</p>
      </div>
      <p className="text-xs opacity-70 leading-relaxed">
        Pagamento encerrado sem captura de fundos. Dois motivos possíveis:
      </p>
      <ul className="text-[11px] opacity-60 space-y-0.5 list-none">
        <li><span className="font-mono">DECLINED</span> — negado pelo banco emissor do cartão</li>
        <li><span className="font-mono">FAILED</span> — erro técnico no processamento</li>
      </ul>
    </div>
  );
}

function KpiApprovalRate() {
  return (
    <div className="space-y-2 py-0.5 w-64">
      <div className="flex items-center gap-2">
        <span className="w-2 h-2 rounded-full bg-blue-400 shrink-0" />
        <p className="text-xs font-semibold">Taxa de aprovação</p>
      </div>
      <p className="text-xs opacity-70 leading-relaxed">
        Percentual de tentativas com resultado final que foram aprovadas pelo adquirente. Principal indicador de saúde do checkout.
      </p>
      <p className="text-[11px] opacity-60">
        Referência do setor: acima de 85% é considerado saudável. Abaixo de 70% requer atenção imediata.
      </p>
      <p className="text-[11px] opacity-50 font-mono">COMPLETED ÷ tentativas finalizadas</p>
    </div>
  );
}

// ── StatCard ─────────────────────────────────────────────────────────────────

interface StatCardProps {
  label: string;
  value: string | number;
  bar: string;
  active?: boolean;
  onClick?: () => void;
  tooltipContent: React.ReactNode;
}

function StatCard({ label, value, bar, active, onClick, tooltipContent }: StatCardProps) {
  return (
    <Tooltip>
      <TooltipTrigger
        onClick={onClick}
        className={`w-full text-left rounded-xl bg-white border px-5 py-4 transition-all ${
          onClick ? "cursor-pointer" : "cursor-default"
        } ${
          active
            ? "border-slate-300 shadow-sm"
            : "border-slate-200 hover:border-slate-300 hover:shadow-sm"
        }`}
      >
        <div className={`h-0.5 w-8 rounded-full mb-4 ${bar}`} />
        <p className="text-2xl font-bold tabular-nums text-slate-900">{value}</p>
        <p className="text-xs text-slate-500 font-medium mt-1">{label}</p>
      </TooltipTrigger>
      <TooltipContent side="bottom" sideOffset={8}>
        {tooltipContent}
      </TooltipContent>
    </Tooltip>
  );
}

// ── Page ─────────────────────────────────────────────────────────────────────

export default function DashboardPage() {
  const [filters, setFilters] = useState<TransactionFilters>({});
  const [activeGroup, setActiveGroup] = useState<string | null>(null);
  const [spinning, setSpinning] = useState(false);

  const { data: transactions = [], isLoading, refetch } = useTransactionList();

  const handleRefetch = useCallback(() => {
    setSpinning(true);
    refetch().finally(() => setTimeout(() => setSpinning(false), 600));
  }, [refetch]);

  const stats = {
    completed: transactions.filter((t) => STATUS_GROUP.completed.includes(resolveDisplayStatus(t))).length,
    pending:   transactions.filter((t) => STATUS_GROUP.pending.includes(resolveDisplayStatus(t))).length,
    failed:    transactions.filter((t) => STATUS_GROUP.failed.includes(resolveDisplayStatus(t))).length,
  };

  // Approval rate: completed ÷ total attempts (pending excluded — outcome unknown yet)
  const attempted = transactions.filter((t) => !STATUS_GROUP.pending.includes(resolveDisplayStatus(t))).length;
  const approvalRate = attempted > 0 ? Math.round((stats.completed / attempted) * 100) : null;
  const approvalRateDisplay = approvalRate !== null ? `${approvalRate}%` : "—";
  const approvalRateBar =
    approvalRate === null ? "bg-slate-200"
    : approvalRate >= 85   ? "bg-emerald-500"
    : approvalRate >= 70   ? "bg-amber-400"
    : "bg-red-400";

  function handleStatClick(group: string) {
    if (activeGroup === group) {
      setActiveGroup(null);
      setFilters({});
    } else {
      setActiveGroup(group);
      setFilters({ statuses: STATUS_GROUP[group] });
    }
  }

  const filtered = filterTransactions(transactions, filters);

  return (
    <div className="max-w-6xl mx-auto w-full px-6 py-8 space-y-6">

      {/* KPI cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <StatCard
          label="Concluídas"
          value={stats.completed}
          bar="bg-emerald-500"
          active={activeGroup === "completed"}
          onClick={() => handleStatClick("completed")}
          tooltipContent={<KpiCompleted />}
        />
        <StatCard
          label="Em andamento"
          value={stats.pending}
          bar="bg-[#0D2B1E]"
          active={activeGroup === "pending"}
          onClick={() => handleStatClick("pending")}
          tooltipContent={<KpiPending />}
        />
        <StatCard
          label="Recusadas / Falhas"
          value={stats.failed}
          bar="bg-red-500"
          active={activeGroup === "failed"}
          onClick={() => handleStatClick("failed")}
          tooltipContent={<KpiFailed />}
        />
        <StatCard
          label="Taxa de aprovação"
          value={approvalRateDisplay}
          bar={approvalRateBar}
          tooltipContent={<KpiApprovalRate />}
        />
      </div>

      {/* Transactions section */}
      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-semibold text-slate-900">Transações</h2>
            {!isLoading && (
              <p className="text-xs text-slate-400 mt-0.5">
                {filtered.length === transactions.length
                  ? `${transactions.length} registros`
                  : `${filtered.length} de ${transactions.length} registros`}
              </p>
            )}
          </div>
          <button
            onClick={handleRefetch}
            className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-700 transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 transition-transform ${spinning ? "animate-spin" : ""}`} />
            Atualizar
          </button>
        </div>

        <div className="px-6 py-4">
          <FilterBar
            filters={filters}
            onChange={(f) => { setFilters(f); setActiveGroup(null); }}
          />
          <TransactionTable transactions={filtered} isLoading={isLoading} />
        </div>
      </div>

    </div>
  );
}
