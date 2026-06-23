"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { createPayment, getPayment } from "@/lib/api/payments";
import { TERMINAL_STATUSES } from "@/types/payment";
import type { PaymentRequest, PaymentResponse } from "@/types/payment";

const POLL_INTERVAL_MS = 2000;
const POLL_TIMEOUT_MS = 120_000;

export function useCreatePayment() {
  return useMutation<PaymentResponse, Error, { request: PaymentRequest; idempotencyKey: string }>({
    mutationFn: ({ request, idempotencyKey }) =>
      createPayment(request, idempotencyKey),
  });
}

export function useGetPayment(transactionId: string | null, enabled = true) {
  return useQuery<PaymentResponse, Error>({
    queryKey: ["payment", transactionId],
    queryFn: () => getPayment(transactionId!),
    enabled: !!transactionId && enabled,
    staleTime: 0,
    refetchInterval: (query) => {
      const data = query.state.data;
      if (!data) return POLL_INTERVAL_MS;
      if (TERMINAL_STATUSES.includes(data.status)) return false;
      return POLL_INTERVAL_MS;
    },
  });
}

export function usePaymentTimeout(
  startedAt: number | null,
  isPolling: boolean
): boolean {
  if (!startedAt || !isPolling) return false;
  return Date.now() - startedAt > POLL_TIMEOUT_MS;
}
