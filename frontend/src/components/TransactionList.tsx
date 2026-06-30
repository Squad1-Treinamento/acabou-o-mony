import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { apiClient } from '../services/api';
import type { TransactionDetails, PaymentStatus } from '../types/payment';

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

  const fetchTransactions = useCallback(async () => {
    if (!merchantId) return;

    setLoading(true);
    setError(null);

    try {
      const data = await apiClient.getTransactions(merchantId);
      setTransactions(data);
      setFilteredTransactions(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load transactions');
    } finally {
      setLoading(false);
    }
  }, [merchantId]);

  useEffect(() => {
    fetchTransactions();
  }, [fetchTransactions]);

  useEffect(() => {
    if (statusFilter === 'ALL') {
      setFilteredTransactions(transactions);
    } else {
      setFilteredTransactions(
        transactions.filter((tx) => tx.status === statusFilter)
      );
    }
  }, [statusFilter, transactions]);

  const getStatusColor = (status: PaymentStatus) => {
    switch (status) {
      case 'COMPLETED':
        return 'bg-ml-green/10 text-ml-green';
      case 'DECLINED':
      case 'FAILED':
        return 'bg-ml-red/10 text-ml-red';
      case 'PROCESSING':
      case 'CHALLENGE_PENDING':
        return 'bg-ml-orange/10 text-ml-orange';
      default:
        return 'bg-gray-100 text-ml-text-secondary';
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
        <div className="text-ml-text-secondary">Loading transactions...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-ml-red/20 rounded-lg p-6">
        <h3 className="text-lg font-bold text-ml-red mb-2">Error</h3>
        <p className="text-sm text-ml-red/80">{error}</p>
        <button
          onClick={fetchTransactions}
          className="mt-4 px-4 py-2 bg-ml-red text-white rounded-md text-sm hover:brightness-110 transition-all"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-bold text-ml-text-primary">Transactions</h2>
        <button
          onClick={fetchTransactions}
          className="px-4 py-2 bg-ml-blue text-white rounded-md text-sm hover:bg-ml-blue-dark transition-colors"
        >
          Refresh
        </button>
      </div>

      <div className="bg-ml-surface rounded-lg shadow-sm p-4 mb-4">
        <label className="block text-sm font-medium text-ml-text-secondary mb-1.5">
          Filter by Status
        </label>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-3 py-2 border border-ml-border rounded-md text-sm text-ml-text-primary focus:outline-none focus:ring-2 focus:ring-ml-blue focus:border-transparent"
        >
          <option value="ALL">All</option>
          <option value="COMPLETED">Completed</option>
          <option value="DECLINED">Declined</option>
          <option value="FAILED">Failed</option>
          <option value="PROCESSING">Processing</option>
          <option value="CHALLENGE_PENDING">Challenge Pending</option>
        </select>
        <span className="ml-4 text-sm text-ml-text-muted">
          {filteredTransactions.length} of {transactions.length}
        </span>
      </div>

      {filteredTransactions.length === 0 ? (
        <div className="bg-ml-surface rounded-lg shadow-sm p-12 text-center">
          <p className="text-ml-text-muted">No transactions found</p>
        </div>
      ) : (
        <div className="bg-ml-surface rounded-lg shadow-sm overflow-hidden">
          <table className="min-w-full divide-y divide-ml-border">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-semibold text-ml-text-muted uppercase tracking-wider">Transaction ID</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-ml-text-muted uppercase tracking-wider">Amount</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-ml-text-muted uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-ml-text-muted uppercase tracking-wider">Created At</th>
                <th className="px-6 py-3 text-left text-xs font-semibold text-ml-text-muted uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ml-border">
              {filteredTransactions.map((tx) => (
                <tr
                  key={tx.id}
                  className="hover:bg-gray-50 cursor-pointer transition-colors"
                  onClick={() => onSelectTransaction(tx.id)}
                >
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-ml-text-primary">
                    {tx.id.substring(0, 8)}...
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-ml-text-primary font-medium">
                    {formatAmount(tx.amount, tx.currency)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 py-0.5 text-xs font-medium rounded ${getStatusColor(tx.status)}`}>
                      {tx.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-ml-text-secondary">
                    {formatDate(tx.created_at)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelectTransaction(tx.id);
                      }}
                      className="text-ml-blue hover:text-ml-blue-dark font-medium transition-colors"
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
