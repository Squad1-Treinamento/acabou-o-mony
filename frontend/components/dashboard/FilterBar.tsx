"use client";

import { useState, useCallback } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { StatusBadge } from "./StatusBadge";
import type { PaymentStatus } from "@/types/payment";
import type { TransactionFilters } from "@/hooks/useTransactions";

const ALL_STATUSES: PaymentStatus[] = [
  "CREATED",
  "VALIDATED",
  "PROCESSING",
  "COMPLETED",
  "DECLINED",
  "FAILED",
  "UNKNOWN",
  "CHALLENGE_PENDING",
  "AUTHENTICATED",
];

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
      onChange({ ...filters, statuses: next });
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
    filters.dateFrom ||
    filters.dateTo ||
    filters.idPrefix;

  return (
    <div className="space-y-4 mb-6">
      <div className="flex flex-wrap gap-2">
        {ALL_STATUSES.map((status) => {
          const isSelected = filters.statuses?.includes(status) ?? false;
          return (
            <button
              key={status}
              onClick={() => toggleStatus(status)}
              className={`rounded-full transition-opacity ${isSelected ? "opacity-100 ring-2 ring-primary ring-offset-1" : "opacity-60 hover:opacity-80"}`}
              aria-pressed={isSelected}
            >
              <StatusBadge status={status} />
            </button>
          );
        })}
      </div>

      <div className="flex gap-3 flex-wrap items-center">
        <Input
          placeholder="Buscar por ID..."
          value={idSearch}
          onChange={handleIdChange}
          className="max-w-xs"
        />
        <Input
          type="date"
          value={filters.dateFrom ?? ""}
          onChange={(e) => onChange({ ...filters, dateFrom: e.target.value || undefined })}
          className="max-w-[160px]"
          aria-label="Data inicial"
        />
        <Input
          type="date"
          value={filters.dateTo ?? ""}
          onChange={(e) => onChange({ ...filters, dateTo: e.target.value || undefined })}
          className="max-w-[160px]"
          aria-label="Data final"
        />
        {hasFilters && (
          <Button variant="ghost" size="sm" onClick={handleClear} className="text-text-secondary">
            Limpar filtros
          </Button>
        )}
      </div>
    </div>
  );
}
