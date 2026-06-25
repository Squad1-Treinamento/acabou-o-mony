# Task 07: Transaction Details

**Status**: Not Started  
**Estimated Time**: 1.5 hours

## Goal

Create transaction details screen with full transaction information.

## Acceptance Criteria

- [ ] Fetch and display single transaction by ID
- [ ] Show all transaction fields
- [ ] "Back to List" button
- [ ] "Refresh Status" button
- [ ] Handle loading and error states
- [ ] Handle 404 (transaction not found)

## Implementation Steps

### 1. Create TransactionDetails Component

**`src/components/TransactionDetails.tsx`**:
```typescript
import { useState, useEffect } from 'react';
import { apiClient } from '../services/api';
import { TransactionDetails as Transaction, PaymentStatus } from '../types/payment';

interface TransactionDetailsProps {
  transactionId: string;
  onBack: () => void;
}

export function TransactionDetails({ transactionId, onBack }: TransactionDetailsProps) {
  const [transaction, setTransaction] = useState<Transaction | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchTransaction();
  }, [transactionId]);

  const fetchTransaction = async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await apiClient.getTransaction(transactionId);
      setTransaction(data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: PaymentStatus) => {
    switch (status) {
      case 'COMPLETED':
        return 'bg-green-100 text-green-800';
      case 'DECLINED':
      case 'FAILED':
        return 'bg-red-100 text-red-800';
      case 'PROCESSING':
      case 'CHALLENGE_PENDING':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  const formatAmount = (amount: number, currency: string) => {
    return `${(amount / 100).toFixed(2)} ${currency}`;
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="text-gray-600">Loading transaction...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-2xl mx-auto">
        <button
          onClick={onBack}
          className="mb-4 text-blue-600 hover:text-blue-800"
        >
          ← Back to List
        </button>
        <div className="bg-red-50 border border-red-200 rounded-lg p-6">
          <h3 className="text-lg font-bold text-red-900 mb-2">Error</h3>
          <p className="text-sm text-red-700">{error}</p>
          <button
            onClick={fetchTransaction}
            className="mt-4 px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!transaction) {
    return (
      <div className="max-w-2xl mx-auto">
        <button
          onClick={onBack}
          className="mb-4 text-blue-600 hover:text-blue-800"
        >
          ← Back to List
        </button>
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-6">
          <p className="text-gray-600">Transaction not found</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <button
          onClick={onBack}
          className="text-blue-600 hover:text-blue-800"
        >
          ← Back to List
        </button>
        <button
          onClick={fetchTransaction}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          Refresh Status
        </button>
      </div>

      <div className="bg-white rounded-lg shadow-md p-6">
        <h2 className="text-2xl font-bold text-gray-900 mb-6">
          Transaction Details
        </h2>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-500 mb-1">
              Transaction ID
            </label>
            <code className="block text-sm bg-gray-100 px-3 py-2 rounded font-mono">
              {transaction.id}
            </code>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-500 mb-1">
              Status
            </label>
            <span
              className={`inline-block px-3 py-1 text-sm font-medium rounded ${getStatusColor(
                transaction.status
              )}`}
            >
              {transaction.status}
            </span>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                Amount
              </label>
              <p className="text-lg font-semibold text-gray-900">
                {formatAmount(transaction.amount, transaction.currency)}
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                Currency
              </label>
              <p className="text-lg font-semibold text-gray-900">
                {transaction.currency}
              </p>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-500 mb-1">
              Merchant ID
            </label>
            <code className="block text-sm bg-gray-100 px-3 py-2 rounded font-mono">
              {transaction.merchant_id}
            </code>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-500 mb-1">
              Masked Card
            </label>
            <p className="text-sm text-gray-900 font-mono">
              {transaction.masked_card}
            </p>
          </div>

          {transaction.acquirer_reference && (
            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                Acquirer Reference (Mercado Pago ID)
              </label>
              <code className="block text-sm bg-gray-100 px-3 py-2 rounded font-mono">
                {transaction.acquirer_reference}
              </code>
            </div>
          )}

          {transaction.challenge_id && (
            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                3DS Challenge ID
              </label>
              <code className="block text-sm bg-yellow-100 px-3 py-2 rounded font-mono">
                {transaction.challenge_id}
              </code>
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                Created At
              </label>
              <p className="text-sm text-gray-900">
                {formatDate(transaction.created_at)}
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                Updated At
              </label>
              <p className="text-sm text-gray-900">
                {formatDate(transaction.updated_at)}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
```

### 2. Update App.tsx to Include TransactionDetails

**`src/App.tsx`**:
```typescript
import { useAuth } from './context/AuthContext';
import { LoginForm } from './components/LoginForm';
import { PaymentForm } from './components/PaymentForm';
import { TransactionList } from './components/TransactionList';
import { TransactionDetails } from './components/TransactionDetails';
import { apiClient } from './services/api';
import { useEffect, useState } from 'react';

type Tab = 'payment' | 'transactions' | 'details';

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
# 2. Go to "Transactions" tab
# 3. Click on a transaction
# 4. Should show transaction details
# 5. Verify all fields are displayed
# 6. Click "Refresh Status" button
# 7. Should reload transaction data
# 8. Click "Back to List"
# 9. Should return to transaction list
```

## Files Created

- `src/components/TransactionDetails.tsx`

## Files Modified

- `src/App.tsx` (added details view logic)

## Next Task

`08-idempotency-test.md` - Create idempotency testing screen
