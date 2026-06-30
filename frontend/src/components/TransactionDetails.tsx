import { useState, useEffect, useCallback } from 'react';
import { apiClient } from '../services/api';
import type { TransactionDetails as Transaction, PaymentStatus } from '../types/payment';

interface TransactionDetailsProps {
  transactionId: string;
  onBack: () => void;
}

export function TransactionDetails({ transactionId, onBack }: TransactionDetailsProps) {
  const [transaction, setTransaction] = useState<Transaction | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTransaction = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await apiClient.getTransaction(transactionId);
      setTransaction(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load transaction');
    } finally {
      setLoading(false);
    }
  }, [transactionId]);

  useEffect(() => {
    fetchTransaction();
  }, [fetchTransaction]);

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
        <div className="text-ml-text-secondary">Loading transaction...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-2xl mx-auto">
        <button
          onClick={onBack}
          className="mb-4 text-ml-blue hover:text-ml-blue-dark transition-colors"
        >
          ← Back to List
        </button>
        <div className="bg-red-50 border border-ml-red/20 rounded-lg p-6">
          <h3 className="text-lg font-bold text-ml-red mb-2">Error</h3>
          <p className="text-sm text-ml-red/80">{error}</p>
          <button
            onClick={fetchTransaction}
            className="mt-4 px-4 py-2 bg-ml-red text-white rounded-md text-sm hover:brightness-110 transition-all"
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
          className="mb-4 text-ml-blue hover:text-ml-blue-dark transition-colors"
        >
          ← Back to List
        </button>
        <div className="bg-gray-50 border border-ml-border rounded-lg p-6">
          <p className="text-ml-text-secondary">Transaction not found</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <button
          onClick={onBack}
          className="text-ml-blue hover:text-ml-blue-dark transition-colors"
        >
          ← Back to List
        </button>
        <button
          onClick={fetchTransaction}
          className="px-4 py-2 bg-ml-blue text-white rounded-md text-sm hover:bg-ml-blue-dark transition-colors"
        >
          Refresh Status
        </button>
      </div>

      <div className="bg-ml-surface rounded-lg shadow-sm p-6">
        <h2 className="text-xl font-bold text-ml-text-primary mb-6">
          Transaction Details
        </h2>

        <div className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-ml-text-muted uppercase tracking-wider mb-1">
              Transaction ID
            </label>
            <code className="block text-sm bg-gray-50 border border-ml-border px-3 py-2 rounded font-mono text-ml-text-primary">
              {transaction.id}
            </code>
          </div>

          <div>
            <label className="block text-xs font-medium text-ml-text-muted uppercase tracking-wider mb-1">
              Status
            </label>
            <span className={`inline-block px-3 py-1 text-sm font-medium rounded ${getStatusColor(transaction.status)}`}>
              {transaction.status}
            </span>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-ml-text-muted uppercase tracking-wider mb-1">
                Amount
              </label>
              <p className="text-lg font-semibold text-ml-text-primary">
                {formatAmount(transaction.amount, transaction.currency)}
              </p>
            </div>

            <div>
              <label className="block text-xs font-medium text-ml-text-muted uppercase tracking-wider mb-1">
                Currency
              </label>
              <p className="text-lg font-semibold text-ml-text-primary">
                {transaction.currency}
              </p>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-ml-text-muted uppercase tracking-wider mb-1">
              Merchant ID
            </label>
            <code className="block text-sm bg-gray-50 border border-ml-border px-3 py-2 rounded font-mono text-ml-text-primary">
              {transaction.merchant_id}
            </code>
          </div>

          <div>
            <label className="block text-xs font-medium text-ml-text-muted uppercase tracking-wider mb-1">
              Masked Card
            </label>
            <p className="text-sm text-ml-text-primary font-mono">
              {transaction.masked_card}
            </p>
          </div>

          {transaction.acquirer_reference && (
            <div>
              <label className="block text-xs font-medium text-ml-text-muted uppercase tracking-wider mb-1">
                Acquirer Reference
              </label>
              <code className="block text-sm bg-gray-50 border border-ml-border px-3 py-2 rounded font-mono text-ml-text-primary">
                {transaction.acquirer_reference}
              </code>
            </div>
          )}

          {transaction.challenge_id && (
            <div>
              <label className="block text-xs font-medium text-ml-text-muted uppercase tracking-wider mb-1">
                3DS Challenge ID
              </label>
              <code className="block text-sm bg-ml-orange/5 border border-ml-orange/20 px-3 py-2 rounded font-mono text-ml-orange">
                {transaction.challenge_id}
              </code>
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-ml-text-muted uppercase tracking-wider mb-1">
                Created At
              </label>
              <p className="text-sm text-ml-text-primary">
                {formatDate(transaction.created_at)}
              </p>
            </div>

            <div>
              <label className="block text-xs font-medium text-ml-text-muted uppercase tracking-wider mb-1">
                Updated At
              </label>
              <p className="text-sm text-ml-text-primary">
                {formatDate(transaction.updated_at)}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
