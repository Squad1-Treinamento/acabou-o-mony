export type PaymentStatus = 
  | 'CREATED'
  | 'VALIDATED'
  | 'CHALLENGE_PENDING'
  | 'AUTHENTICATED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'DECLINED'
  | 'FAILED'
  | 'UNKNOWN';

export interface PaymentMethod {
  card_token_id: string;
  masked_card?: string;
}

export interface PaymentRequest {
  amount: number;           // cents
  currency: string;         // "BRL" | "USD"
  idempotency_key: string;  // UUID
  payment_method: PaymentMethod;
  customer_id?: string;
  customer_email?: string;
}

export interface PaymentResponse {
  transaction_id: string;
  status: PaymentStatus;
  amount: number;
  currency: string;
  challenge_id?: string;
  acs_url?: string;
  created_at: string;
}

export interface TransactionDetails {
  id: string;
  merchant_id: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  masked_card: string;
  acquirer_reference?: string;
  challenge_id?: string;
  created_at: string;
  updated_at: string;
}

export interface ApiError {
  message: string;
  status: number;
}
