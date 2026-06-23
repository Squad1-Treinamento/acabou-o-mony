"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { AppHeader } from "@/components/layout/AppHeader";
import { TransactionTable } from "@/components/dashboard/TransactionTable";
import { FilterBar } from "@/components/dashboard/FilterBar";
import { useTransactionList } from "@/hooks/useTransactionList";
import { filterTransactions } from "@/hooks/useTransactions";
import type { TransactionFilters } from "@/hooks/useTransactions";
import type { PaymentResponse, PaymentStatus } from "@/types/payment";

const STATUS_GROUP: Record<string, PaymentStatus[]> = {
  completed: ["COMPLETED"],
  pending: ["CREATED", "VALIDATED", "PROCESSING", "CHALLENGE_PENDING", "AUTHENTICATED"],
  failed: ["DECLINED", "FAILED"],
  unknown: ["UNKNOWN"],
};

interface StatCardProps {
  label: string;
  value: number;
  color: string;
  bg: string;
  tooltip: string;
  onClick?: () => void;
  active?: boolean;
}

function StatCard({ label, value, color, bg, tooltip, onClick, active }: StatCardProps) {
  return (
    <div className="relative group">
      <button
        onClick={onClick}
        className={`w-full text-left rounded-card p-4 border transition-all ${
          active
            ? `${bg} border-current ring-2 ring-offset-1`
            : "bg-white border-border hover:border-current/30"
        } ${color}`}
      >
        <p className="text-2xl font-bold tabular-nums">{value}</p>
        <p className="text-xs font-medium mt-0.5 opacity-70">{label}</p>
      </button>
      <div className="pointer-events-none absolute bottom-full left-1/2 -translate-x-1/2 mb-2 z-50
                      w-52 rounded-lg bg-gray-900 px-3 py-2 text-xs text-white shadow-lg
                      opacity-0 group-hover:opacity-100 transition-opacity duration-150">
        {tooltip}
        <div className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-gray-900" />
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const router = useRouter();
  const [filters, setFilters] = useState<TransactionFilters>({});
  const [activeGroup, setActiveGroup] = useState<string | null>(null);

  useEffect(() => {
    const key = sessionStorage.getItem("mony_api_key");
    if (!key) router.replace("/login");
  }, [router]);

  const { data: transactions = [], isLoading, refetch } = useTransactionList();

  const stats = {
    total: transactions.length,
    completed: transactions.filter((t) => STATUS_GROUP.completed.includes(t.status)).length,
    pending: transactions.filter((t) => STATUS_GROUP.pending.includes(t.status)).length,
    failed: transactions.filter((t) => STATUS_GROUP.failed.includes(t.status)).length,
    unknown: transactions.filter((t) => STATUS_GROUP.unknown.includes(t.status)).length,
  };

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
    <div className="min-h-screen bg-background flex flex-col">
      <AppHeader showLogout />

      {/* Stats strip */}
      <div className="bg-white border-b border-border">
        <div className="max-w-6xl mx-auto px-4 py-5">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <StatCard
              label="Concluídas"
              value={stats.completed}
              color="text-[#1F8F53]"
              bg="bg-[#EAF7F0]"
              tooltip="Pagamentos aprovados e liquidados com sucesso pelo adquirente."
              active={activeGroup === "completed"}
              onClick={() => handleStatClick("completed")}
            />
            <StatCard
              label="Em andamento"
              value={stats.pending}
              color="text-primary"
              bg="bg-primary/8"
              tooltip="Pagamentos em processamento, aguardando validação ou autenticação 3DS."
              active={activeGroup === "pending"}
              onClick={() => handleStatClick("pending")}
            />
            <StatCard
              label="Recusadas / Falhas"
              value={stats.failed}
              color="text-[#DC2626]"
              bg="bg-[#FEF2F2]"
              tooltip="Pagamentos negados pela operadora do cartão ou com erro no processamento."
              active={activeGroup === "failed"}
              onClick={() => handleStatClick("failed")}
            />
            <StatCard
              label="Em reconciliação"
              value={stats.unknown}
              color="text-[#D97706]"
              bg="bg-[#FFFBEB]"
              tooltip="Status incerto: o adquirente não confirmou o resultado. A reconciliação automática resolverá em breve."
              active={activeGroup === "unknown"}
              onClick={() => handleStatClick("unknown")}
            />
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="max-w-6xl mx-auto px-4 py-8 w-full flex-1">
        <div className="flex items-center justify-between mb-5">
          <div>
            <h2 className="text-lg font-semibold text-text-primary">Transações</h2>
            {!isLoading && (
              <p className="text-sm text-text-secondary mt-0.5">
                {filtered.length === transactions.length
                  ? `${transactions.length} transações`
                  : `${filtered.length} de ${transactions.length} transações`}
              </p>
            )}
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetch()}
            className="gap-2 text-text-secondary"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Atualizar
          </Button>
        </div>

        <FilterBar filters={filters} onChange={(f) => { setFilters(f); setActiveGroup(null); }} />
        <TransactionTable transactions={filtered} isLoading={isLoading} />
      </div>
    </div>
  );
}
