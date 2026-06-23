"use client";

import { useQuery } from "@tanstack/react-query";
import { listPayments } from "@/lib/api/payments";
import type { PaymentResponse } from "@/types/payment";

export function useTransactionList() {
  return useQuery<PaymentResponse[], Error>({
    queryKey: ["transactions"],
    queryFn: listPayments,
    staleTime: 30_000,
    refetchInterval: 30_000,
  });
}
