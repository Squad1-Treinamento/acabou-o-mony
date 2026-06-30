import { useState, FormEvent, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { apiClient } from '../services/api';
import type { PaymentResponse, ApiError } from '../types/payment';
import { generateIdempotencyKey } from '../utils/uuid';

export function PaymentForm() {
  const { merchantId } = useAuth();
  const [amount, setAmount] = useState('10000');
  const [currency, setCurrency] = useState('BRL');
  const [cardToken, setCardToken] = useState('tok_visa_approved');
  const [customerId, setCustomerId] = useState('');
  const [customerEmail, setCustomerEmail] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [response, setResponse] = useState<PaymentResponse | null>(null);
  const [error, setError] = useState<ApiError | null>(null);

  useEffect(() => {
    setIdempotencyKey(generateIdempotencyKey());
  }, []);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!merchantId) return;

    setLoading(true);
    setResponse(null);
    setError(null);

    try {
      const result = await apiClient.createPayment({
        amount: parseInt(amount),
        currency,
        payment_method: {
          card_token_id: cardToken,
        },
        customer_id: customerId || undefined,
        customer_email: customerEmail || undefined,
        idempotency_key: idempotencyKey,
      });
      setResponse(result);
    } catch (err) {
      setError(err as ApiError);
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateNewKey = () => {
    setIdempotencyKey(generateIdempotencyKey());
    setResponse(null);
    setError(null);
  };

  const handleOpen3DS = () => {
    if (response?.acs_url) {
      window.open(response.acs_url, '_blank');
    }
  };

  const isValid = amount && parseInt(amount) > 0 && cardToken && idempotencyKey;

  return (
    <div className="max-w-2xl mx-auto">
      <h2 className="text-2xl font-bold text-gray-900 mb-6">
        Create Payment
      </h2>

      <form onSubmit={handleSubmit} className="bg-white p-6 rounded-lg shadow-md">
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
            <p className="text-xs text-gray-500 mt-1">
              {parseInt(amount) / 100 || 0} {currency}
            </p>
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
            Customer Email (optional)
          </label>
          <input
            type="email"
            value={customerEmail}
            onChange={(e) => setCustomerEmail(e.target.value)}
            placeholder="customer@example.com"
            className="w-full px-3 py-2 border border-gray-300 rounded-md"
          />
        </div>

        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Idempotency Key
          </label>
          <div className="flex gap-2">
            <input
              type="text"
              value={idempotencyKey}
              readOnly
              className="flex-1 px-3 py-2 border border-gray-300 rounded-md bg-gray-50"
            />
            <button
              type="button"
              onClick={handleGenerateNewKey}
              className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700"
            >
              New Key
            </button>
          </div>
        </div>

        <button
          type="submit"
          disabled={!isValid || loading}
          className={`w-full py-2 px-4 rounded-md font-medium ${
            isValid && !loading
              ? 'bg-blue-600 text-white hover:bg-blue-700'
              : 'bg-gray-300 text-gray-500 cursor-not-allowed'
          }`}
        >
          {loading ? 'Processing...' : 'Submit Payment'}
        </button>
      </form>

      {response && (
        <div className="mt-6 bg-green-50 border border-green-200 rounded-lg p-6">
          <h3 className="text-lg font-bold text-green-900 mb-4">
            Payment Response
          </h3>
          <div className="space-y-2 text-sm">
            <p>
              <span className="font-medium">Transaction ID:</span>{' '}
              <code className="bg-green-100 px-2 py-1 rounded">{response.transaction_id}</code>
            </p>
            <p>
              <span className="font-medium">Status:</span>{' '}
              <span className={`px-2 py-1 rounded ${
                response.status === 'COMPLETED' ? 'bg-green-200 text-green-800' :
                response.status === 'CHALLENGE_PENDING' ? 'bg-yellow-200 text-yellow-800' :
                'bg-gray-200 text-gray-800'
              }`}>
                {response.status}
              </span>
            </p>
            <p>
              <span className="font-medium">Amount:</span> {response.amount / 100} {response.currency}
            </p>
            {response.challenge_id && (
              <>
                <p>
                  <span className="font-medium">Challenge ID:</span>{' '}
                  <code className="bg-yellow-100 px-2 py-1 rounded">{response.challenge_id}</code>
                </p>
                <button
                  onClick={handleOpen3DS}
                  className="mt-4 w-full py-2 px-4 bg-yellow-600 text-white rounded-md hover:bg-yellow-700"
                >
                  Open 3DS Challenge
                </button>
              </>
            )}
          </div>
        </div>
      )}

      {error && (
        <div className="mt-6 bg-red-50 border border-red-200 rounded-lg p-6">
          <h3 className="text-lg font-bold text-red-900 mb-2">
            Error
          </h3>
          <p className="text-sm text-red-700">{error.message}</p>
          <p className="text-xs text-red-600 mt-1">Status: {error.status}</p>
        </div>
      )}
    </div>
  );
}
