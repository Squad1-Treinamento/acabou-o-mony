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

export interface PaymentRequest {
  merchant_id: string;
  amount: number;           // cents
  currency: string;         // "BRL" | "USD"
  card_token: string;
  customer_id?: string;
  idempotency_key: string;  // UUID
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
