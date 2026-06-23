"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowUpDown } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { StatusBadge } from "./StatusBadge";
import { TransactionCard } from "./TransactionCard";
import { EmptyState } from "@/components/shared/EmptyState";
import { formatCurrency, formatRelativeTime } from "@/lib/utils/formatters";
import {
  sortTransactions,
  paginateTransactions,
} from "@/hooks/useTransactions";
import type { PaymentResponse } from "@/types/payment";
import type { SortField, SortDirection } from "@/hooks/useTransactions";

const PAGE_SIZE = 20;

interface TransactionTableProps {
  transactions: PaymentResponse[];
  isLoading: boolean;
}

export function TransactionTable({ transactions, isLoading }: TransactionTableProps) {
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
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-12 w-full rounded-card" />
        ))}
      </div>
    );
  }

  if (transactions.length === 0) {
    return (
      <EmptyState
        title="Nenhuma transação encontrada"
        subtitle="Ajuste os filtros ou insira IDs de transação acima."
      />
    );
  }

  return (
    <div className="space-y-4">
      {/* Mobile: card list */}
      <div className="flex flex-col gap-3 md:hidden">
        {items.map((tx) => (
          <TransactionCard key={tx.transaction_id} transaction={tx} />
        ))}
      </div>

      {/* Desktop: table */}
      <div className="hidden md:block rounded-card border border-border overflow-hidden">
        <Table>
          <TableHeader>
            <TableRow className="bg-[#F0F2F4] hover:bg-[#F0F2F4]">
              <TableHead className="w-32">ID</TableHead>
              <TableHead>
                <button
                  onClick={() => toggleSort("status")}
                  className="flex items-center gap-1 text-text-secondary hover:text-text-primary"
                >
                  Status <ArrowUpDown className="w-3 h-3" />
                </button>
              </TableHead>
              <TableHead>
                <button
                  onClick={() => toggleSort("amount")}
                  className="flex items-center gap-1 text-text-secondary hover:text-text-primary"
                >
                  Valor <ArrowUpDown className="w-3 h-3" />
                </button>
              </TableHead>
              <TableHead>Moeda</TableHead>
              <TableHead>
                <button
                  onClick={() => toggleSort("created_at")}
                  className="flex items-center gap-1 text-text-secondary hover:text-text-primary"
                >
                  Data <ArrowUpDown className="w-3 h-3" />
                </button>
              </TableHead>
              <TableHead>Cartão</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {items.map((tx) => (
              <TableRow
                key={tx.transaction_id}
                className={`cursor-pointer hover:bg-surface ${
                  tx.status === "UNKNOWN" ? "bg-[#FFFBEB]" : ""
                }`}
                onClick={() => {
                  window.location.href = `/dashboard/transactions/${tx.transaction_id}`;
                }}
              >
                <TableCell>
                  <Tooltip>
                    <TooltipTrigger className="cursor-default">
                      <Link
                        href={`/dashboard/transactions/${tx.transaction_id}`}
                        className="font-mono text-xs text-text-secondary hover:text-primary"
                        onClick={(e) => e.stopPropagation()}
                      >
                        {tx.transaction_id.slice(0, 8)}…
                      </Link>
                    </TooltipTrigger>
                    <TooltipContent>{tx.transaction_id}</TooltipContent>
                  </Tooltip>
                  {tx.status === "UNKNOWN" && (
                    <span className="ml-2 text-[11px] text-[#D97706] font-medium">
                      Em reconciliação
                    </span>
                  )}
                </TableCell>
                <TableCell>
                  <StatusBadge status={tx.status} />
                </TableCell>
                <TableCell className="font-semibold text-text-primary">
                  {formatCurrency(tx.amount, tx.currency)}
                </TableCell>
                <TableCell className="text-text-secondary text-xs">{tx.currency}</TableCell>
                <TableCell>
                  <Tooltip>
                    <TooltipTrigger className="text-sm text-text-secondary">
                      {formatRelativeTime(tx.created_at)}
                    </TooltipTrigger>
                    <TooltipContent>
                      {new Date(tx.created_at).toLocaleString("pt-BR")}
                    </TooltipContent>
                  </Tooltip>
                </TableCell>
                <TableCell className="text-sm text-text-secondary">
                  {tx.masked_card ?? "—"}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <span className="text-sm text-text-secondary">
            {totalItems} transações · página {page} de {totalPages}
          </span>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={page === 1}
            >
              Anterior
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
              disabled={page === totalPages}
            >
              Próxima
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
