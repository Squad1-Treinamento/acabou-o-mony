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
