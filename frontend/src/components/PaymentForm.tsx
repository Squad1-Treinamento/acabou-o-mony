import { useState, FormEvent, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { apiClient } from '../services/api';
import type { PaymentResponseWithHeaders } from '../services/api';
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
  const [advancedMode, setAdvancedMode] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<PaymentResponse | null>(null);
  const [firstResponse, setFirstResponse] = useState<PaymentResponseWithHeaders | null>(null);
  const [secondResponse, setSecondResponse] = useState<PaymentResponseWithHeaders | null>(null);

  useEffect(() => {
    setIdempotencyKey(generateIdempotencyKey());
  }, []);

  const buildRequest = () => ({
    amount: parseInt(amount),
    currency,
    payment_method: { card_token_id: cardToken },
    customer_id: customerId || undefined,
    customer_email: customerEmail || undefined,
    idempotency_key: idempotencyKey,
  });

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!merchantId) return;

    setLoading(true);
    setError(null);

    if (advancedMode) {
      setFirstResponse(null);
      setSecondResponse(null);
      try {
        const result = await apiClient.createPaymentWithHeaders(buildRequest());
        setFirstResponse(result);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Request failed');
      } finally {
        setLoading(false);
      }
    } else {
      setResponse(null);
      try {
        const result = await apiClient.createPayment(buildRequest());
        setResponse(result);
      } catch (err) {
        setError((err as ApiError).message);
      } finally {
        setLoading(false);
      }
    }
  };

  const handleSecondSubmit = async () => {
    if (!firstResponse || !merchantId) return;
    setLoading(true);
    setError(null);

    try {
      const result = await apiClient.createPaymentWithHeaders(buildRequest());
      setSecondResponse(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Request failed');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateNewKey = () => {
    setIdempotencyKey(generateIdempotencyKey());
    setResponse(null);
    setFirstResponse(null);
    setSecondResponse(null);
    setError(null);
  };

  const handleOpen3DS = (url: string | undefined) => {
    if (url) window.open(url, '_blank');
  };

  const isValid = amount && parseInt(amount) > 0 && cardToken && idempotencyKey;

  return (
    <div className={advancedMode ? 'max-w-6xl mx-auto' : 'max-w-2xl mx-auto'}>
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-900">Create Payment</h2>
        <label className="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
          <input
            type="checkbox"
            checked={advancedMode}
            onChange={(e) => {
              setAdvancedMode(e.target.checked);
              setResponse(null);
              setFirstResponse(null);
              setSecondResponse(null);
              setError(null);
            }}
            className="rounded border-gray-300"
          />
          Advanced Mode (test idempotency)
        </label>
      </div>

      <form onSubmit={handleSubmit} className="bg-white p-6 rounded-lg shadow-md mb-6">
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
            {advancedMode ? (
              <input
                type="text"
                value={idempotencyKey}
                onChange={(e) => setIdempotencyKey(e.target.value)}
                className="flex-1 px-3 py-2 border border-gray-300 rounded-md font-mono text-sm"
              />
            ) : (
              <input
                type="text"
                value={idempotencyKey}
                readOnly
                className="flex-1 px-3 py-2 border border-gray-300 rounded-md bg-gray-50 font-mono text-sm"
              />
            )}
            <button
              type="button"
              onClick={handleGenerateNewKey}
              className="px-4 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700 whitespace-nowrap"
            >
              New Key
            </button>
          </div>
        </div>

        {advancedMode ? (
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
          </div>
        ) : (
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
        )}
      </form>

      {error && !advancedMode && (
        <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-6">
          <h3 className="text-lg font-bold text-red-900 mb-2">Error</h3>
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {error && advancedMode && (
        <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {!advancedMode && response && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-6">
          <h3 className="text-lg font-bold text-green-900 mb-4">Payment Response</h3>
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
                <p><span className="font-medium">Challenge ID:</span> <code className="bg-yellow-100 px-2 py-1 rounded">{response.challenge_id}</code></p>
                <button
                  onClick={() => handleOpen3DS(response.acs_url)}
                  className="mt-4 w-full py-2 px-4 bg-yellow-600 text-white rounded-md hover:bg-yellow-700"
                >
                  Open 3DS Challenge
                </button>
              </>
            )}
          </div>
        </div>
      )}

      {advancedMode && (firstResponse || secondResponse) && (
        <div className="grid grid-cols-2 gap-6">
          <div>
            <h3 className="text-lg font-bold text-gray-900 mb-4">First Request</h3>
            {firstResponse && (
              <div className="bg-white border border-gray-200 rounded-lg p-4">
                <div className="mb-4">
                  <span className="text-xs font-medium text-gray-500">X-Idempotent-Replayed</span>
                  <p className="text-sm font-mono mt-1">{firstResponse.headers.idempotentReplayed || 'false'}</p>
                </div>
                <div className="space-y-2 text-sm">
                  <p><span className="font-medium">Transaction ID:</span> <code className="bg-gray-100 px-2 py-1 rounded text-xs">{firstResponse.data.transaction_id}</code></p>
                  <p><span className="font-medium">Status:</span> <span className="px-2 py-1 bg-green-100 text-green-800 rounded text-xs">{firstResponse.data.status}</span></p>
                  <p><span className="font-medium">Amount:</span> {firstResponse.data.amount / 100} {firstResponse.data.currency}</p>
                </div>
              </div>
            )}
          </div>

          <div>
            <h3 className="text-lg font-bold text-gray-900 mb-4">Second Request (Duplicate)</h3>
            {secondResponse ? (
              <div className="bg-white border border-gray-200 rounded-lg p-4">
                <div className="mb-4">
                  <span className="text-xs font-medium text-gray-500">X-Idempotent-Replayed</span>
                  <p className={`text-sm font-mono mt-1 ${secondResponse.headers.idempotentReplayed === 'true' ? 'text-green-600 font-bold' : 'text-red-600'}`}>
                    {secondResponse.headers.idempotentReplayed || 'false'}
                  </p>
                </div>
                <div className="space-y-2 text-sm">
                  <p><span className="font-medium">Transaction ID:</span> <code className="bg-gray-100 px-2 py-1 rounded text-xs">{secondResponse.data.transaction_id}</code></p>
                  <p><span className="font-medium">Status:</span> <span className="px-2 py-1 bg-green-100 text-green-800 rounded text-xs">{secondResponse.data.status}</span></p>
                  <p><span className="font-medium">Amount:</span> {secondResponse.data.amount / 100} {secondResponse.data.currency}</p>
                </div>
                {firstResponse && secondResponse.data.transaction_id === firstResponse.data.transaction_id && (
                  <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded">
                    <p className="text-xs text-green-800 font-medium">✓ Transaction IDs match - Idempotency working correctly!</p>
                  </div>
                )}
              </div>
            ) : (
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 text-center">
                <p className="text-sm text-gray-500">Click "Submit Again" to test idempotency</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
