# Task 03: API Client

**Status**: Not Started  
**Estimated Time**: 1.5 hours

## Goal

Create Axios client with authentication headers and TypeScript types for API requests.

## Acceptance Criteria

- [ ] Axios instance configured with base URL
- [ ] Authorization header added to all requests
- [ ] TypeScript interfaces for request/response types
- [ ] Error handling wrapper
- [ ] API service methods created

## Implementation Steps

### 1. Create Payment Types

**`src/types/payment.ts`**:
```typescript
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
```

### 2. Create Axios Instance

**`src/services/api.ts`**:
```typescript
import axios, { AxiosError, AxiosInstance } from 'axios';
import { PaymentRequest, PaymentResponse, TransactionDetails, ApiError } from '../types/payment';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

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

  async getTransactions(merchantId: string): Promise<TransactionDetails[]> {
    try {
      const response = await this.client.get<TransactionDetails[]>('/payments', {
        params: { merchant_id: merchantId },
      });
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
```

### 3. Create Environment Variables File

**`.env.example`**:
```env
VITE_API_URL=http://localhost:8080/api/v1
```

**`.env`** (create this file, not committed to git):
```env
VITE_API_URL=http://localhost:8080/api/v1
```

### 4. Update .gitignore

Add to **`.gitignore`**:
```
.env
.env.local
```

### 5. Test API Client in App.tsx

**`src/App.tsx`** (temporary test):
```typescript
import { useAuth } from './context/AuthContext';
import { apiClient } from './services/api';
import { useEffect, useState } from 'react';

function App() {
  const { isAuthenticated, merchantId, apiKey, login, logout } = useAuth();
  const [testResult, setTestResult] = useState<string>('');

  useEffect(() => {
    if (apiKey) {
      apiClient.setApiKey(apiKey);
    } else {
      apiClient.clearApiKey();
    }
  }, [apiKey]);

  const testApiCall = async () => {
    if (!merchantId) return;
    
    try {
      const transactions = await apiClient.getTransactions(merchantId);
      setTestResult(`Success! Found ${transactions.length} transactions`);
    } catch (error: any) {
      setTestResult(`Error: ${error.message} (status: ${error.status})`);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100">
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-gray-900">
          Acabou o Mony - Payment Gateway
        </h1>
        
        <div className="mt-4">
          <p>Authenticated: {isAuthenticated ? 'Yes' : 'No'}</p>
          <p>Merchant ID: {merchantId || 'None'}</p>
          
          {!isAuthenticated ? (
            <button
              onClick={() => login('m_123', 'sk_test_abc')}
              className="mt-2 px-4 py-2 bg-blue-600 text-white rounded"
            >
              Test Login
            </button>
          ) : (
            <>
              <button
                onClick={logout}
                className="mt-2 px-4 py-2 bg-red-600 text-white rounded mr-2"
              >
                Logout
              </button>
              <button
                onClick={testApiCall}
                className="mt-2 px-4 py-2 bg-green-600 text-white rounded"
              >
                Test API Call
              </button>
            </>
          )}
          
          {testResult && (
            <p className="mt-4 text-sm text-gray-700">{testResult}</p>
          )}
        </div>
      </div>
    </div>
  );
}

export default App;
```

## Validation

```bash
# Type checking
npm run type-check

# Start backend (in separate terminal)
# Make sure backend is running on http://localhost:8080

# Start dev server
npm run dev

# Manual testing:
# 1. Click "Test Login"
# 2. Click "Test API Call"
# 3. Should show "Success! Found X transactions" or error message
```

## Files Created

- `src/types/payment.ts`
- `src/services/api.ts`
- `.env.example`
- `.env`

## Files Modified

- `.gitignore` (add .env)
- `src/App.tsx` (temporary test code)

## Next Task

`04-login-screen.md` - Create mock login form component
