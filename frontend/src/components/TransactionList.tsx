import { useState, useEffect, useCallback } from 'react';
import { apiClient } from '../services/api';
import type { TransactionDetails, PaymentStatus } from '../types/payment';

interface TransactionListProps {
  onSelectTransaction: (transactionId: string) => void;
}

export function TransactionList({ onSelectTransaction }: TransactionListProps) {
  const [transactions, setTransactions] = useState<TransactionDetails[]>([]);
  const [filteredTransactions, setFilteredTransactions] = useState<TransactionDetails[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  const fetchTransactions = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await apiClient.getTransactions();
      setTransactions(data);
      setFilteredTransactions(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load transactions');
    } finally {
      setLoading(false);
    }
  }, []);

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

  const getStatusBadge = (status: PaymentStatus) => {
    switch (status) {
      case 'COMPLETED':
        return 'nu-badge-success';
      case 'DECLINED':
      case 'FAILED':
        return 'nu-badge-error';
      case 'PROCESSING':
      case 'CHALLENGE_PENDING':
        return 'nu-badge-warning';
      default:
        return 'nu-badge-neutral';
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
        <div className="text-nu-text-secondary">Loading transactions...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-nu-error/20 rounded-xl p-6">
        <h3 className="text-lg font-bold text-nu-error mb-2">Error</h3>
        <p className="text-sm text-nu-error/80">{error}</p>
        <button
          onClick={fetchTransactions}
          className="mt-4 px-4 py-2 rounded-full bg-nu-error text-white text-sm hover:brightness-110 transition-all"
        >
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-bold text-nu-text-primary">Transactions</h2>
        <button
          onClick={fetchTransactions}
          className="px-4 py-2 rounded-full bg-nu-purple-light text-nu-purple text-sm font-medium hover:bg-[#E4D5F5] transition-colors"
        >
          Refresh
        </button>
      </div>

      <div className="nu-card mb-4">
        <label className="nu-label">Filter by Status</label>
        <div className="flex items-center gap-3">
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="nu-input max-w-xs"
          >
            <option value="ALL">All</option>
            <option value="COMPLETED">Completed</option>
            <option value="DECLINED">Declined</option>
            <option value="FAILED">Failed</option>
            <option value="PROCESSING">Processing</option>
            <option value="CHALLENGE_PENDING">Challenge Pending</option>
          </select>
          <span className="text-sm text-nu-text-muted">
            {filteredTransactions.length} of {transactions.length}
          </span>
        </div>
      </div>

      {filteredTransactions.length === 0 ? (
        <div className="nu-card py-12 text-center">
          <p className="text-nu-text-muted">No transactions found</p>
        </div>
      ) : (
        <div className="bg-nu-surface rounded-xl shadow-nu-sm overflow-hidden">
          <table className="min-w-full divide-y divide-nu-border">
            <thead className="bg-nu-bg">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-semibold text-nu-text-muted uppercase tracking-wider">Transaction ID</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-nu-text-muted uppercase tracking-wider">Merchant</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-nu-text-muted uppercase tracking-wider">Amount</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-nu-text-muted uppercase tracking-wider">Status</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-nu-text-muted uppercase tracking-wider">Created At</th>
                <th className="px-6 py-4 text-left text-xs font-semibold text-nu-text-muted uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-nu-border">
              {filteredTransactions.map((tx) => (
                <tr
                  key={tx.id}
                  className="hover:bg-nu-purple-light/30 cursor-pointer transition-colors duration-150"
                  onClick={() => onSelectTransaction(tx.id)}
                >
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-nu-text-primary">
                    {tx.id.substring(0, 8)}...
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-nu-text-muted">
                    {tx.merchant_id.substring(0, 12)}...
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-nu-text-primary font-medium">
                    {formatAmount(tx.amount, tx.currency)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`${getStatusBadge(tx.status)}`}>
                      {tx.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-nu-text-secondary">
                    {formatDate(tx.created_at)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelectTransaction(tx.id);
                      }}
                      className="text-nu-purple hover:text-nu-purple-dark font-medium transition-colors"
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
