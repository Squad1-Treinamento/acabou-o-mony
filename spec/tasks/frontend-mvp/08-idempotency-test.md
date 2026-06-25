# Task 08: Idempotency Test Screen

**Status**: Not Started  
**Estimated Time**: 2 hours

## Goal

Create idempotency testing screen to verify duplicate request handling.

## Acceptance Criteria

- [ ] Form similar to PaymentForm but with manual idempotency key
- [ ] "Submit Payment" button
- [ ] "Submit Again (Same Key)" button
- [ ] Display both responses side-by-side
- [ ] Show `X-Idempotent-Replayed` header value
- [ ] Verify second response is identical to first

## Implementation Steps

### 1. Update API Client to Capture Response Headers

**`src/services/api.ts`** (add new method):
```typescript
// Add this interface at the top
export interface PaymentResponseWithHeaders {
  data: PaymentResponse;
  headers: {
    idempotentReplayed?: string;
  };
}

// Add this new method to ApiClient class
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
```

### 2. Create IdempotencyTest Component

**`src/components/IdempotencyTest.tsx`**:
```typescript
import { useState, FormEvent, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { apiClient, PaymentResponseWithHeaders } from '../services/api';
import { PaymentRequest } from '../types/payment';
import { generateIdempotencyKey } from '../utils/uuid';

export function IdempotencyTest() {
  const { merchantId } = useAuth();
  const [amount, setAmount] = useState('10000');
  const [currency, setCurrency] = useState('BRL');
  const [cardToken, setCardToken] = useState('tok_visa_approved');
  const [customerId, setCustomerId] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [firstResponse, setFirstResponse] = useState<PaymentResponseWithHeaders | null>(null);
  const [secondResponse, setSecondResponse] = useState<PaymentResponseWithHeaders | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setIdempotencyKey(generateIdempotencyKey());
  }, []);

  const buildRequest = (): PaymentRequest => {
    if (!merchantId) throw new Error('Merchant ID not found');
    
    return {
      merchant_id: merchantId,
      amount: parseInt(amount),
      currency,
      card_token: cardToken,
      customer_id: customerId || undefined,
      idempotency_key: idempotencyKey,
    };
  };

  const handleFirstSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setFirstResponse(null);
    setSecondResponse(null);

    try {
      const result = await apiClient.createPaymentWithHeaders(buildRequest());
      setFirstResponse(result);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSecondSubmit = async () => {
    if (!firstResponse) return;

    setLoading(true);
    setError(null);

    try {
      const result = await apiClient.createPaymentWithHeaders(buildRequest());
      setSecondResponse(result);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setIdempotencyKey(generateIdempotencyKey());
    setFirstResponse(null);
    setSecondResponse(null);
    setError(null);
  };

  const isValid = amount && parseInt(amount) > 0 && cardToken && idempotencyKey;

  return (
    <div className="max-w-6xl mx-auto">
      <h2 className="text-2xl font-bold text-gray-900 mb-6">
        Idempotency Test
      </h2>

      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
        <p className="text-sm text-blue-800">
          <strong>Test Purpose:</strong> Submit the same payment request twice with the same
          idempotency key. The second request should return the cached response with{' '}
          <code className="bg-blue-100 px-1 rounded">X-Idempotent-Replayed: true</code>.
        </p>
      </div>

      <form onSubmit={handleFirstSubmit} className="bg-white p-6 rounded-lg shadow-md mb-6">
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Amount (cents)
            </label>
            <input
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="10000"
              className="w-full px-3 py-2 border border-gray-300 rounded-md"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Currency
            </label>
            <select
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md"
            >
              <option value="BRL">BRL</option>
              <option value="USD">USD</option>
            </select>
          </div>
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Card Token
          </label>
          <input
            type="text"
            value={cardToken}
            onChange={(e) => setCardToken(e.target.value)}
            placeholder="tok_visa_approved"
            className="w-full px-3 py-2 border border-gray-300 rounded-md"
          />
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Customer ID (optional)
          </label>
          <input
            type="text"
            value={customerId}
            onChange={(e) => setCustomerId(e.target.value)}
            placeholder="cust_123"
            className="w-full px-3 py-2 border border-gray-300 rounded-md"
          />
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Idempotency Key (editable for testing)
          </label>
          <input
            type="text"
            value={idempotencyKey}
            onChange={(e) => setIdempotencyKey(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md font-mono text-sm"
          />
        </div>

        <div className="flex gap-2">
          <button
            type="submit"
            disabled={!isValid || loading}
            className={`flex-1 py-2 px-4 rounded-md font-medium ${
              isValid && !loading
                ? 'bg-blue-600 text-white hover:bg-blue-700'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            }`}
          >
            {loading ? 'Processing...' : '1. Submit Payment'}
          </button>

          <button
            type="button"
            onClick={handleSecondSubmit}
            disabled={!firstResponse || loading}
            className={`flex-1 py-2 px-4 rounded-md font-medium ${
              firstResponse && !loading
                ? 'bg-green-600 text-white hover:bg-green-700'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            }`}
          >
            {loading ? 'Processing...' : '2. Submit Again (Same Key)'}
          </button>

          <button
            type="button"
            onClick={handleReset}
            className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700"
          >
            Reset
          </button>
        </div>
      </form>

      {error && (
        <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {(firstResponse || secondResponse) && (
        <div className="grid grid-cols-2 gap-6">
          {/* First Response */}
          <div>
            <h3 className="text-lg font-bold text-gray-900 mb-4">
              First Request
            </h3>
            {firstResponse && (
              <div className="bg-white border border-gray-200 rounded-lg p-4">
                <div className="mb-4">
                  <span className="text-xs font-medium text-gray-500">
                    X-Idempotent-Replayed
                  </span>
                  <p className="text-sm font-mono mt-1">
                    {firstResponse.headers.idempotentReplayed || 'false'}
                  </p>
                </div>
                <div className="space-y-2 text-sm">
                  <p>
                    <span className="font-medium">Transaction ID:</span>{' '}
                    <code className="bg-gray-100 px-2 py-1 rounded text-xs">
                      {firstResponse.data.transaction_id}
                    </code>
                  </p>
                  <p>
                    <span className="font-medium">Status:</span>{' '}
                    <span className="px-2 py-1 bg-green-100 text-green-800 rounded text-xs">
                      {firstResponse.data.status}
                    </span>
                  </p>
                  <p>
                    <span className="font-medium">Amount:</span>{' '}
                    {firstResponse.data.amount / 100} {firstResponse.data.currency}
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* Second Response */}
          <div>
            <h3 className="text-lg font-bold text-gray-900 mb-4">
              Second Request (Duplicate)
            </h3>
            {secondResponse ? (
              <div className="bg-white border border-gray-200 rounded-lg p-4">
                <div className="mb-4">
                  <span className="text-xs font-medium text-gray-500">
                    X-Idempotent-Replayed
                  </span>
                  <p
                    className={`text-sm font-mono mt-1 ${
                      secondResponse.headers.idempotentReplayed === 'true'
                        ? 'text-green-600 font-bold'
                        : 'text-red-600'
                    }`}
                  >
                    {secondResponse.headers.idempotentReplayed || 'false'}
                  </p>
                </div>
                <div className="space-y-2 text-sm">
                  <p>
                    <span className="font-medium">Transaction ID:</span>{' '}
                    <code className="bg-gray-100 px-2 py-1 rounded text-xs">
                      {secondResponse.data.transaction_id}
                    </code>
                  </p>
                  <p>
                    <span className="font-medium">Status:</span>{' '}
                    <span className="px-2 py-1 bg-green-100 text-green-800 rounded text-xs">
                      {secondResponse.data.status}
                    </span>
                  </p>
                  <p>
                    <span className="font-medium">Amount:</span>{' '}
                    {secondResponse.data.amount / 100} {secondResponse.data.currency}
                  </p>
                </div>

                {firstResponse &&
                  secondResponse.data.transaction_id === firstResponse.data.transaction_id && (
                    <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded">
                      <p className="text-xs text-green-800 font-medium">
                        ✓ Transaction IDs match - Idempotency working correctly!
                      </p>
                    </div>
                  )}
              </div>
            ) : (
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 text-center">
                <p className="text-sm text-gray-500">
                  Click "Submit Again" to test idempotency
                </p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
```

### 3. Update App.tsx to Include IdempotencyTest Tab

**`src/App.tsx`**:
```typescript
import { useAuth } from './context/AuthContext';
import { LoginForm } from './components/LoginForm';
import { PaymentForm } from './components/PaymentForm';
import { TransactionList } from './components/TransactionList';
import { TransactionDetails } from './components/TransactionDetails';
import { IdempotencyTest } from './components/IdempotencyTest';
import { apiClient } from './services/api';
import { useEffect, useState } from 'react';

type Tab = 'payment' | 'transactions' | 'details' | 'idempotency';

function App() {
  const { isAuthenticated, apiKey, merchantId, logout } = useAuth();
  const [activeTab, setActiveTab] = useState<Tab>('payment');
  const [selectedTransactionId, setSelectedTransactionId] = useState<string | null>(null);

  useEffect(() => {
    if (apiKey) {
      apiClient.setApiKey(apiKey);
    } else {
      apiClient.clearApiKey();
    }
  }, [apiKey]);

  if (!isAuthenticated) {
    return <LoginForm />;
  }

  const handleSelectTransaction = (transactionId: string) => {
    setSelectedTransactionId(transactionId);
    setActiveTab('details');
  };

  const handleBackToList = () => {
    setSelectedTransactionId(null);
    setActiveTab('transactions');
  };

  return (
    <div className="min-h-screen bg-gray-100">
      <header className="bg-white shadow">
        <div className="container mx-auto px-4 py-4 flex justify-between items-center">
          <h1 className="text-2xl font-bold text-gray-900">Acabou o Mony</h1>
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-600">Merchant: {merchantId}</span>
            <button
              onClick={logout}
              className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <nav className="bg-white border-b border-gray-200">
        <div className="container mx-auto px-4">
          <div className="flex space-x-8">
            <button
              onClick={() => setActiveTab('payment')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'payment'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              Create Payment
            </button>
            <button
              onClick={() => setActiveTab('transactions')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'transactions' || activeTab === 'details'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              Transactions
            </button>
            <button
              onClick={() => setActiveTab('idempotency')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'idempotency'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              Idempotency Test
            </button>
          </div>
        </div>
      </nav>

      <main className="container mx-auto px-4 py-8">
        {activeTab === 'payment' && <PaymentForm />}
        {activeTab === 'transactions' && (
          <TransactionList onSelectTransaction={handleSelectTransaction} />
        )}
        {activeTab === 'details' && selectedTransactionId && (
          <TransactionDetails
            transactionId={selectedTransactionId}
            onBack={handleBackToList}
          />
        )}
        {activeTab === 'idempotency' && <IdempotencyTest />}
      </main>
    </div>
  );
}

export default App;
```

## Validation

```bash
# Type checking
npm run type-check

# Start backend
# Make sure backend is running

# Start dev server
npm run dev

# Manual testing:
# 1. Login
# 2. Go to "Idempotency Test" tab
# 3. Fill in payment details
# 4. Click "1. Submit Payment"
# 5. Should show first response with X-Idempotent-Replayed: false
# 6. Click "2. Submit Again (Same Key)"
# 7. Should show second response with X-Idempotent-Replayed: true
# 8. Verify both transaction IDs are identical
# 9. Click "Reset" to start new test
```

## Files Created

- `src/components/IdempotencyTest.tsx`

## Files Modified

- `src/services/api.ts` (added `createPaymentWithHeaders` method)
- `src/App.tsx` (added idempotency test tab)

## Completion

✅ All 8 tasks completed!

The frontend MVP is now complete with:
- Mock login
- Payment processing
- Transaction list
- Transaction details
- Idempotency testing

## Next Steps

1. Test all features end-to-end with backend
2. Fix any bugs found during testing
3. Add real authentication (when backend supports it)
4. Add additional features (refunds, webhooks, etc.)
