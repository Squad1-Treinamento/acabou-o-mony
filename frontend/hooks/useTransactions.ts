"use client";

import { useQueries } from "@tanstack/react-query";
import { getPayment } from "@/lib/api/payments";
import { TERMINAL_STATUSES } from "@/types/payment";
import type { PaymentResponse, PaymentStatus } from "@/types/payment";

const DASHBOARD_POLL_INTERVAL_MS = 30_000;
const UNKNOWN_STALE_MS = 5 * 60 * 1000;

export function resolveDisplayStatus(tx: PaymentResponse): PaymentStatus {
  if (tx.status === "UNKNOWN") {
    const ageMs = Date.now() - new Date(tx.created_at).getTime();
    if (ageMs >= UNKNOWN_STALE_MS) return "DECLINED";
  }
  return tx.status;
}

export interface TransactionFilters {
  statuses?: PaymentStatus[];
  dateFrom?: string;
  dateTo?: string;
  idPrefix?: string;
}

export type SortField = "created_at" | "amount" | "status";
export type SortDirection = "asc" | "desc";

export interface TransactionSort {
  field: SortField;
  direction: SortDirection;
}

export function filterTransactions(
  results: PaymentResponse[],
  filters: TransactionFilters
): PaymentResponse[] {
  return results.filter((tx) => {
    if (filters.statuses?.length && !filters.statuses.includes(resolveDisplayStatus(tx))) {
      return false;
    }
    if (filters.dateFrom && tx.created_at < filters.dateFrom) return false;
    if (filters.dateTo && tx.created_at > filters.dateTo + "T23:59:59Z") return false;
    if (
      filters.idPrefix &&
      !tx.transaction_id.toLowerCase().startsWith(filters.idPrefix.toLowerCase())
    ) {
      return false;
    }
    return true;
  });
}

export function sortTransactions(
  results: PaymentResponse[],
  sort: TransactionSort
): PaymentResponse[] {
  return [...results].sort((a, b) => {
    let cmp = 0;
    if (sort.field === "created_at") {
      cmp = a.created_at.localeCompare(b.created_at);
    } else if (sort.field === "amount") {
      cmp = a.amount - b.amount;
    } else if (sort.field === "status") {
      cmp = a.status.localeCompare(b.status);
    }
    return sort.direction === "asc" ? cmp : -cmp;
  });
}

export function paginateTransactions(
  results: PaymentResponse[],
  page: number,
  pageSize = 20
): { items: PaymentResponse[]; totalPages: number; totalItems: number } {
  const totalItems = results.length;
  const totalPages = Math.ceil(totalItems / pageSize);
  const items = results.slice((page - 1) * pageSize, page * pageSize);
  return { items, totalPages, totalItems };
}

export function useTransactions(transactionIds: string[]) {
  const queries = useQueries({
    queries: transactionIds.map((id) => ({
      queryKey: ["payment", id],
      queryFn: () => getPayment(id),
      staleTime: 0,
      refetchInterval: (query: { state: { data?: PaymentResponse } }) => {
        const data = query.state.data;
        if (!data) return DASHBOARD_POLL_INTERVAL_MS;
        if (TERMINAL_STATUSES.includes(data.status)) return false;
        return DASHBOARD_POLL_INTERVAL_MS;
      },
    })),
  });

  const isLoading = queries.some((q) => q.isLoading);
  const results = queries
    .map((q) => q.data)
    .filter((d): d is PaymentResponse => !!d);

  function refetchAll() {
    queries.forEach((q) => q.refetch());
  }

  return { results, isLoading, refetchAll };
}
