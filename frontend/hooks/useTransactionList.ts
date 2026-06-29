"use client";

import { useQuery } from "@tanstack/react-query";
import { listPayments } from "@/lib/api/payments";
import type { PaymentResponse } from "@/types/payment";

export function useTransactionList() {
  return useQuery<PaymentResponse[], Error>({
    queryKey: ["transactions"],
    queryFn: listPayments,
    staleTime: 30_000,
    refetchInterval: (query) => {
      const data = query.state.data;
      if (data?.some((tx) => tx.status === "UNKNOWN")) return 10_000;
      return 30_000;
    },
  });
}
