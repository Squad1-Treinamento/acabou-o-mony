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
        <div className="text-nu-text-secondary">Loading transaction...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-2xl mx-auto">
        <button
          onClick={onBack}
          className="nu-btn-ghost -ml-2 mb-4"
        >
          ← Back to List
        </button>
        <div className="bg-red-50 border border-nu-error/20 rounded-xl p-6">
          <h3 className="text-lg font-bold text-nu-error mb-2">Error</h3>
          <p className="text-sm text-nu-error/80">{error}</p>
          <button
            onClick={fetchTransaction}
            className="mt-4 px-4 py-2 rounded-full bg-nu-error text-white text-sm hover:brightness-110 transition-all"
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
          className="nu-btn-ghost -ml-2 mb-4"
        >
          ← Back to List
        </button>
        <div className="bg-gray-50 rounded-xl p-6">
          <p className="text-nu-text-secondary">Transaction not found</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <button
          onClick={onBack}
          className="nu-btn-ghost -ml-2"
        >
          ← Back to List
        </button>
        <button
          onClick={fetchTransaction}
          className="px-4 py-2 rounded-full bg-nu-purple-light text-nu-purple text-sm font-medium hover:bg-[#E4D5F5] transition-colors"
        >
          Refresh Status
        </button>
      </div>

      <div className="nu-card">
        <h2 className="text-xl font-bold text-nu-text-primary mb-6">
          Transaction Details
        </h2>

        <div className="space-y-5">
          <div>
            <label className="nu-label text-xs uppercase tracking-wider">
              Transaction ID
            </label>
            <code className="block text-sm bg-nu-bg px-3 py-2.5 rounded-lg font-mono text-nu-text-primary">
              {transaction.id}
            </code>
          </div>

          <div>
            <label className="nu-label text-xs uppercase tracking-wider">
              Status
            </label>
            <span className={`inline-block ${getStatusBadge(transaction.status)}`}>
              {transaction.status}
            </span>
          </div>

          <div className="grid grid-cols-2 gap-6">
            <div>
              <label className="nu-label text-xs uppercase tracking-wider">
                Amount
              </label>
              <p className="text-lg font-semibold text-nu-text-primary">
                {formatAmount(transaction.amount, transaction.currency)}
              </p>
            </div>

            <div>
              <label className="nu-label text-xs uppercase tracking-wider">
                Currency
              </label>
              <p className="text-lg font-semibold text-nu-text-primary">
                {transaction.currency}
              </p>
            </div>
          </div>

          <div>
            <label className="nu-label text-xs uppercase tracking-wider">
              Merchant ID
            </label>
            <code className="block text-sm bg-nu-bg px-3 py-2.5 rounded-lg font-mono text-nu-text-primary">
              {transaction.merchant_id}
            </code>
          </div>

          <div>
            <label className="nu-label text-xs uppercase tracking-wider">
              Masked Card
            </label>
            <p className="text-sm text-nu-text-primary font-mono">
              {transaction.masked_card}
            </p>
          </div>

          {transaction.acquirer_reference && (
            <div>
              <label className="nu-label text-xs uppercase tracking-wider">
                Acquirer Reference
              </label>
              <code className="block text-sm bg-nu-bg px-3 py-2.5 rounded-lg font-mono text-nu-text-primary">
                {transaction.acquirer_reference}
              </code>
            </div>
          )}

          {transaction.challenge_id && (
            <div>
              <label className="nu-label text-xs uppercase tracking-wider">
                3DS Challenge ID
              </label>
              <code className="block text-sm bg-orange-50 border border-nu-warning/20 px-3 py-2.5 rounded-lg font-mono text-nu-warning">
                {transaction.challenge_id}
              </code>
            </div>
          )}

          <div className="grid grid-cols-2 gap-6">
            <div>
              <label className="nu-label text-xs uppercase tracking-wider">
                Created At
              </label>
              <p className="text-sm text-nu-text-primary">
                {formatDate(transaction.created_at)}
              </p>
            </div>

            <div>
              <label className="nu-label text-xs uppercase tracking-wider">
                Updated At
              </label>
              <p className="text-sm text-nu-text-primary">
                {formatDate(transaction.updated_at)}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
