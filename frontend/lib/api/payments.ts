import { apiFetch } from "./client";
import type { PaymentRequest, PaymentResponse, ApiError } from "@/types/payment";

export async function listPayments(): Promise<PaymentResponse[]> {
  const response = await apiFetch("/api/v1/payments");
  if (!response.ok) {
    const err: ApiError = { status: response.status, message: `HTTP ${response.status}` };
    throw err;
  }
  return response.json() as Promise<PaymentResponse[]>;
}

async function parseOrThrow(response: Response): Promise<PaymentResponse> {
  if (!response.ok && response.status !== 401) {
    let message = `HTTP ${response.status}`;
    try {
      const body = await response.json();
      message = body?.message ?? message;
    } catch {
      // ignore parse error
    }
    const err: ApiError = { status: response.status, message };
    throw err;
  }
  return response.json() as Promise<PaymentResponse>;
}

export async function createPayment(
  req: PaymentRequest,
  idempotencyKey: string
): Promise<PaymentResponse> {
  const response = await apiFetch("/api/v1/payments", {
    method: "POST",
    headers: { "Idempotency-Key": idempotencyKey },
    body: JSON.stringify(req),
  });
  return parseOrThrow(response);
}

export async function getPayment(id: string): Promise<PaymentResponse> {
  const response = await apiFetch(`/api/v1/payments/${id}`);
  return parseOrThrow(response);
}
