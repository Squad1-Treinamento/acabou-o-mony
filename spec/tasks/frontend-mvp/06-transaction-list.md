# Task 06: Transaction List

**Status**: Not Started  
**Estimated Time**: 2 hours

## Goal

Create transaction list screen with filtering and detail view navigation.

## Acceptance Criteria

- [ ] Fetch and display all transactions for merchant
- [ ] Table with transaction data
- [ ] Filter by status
- [ ] Click row to view details
- [ ] Handle empty state
- [ ] Handle loading and error states

## Implementation Steps

### 1. Create TransactionList Component

**`src/components/TransactionList.tsx`**:
```typescript
import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { apiClient } from '../services/api';
import { TransactionDetails, PaymentStatus } from '../types/payment';

interface TransactionListProps {
  onSelectTransaction: (transactionId: string) => void;
}

export function TransactionList({ onSelectTransaction }: TransactionListProps) {
  const { merchantId } = useAuth();
  const [transactions, setTransactions] = useState<TransactionDetails[]>([]);
  const [filteredTransactions, setFilteredTransactions] = useState<TransactionDetails[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  useEffect(() => {
    fetchTransactions();
  }, [merchantId]);

  useEffect(() => {
    if (statusFilter === 'ALL') {
      setFilteredTransactions(transactions);
    } else {
      setFilteredTransactions(
        transactions.filter((tx) => tx.status === statusFilter)
      );
    }
  }, [statusFilter, transactions]);

  const fetchTransactions = async () => {
    if (!merchantId) return;

    setLoading(true);
    setError(null);

    try {
      const data = await apiClient.getTransactions(merchantId);
      setTransactions(data);
      setFilteredTransactions(data);
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
        <div className="text-gray-600">Loading transactions...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-6">
        <h3 className="text-lg font-bold text-red-900 mb-2">Error</h3>
        <p className="text-sm text-red-700">{error}</p>
        <button
          onClick={fetchTransactions}
          className="mt-4 px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900">Transactions</h2>
        <button
          onClick={fetchTransactions}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          Refresh
        </button>
      </div>

      <div className="bg-white rounded-lg shadow-md p-4 mb-4">
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Filter by Status
        </label>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-3 py-2 border border-gray-300 rounded-md"
        >
          <option value="ALL">All</option>
          <option value="COMPLETED">Completed</option>
          <option value="DECLINED">Declined</option>
          <option value="FAILED">Failed</option>
          <option value="PROCESSING">Processing</option>
          <option value="CHALLENGE_PENDING">Challenge Pending</option>
        </select>
        <span className="ml-4 text-sm text-gray-600">
          Showing {filteredTransactions.length} of {transactions.length} transactions
        </span>
      </div>

      {filteredTransactions.length === 0 ? (
        <div className="bg-white rounded-lg shadow-md p-12 text-center">
          <p className="text-gray-600">No transactions found</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow-md overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Transaction ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Amount
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Created At
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredTransactions.map((tx) => (
                <tr
                  key={tx.id}
                  className="hover:bg-gray-50 cursor-pointer"
                  onClick={() => onSelectTransaction(tx.id)}
                >
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-900">
                    {tx.id.substring(0, 8)}...
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {formatAmount(tx.amount, tx.currency)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span
                      className={`px-2 py-1 text-xs font-medium rounded ${getStatusColor(
                        tx.status
                      )}`}
                    >
                      {tx.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {formatDate(tx.created_at)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelectTransaction(tx.id);
                      }}
                      className="text-blue-600 hover:text-blue-800"
                    >
                      View Details
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
```

### 2. Update App.tsx to Include Tab Navigation

**`src/App.tsx`**:
```typescript
import { useAuth } from './context/AuthContext';
import { LoginForm } from './components/LoginForm';
import { PaymentForm } from './components/PaymentForm';
import { TransactionList } from './components/TransactionList';
import { apiClient } from './services/api';
import { useEffect, useState } from 'react';

type Tab = 'payment' | 'transactions';

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
    // Will implement details view in next task
    alert(`Selected transaction: ${transactionId}`);
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
                activeTab === 'transactions'
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
# Make sure backend is running with some test transactions

# Start dev server
npm run dev

# Manual testing:
# 1. Login
# 2. Click "Transactions" tab
# 3. Should show list of transactions
# 4. Try filtering by status
# 5. Click on a transaction row
# 6. Should show alert with transaction ID (temporary)
# 7. Click "Refresh" button
# 8. Should reload transactions
```

## Files Created

- `src/components/TransactionList.tsx`

## Files Modified

- `src/App.tsx` (added tab navigation)

## Next Task

`07-transaction-details.md` - Create transaction details screen
