export type PaymentStatus =
  | "CREATED"
  | "VALIDATED"
  | "PROCESSING"
  | "COMPLETED"
  | "DECLINED"
  | "FAILED"
  | "UNKNOWN"
  | "CHALLENGE_PENDING"
  | "AUTHENTICATED";

export const TERMINAL_STATUSES: readonly PaymentStatus[] = [
  "COMPLETED",
  "DECLINED",
  "FAILED",
] as const;

export const NON_TERMINAL_STATUSES: readonly PaymentStatus[] = [
  "CREATED",
  "VALIDATED",
  "PROCESSING",
  "UNKNOWN",
  "CHALLENGE_PENDING",
  "AUTHENTICATED",
] as const;

export interface PaymentMethod {
  card_token_id: string;
  masked_card?: string;
}

export interface PaymentRequest {
  amount: number;
  currency: string;
  idempotency_key: string;
  payment_method: PaymentMethod;
  customer_email?: string;
}

export interface PaymentResponse {
  transaction_id: string;
  status: PaymentStatus;
  amount: number;
  currency: string;
  masked_card?: string;
  challenge_id?: string;
  acs_url?: string;
  message?: string;
  created_at: string;
  updated_at: string;
  idempotency_key?: string;
}

export interface ApiError {
  status: number;
  message: string;
  field?: string;
}
