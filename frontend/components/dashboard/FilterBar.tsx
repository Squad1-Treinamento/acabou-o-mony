"use client";

import { useState, useCallback } from "react";
import { Search, X } from "lucide-react";
import { StatusBadge } from "./StatusBadge";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import type { PaymentStatus } from "@/types/payment";
import type { TransactionFilters } from "@/hooks/useTransactions";

const ALL_STATUSES: { status: PaymentStatus; description: string }[] = [
  { status: "COMPLETED",       description: "Pagamento aprovado pela adquirente." },
  { status: "VALIDATED",       description: "Dados validados pelo gateway, aguardando processamento." },
  { status: "CHALLENGE_PENDING", description: "Aguardando autenticação 3DS pelo portador do cartão." },
  { status: "DECLINED",        description: "Pagamento recusado pela adquirente ou banco emissor." },
  { status: "FAILED",          description: "Erro interno durante o processamento." },
  { status: "UNKNOWN",         description: "Resposta da adquirente não recebida — em reconciliação." },
];

const inputCls =
  "h-9 rounded-lg border border-slate-200 bg-white text-sm text-slate-900 " +
  "placeholder:text-slate-400 focus-visible:border-[#0D2B1E] focus-visible:ring-2 " +
  "focus-visible:ring-[#0D2B1E]/10 focus-visible:outline-none transition-all";

interface FilterBarProps {
  filters: TransactionFilters;
  onChange: (filters: TransactionFilters) => void;
}

export function FilterBar({ filters, onChange }: FilterBarProps) {
  const [idSearch, setIdSearch] = useState(filters.idPrefix ?? "");

  const toggleStatus = useCallback(
    (status: PaymentStatus) => {
      const current = filters.statuses ?? [];
      const next = current.includes(status)
        ? current.filter((s) => s !== status)
        : [...current, status];
      onChange({ ...filters, statuses: next.length ? next : undefined });
    },
    [filters, onChange]
  );

  function handleIdChange(e: React.ChangeEvent<HTMLInputElement>) {
    const value = e.target.value;
    setIdSearch(value);
    onChange({ ...filters, idPrefix: value || undefined });
  }

  function handleClear() {
    setIdSearch("");
    onChange({});
  }

  const hasFilters =
    (filters.statuses?.length ?? 0) > 0 ||
    !!filters.dateFrom ||
    !!filters.dateTo ||
    !!filters.idPrefix;

  return (
    <div className="space-y-3 mb-5">
      {/* Status pills */}
      <div className="flex flex-wrap gap-1.5">
        {ALL_STATUSES.map(({ status, description }) => {
          const isSelected = filters.statuses?.includes(status) ?? false;
          return (
            <Tooltip key={status}>
              <TooltipTrigger
                onClick={() => toggleStatus(status)}
                aria-pressed={isSelected}
                className={`rounded-full transition-all ${
                  isSelected
                    ? "ring-2 ring-[#0D2B1E] ring-offset-1 opacity-100"
                    : "opacity-50 hover:opacity-75"
                }`}
              >
                <StatusBadge status={status} />
              </TooltipTrigger>
              <TooltipContent>{description}</TooltipContent>
            </Tooltip>
          );
        })}
      </div>

      {/* Search + date range */}
      <div className="flex flex-wrap items-center gap-2">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400 pointer-events-none" />
          <input
            placeholder="Buscar por ID…"
            value={idSearch}
            onChange={handleIdChange}
            className={`${inputCls} pl-8 pr-3 w-52`}
          />
        </div>

        <input
          type="date"
          value={filters.dateFrom ?? ""}
          onChange={(e) => onChange({ ...filters, dateFrom: e.target.value || undefined })}
          aria-label="Data inicial"
          className={`${inputCls} px-3 w-40`}
        />

        <input
          type="date"
          value={filters.dateTo ?? ""}
          onChange={(e) => onChange({ ...filters, dateTo: e.target.value || undefined })}
          aria-label="Data final"
          className={`${inputCls} px-3 w-40`}
        />

        {hasFilters && (
          <button
            onClick={handleClear}
            className="flex items-center gap-1 text-xs text-slate-400 hover:text-slate-700 transition-colors h-9 px-2"
          >
            <X className="w-3.5 h-3.5" />
            Limpar
          </button>
        )}
      </div>
    </div>
  );
}
