import axios, { type AxiosError, type AxiosInstance } from 'axios';
import type { PaymentRequest, PaymentResponse, TransactionDetails, ApiError } from '../types/payment.ts';

export interface PaymentResponseWithHeaders {
  data: PaymentResponse;
  headers: {
    idempotentReplayed?: string;
  };
}

// Use relative path to leverage Vite proxy (avoids CORS in development)
// In production, set VITE_API_URL to the actual backend URL
const API_BASE_URL = import.meta.env.VITE_API_URL || '/api/v1';

class ApiClient {
  private client: AxiosInstance;
  private apiKey: string | null = null;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Add request interceptor to include auth header
    this.client.interceptors.request.use((config) => {
      if (this.apiKey) {
        config.headers.Authorization = `Bearer ${this.apiKey}`;
      }
      return config;
    });
  }

  setApiKey(apiKey: string) {
    this.apiKey = apiKey;
  }

  clearApiKey() {
    this.apiKey = null;
  }

  private handleError(error: AxiosError): ApiError {
    if (error.response) {
      return {
        message: (error.response.data as any)?.message || error.message,
        status: error.response.status,
      };
    } else if (error.request) {
      return {
        message: 'Cannot connect to server',
        status: 0,
      };
    } else {
      return {
        message: error.message,
        status: 0,
      };
    }
  }

  async createPayment(request: PaymentRequest): Promise<PaymentResponse> {
    try {
      const response = await this.client.post<PaymentResponse>('/payments', request);
      return response.data;
    } catch (error) {
      throw this.handleError(error as AxiosError);
    }
  }

  async createPaymentWithHeaders(request: PaymentRequest): Promise<PaymentResponseWithHeaders> {
    try {
      const response = await this.client.post<PaymentResponse>('/payments', request);
      return {
        data: response.data,
        headers: {
          idempotentReplayed: response.headers['x-idempotent-replayed'],
        },
      };
    } catch (error) {
      throw this.handleError(error as AxiosError);
    }
  }

  async getTransactions(merchantId?: string): Promise<TransactionDetails[]> {
    try {
      const params: Record<string, string> = {};
      if (merchantId) {
        params.merchant_id = merchantId;
      }
      const response = await this.client.get<TransactionDetails[]>('/payments', { params });
      return response.data;
    } catch (error) {
      throw this.handleError(error as AxiosError);
    }
  }

  async getTransaction(transactionId: string): Promise<TransactionDetails> {
    try {
      const response = await this.client.get<TransactionDetails>(`/payments/${transactionId}`);
      return response.data;
    } catch (error) {
      throw this.handleError(error as AxiosError);
    }
  }
}

export const apiClient = new ApiClient();
