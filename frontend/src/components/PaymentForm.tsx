import { useState, useEffect } from 'react';
import type { FormEvent } from 'react';
import { useAuth } from '../context/AuthContext';
import { apiClient } from '../services/api';
import type { PaymentResponse, ApiError } from '../types/payment';
import { generateIdempotencyKey } from '../utils/uuid';
import { DevToolsWarning } from './DevToolsWarning';

export function PaymentForm() {
  const { merchantId } = useAuth();
  const [amount, setAmount] = useState('10000');
  const [currency, setCurrency] = useState('BRL');
  const [cardToken, setCardToken] = useState('tok_visa_approved');
  const [customerId, setCustomerId] = useState('');
  const [customerEmail, setCustomerEmail] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<PaymentResponse | null>(null);

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
    setResponse(null);

    try {
      const result = await apiClient.createPayment(buildRequest());
      setResponse(result);
    } catch (err) {
      setError((err as ApiError).message);
    } finally {
      setLoading(false);
    }
  };

    const handleGenerateNewKey = () => {
    setIdempotencyKey(generateIdempotencyKey());
    setResponse(null);
    setError(null);
  };

  const handleOpen3DS = (url: string | undefined) => {
    if (url) window.open(url, '_blank');
  };

  const isValid = amount && parseInt(amount) > 0 && cardToken && idempotencyKey;

  return (
    <div className="max-w-2xl mx-auto">
      {/* Warning Banner */}
      <DevToolsWarning />
      
      <h2 className="text-xl font-bold text-nu-text-primary mb-6">Create Payment</h2>

            <form onSubmit={handleSubmit} className="nu-card mb-6">
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div>
            <label className="nu-label">Amount (cents)</label>
            <input
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="10000"
              className="nu-input"
            />
            <p className="text-xs text-nu-text-muted mt-1">
              {parseInt(amount) / 100 || 0} {currency}
            </p>
          </div>

          <div>
            <label className="nu-label">Currency</label>
            <select
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
              className="nu-input"
            >
              <option value="BRL">BRL</option>
              <option value="USD">USD</option>
            </select>
          </div>
        </div>

        <div className="mb-4">
          <label className="nu-label">Card Token</label>
          <input
            type="text"
            value={cardToken}
            onChange={(e) => setCardToken(e.target.value)}
            placeholder="tok_visa_approved"
            className="nu-input"
          />
        </div>

        <div className="mb-4">
          <label className="nu-label">Customer ID (optional)</label>
          <input
            type="text"
            value={customerId}
            onChange={(e) => setCustomerId(e.target.value)}
            placeholder="cust_123"
            className="nu-input"
          />
        </div>

        <div className="mb-4">
          <label className="nu-label">Customer Email (optional)</label>
          <input
            type="email"
            value={customerEmail}
            onChange={(e) => setCustomerEmail(e.target.value)}
            placeholder="customer@example.com"
            className="nu-input"
          />
        </div>

        <div className="mb-4">
          <label className="nu-label">Idempotency Key</label>
          <div className="flex gap-2">
            <input
              type="text"
              value={idempotencyKey}
              readOnly
              className="nu-input font-mono bg-gray-50"
            />
            <button
              type="button"
              onClick={handleGenerateNewKey}
              className="px-4 py-2.5 rounded-full bg-nu-purple-light text-nu-purple text-sm font-medium hover:bg-[#E4D5F5] transition-colors whitespace-nowrap"
            >
              New Key
            </button>
          </div>
        </div>

        <button
          type="submit"
          disabled={!isValid || loading}
          className={`nu-btn-primary ${
            !isValid || loading ? 'disabled' : ''
          }`}
        >
          {loading ? 'Processing...' : 'Submit Payment'}
        </button>
      </form>

            {error && (
        <div className="mb-6 bg-red-50 border border-nu-error/20 rounded-xl p-4">
          <p className="text-sm text-nu-error">{error}</p>
        </div>
      )}

      {response && (
        <div className="nu-card border border-nu-success/20">
          <h3 className="text-lg font-bold text-nu-success mb-4">Payment Response</h3>
          <div className="space-y-2 text-sm text-nu-text-primary">
            <p>
              <span className="text-nu-text-secondary">Transaction ID:</span>{' '}
              <code className="bg-emerald-50 px-2 py-0.5 rounded font-mono text-xs">{response.transaction_id}</code>
            </p>
            <p>
              <span className="text-nu-text-secondary">Status:</span>{' '}
              <span className={
                response.status === 'COMPLETED' ? 'nu-badge-success' :
                response.status === 'CHALLENGE_PENDING' ? 'nu-badge-warning' :
                'nu-badge-neutral'
              }>
                {response.status}
              </span>
            </p>
            <p>
              <span className="text-nu-text-secondary">Amount:</span>{' '}
              <span className="font-semibold">{response.amount / 100} {response.currency}</span>
            </p>
            {response.challenge_id && (
              <>
                <p>
                  <span className="text-nu-text-secondary">Challenge ID:</span>{' '}
                  <code className="bg-orange-50 px-2 py-0.5 rounded font-mono text-xs">{response.challenge_id}</code>
                </p>
                <button
                  onClick={() => handleOpen3DS(response.acs_url)}
                  className="mt-3 w-full py-3 px-6 rounded-full bg-nu-warning text-white font-semibold text-sm hover:brightness-110 transition-all"
                >
                  Open 3DS Challenge
                </button>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
