import { useState, useEffect } from 'react';
import type { FormEvent } from 'react';
import { useAuth } from '../context/AuthContext';
import { apiClient } from '../services/api';
import type { PaymentResponseWithHeaders } from '../services/api';
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

  const inputClass = "nu-input";
  const labelClass = "nu-label";

    return (
    <div className={advancedMode ? 'max-w-6xl mx-auto' : 'max-w-2xl mx-auto'}>
      {/* Warning Banner */}
      <DevToolsWarning />
      
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-bold text-nu-text-primary">Create Payment</h2>
        <label className="flex items-center gap-2 text-sm text-nu-text-secondary cursor-pointer select-none">
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
            className="rounded-lg border-nu-border text-nu-purple focus:ring-nu-purple/30"
          />
          Advanced Mode
        </label>
      </div>

      <form onSubmit={handleSubmit} className="nu-card mb-6">
        <div className="grid grid-cols-2 gap-4 mb-4">
          <div>
            <label className={labelClass}>Amount (cents)</label>
            <input
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="10000"
              className={inputClass}
            />
            <p className="text-xs text-nu-text-muted mt-1">
              {parseInt(amount) / 100 || 0} {currency}
            </p>
          </div>

          <div>
            <label className={labelClass}>Currency</label>
            <select
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
              className={inputClass}
            >
              <option value="BRL">BRL</option>
              <option value="USD">USD</option>
            </select>
          </div>
        </div>

        <div className="mb-4">
          <label className={labelClass}>Card Token</label>
          <input
            type="text"
            value={cardToken}
            onChange={(e) => setCardToken(e.target.value)}
            placeholder="tok_visa_approved"
            className={inputClass}
          />
        </div>

        <div className="mb-4">
          <label className={labelClass}>Customer ID (optional)</label>
          <input
            type="text"
            value={customerId}
            onChange={(e) => setCustomerId(e.target.value)}
            placeholder="cust_123"
            className={inputClass}
          />
        </div>

        <div className="mb-4">
          <label className={labelClass}>Customer Email (optional)</label>
          <input
            type="email"
            value={customerEmail}
            onChange={(e) => setCustomerEmail(e.target.value)}
            placeholder="customer@example.com"
            className={inputClass}
          />
        </div>

        <div className="mb-4">
          <label className={labelClass}>Idempotency Key</label>
          <div className="flex gap-2">
            {advancedMode ? (
              <input
                type="text"
                value={idempotencyKey}
                onChange={(e) => setIdempotencyKey(e.target.value)}
                className="nu-input font-mono"
              />
            ) : (
              <input
                type="text"
                value={idempotencyKey}
                readOnly
                className="nu-input font-mono bg-gray-50"
              />
            )}
            <button
              type="button"
              onClick={handleGenerateNewKey}
              className="px-4 py-2.5 rounded-full bg-nu-purple-light text-nu-purple text-sm font-medium hover:bg-[#E4D5F5] transition-colors whitespace-nowrap"
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
              className={`flex-1 py-3 px-6 rounded-full font-semibold text-sm transition-all duration-200 ${
                isValid && !loading
                  ? 'nu-btn-primary'
                  : 'bg-gray-200 text-nu-text-muted cursor-not-allowed'
              }`}
            >
              {loading ? 'Processing...' : '1. Submit Payment'}
            </button>
            <button
              type="button"
              onClick={handleSecondSubmit}
              disabled={!firstResponse || loading}
              className={`flex-1 py-3 px-6 rounded-full font-semibold text-sm transition-all duration-200 ${
                firstResponse && !loading
                  ? 'bg-nu-purple-light text-nu-purple hover:bg-[#E4D5F5]'
                  : 'bg-gray-200 text-nu-text-muted cursor-not-allowed'
              }`}
            >
              {loading ? 'Processing...' : '2. Submit Again'}
            </button>
          </div>
        ) : (
          <button
            type="submit"
            disabled={!isValid || loading}
            className={`nu-btn-primary ${
              !isValid || loading ? 'disabled' : ''
            }`}
          >
            {loading ? 'Processing...' : 'Submit Payment'}
          </button>
        )}
      </form>

      {error && (
        <div className="mb-6 bg-red-50 border border-nu-error/20 rounded-xl p-4">
          <p className="text-sm text-nu-error">{error}</p>
        </div>
      )}

      {!advancedMode && response && (
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

      {advancedMode && (firstResponse || secondResponse) && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h3 className="text-sm font-semibold text-nu-text-primary mb-3 uppercase tracking-wider">First Request</h3>
            {firstResponse && (
              <div className="nu-card">
                <div className="mb-3 pb-3 border-b border-nu-border">
                  <span className="text-xs font-medium text-nu-text-muted">X-Idempotent-Replayed</span>
                  <p className="text-sm font-mono mt-0.5 text-nu-text-primary">{firstResponse.headers.idempotentReplayed || 'false'}</p>
                </div>
                <div className="space-y-1.5 text-sm text-nu-text-primary">
                  <p><span className="text-nu-text-secondary">Transaction ID:</span> <code className="bg-gray-100 px-1.5 py-0.5 rounded text-xs font-mono">{firstResponse.data.transaction_id}</code></p>
                  <p><span className="text-nu-text-secondary">Status:</span> <span className="nu-badge-success">{firstResponse.data.status}</span></p>
                  <p><span className="text-nu-text-secondary">Amount:</span> {firstResponse.data.amount / 100} {firstResponse.data.currency}</p>
                </div>
              </div>
            )}
          </div>

          <div>
            <h3 className="text-sm font-semibold text-nu-text-primary mb-3 uppercase tracking-wider">Second Request</h3>
            {secondResponse ? (
              <div className="nu-card">
                <div className="mb-3 pb-3 border-b border-nu-border">
                  <span className="text-xs font-medium text-nu-text-muted">X-Idempotent-Replayed</span>
                  <p className={`text-sm font-mono mt-0.5 ${secondResponse.headers.idempotentReplayed === 'true' ? 'text-nu-success font-bold' : 'text-nu-error'}`}>
                    {secondResponse.headers.idempotentReplayed || 'false'}
                  </p>
                </div>
                <div className="space-y-1.5 text-sm text-nu-text-primary">
                  <p><span className="text-nu-text-secondary">Transaction ID:</span> <code className="bg-gray-100 px-1.5 py-0.5 rounded text-xs font-mono">{secondResponse.data.transaction_id}</code></p>
                  <p><span className="text-nu-text-secondary">Status:</span> <span className="nu-badge-success">{secondResponse.data.status}</span></p>
                  <p><span className="text-nu-text-secondary">Amount:</span> {secondResponse.data.amount / 100} {secondResponse.data.currency}</p>
                </div>
                {firstResponse && secondResponse.data.transaction_id === firstResponse.data.transaction_id && (
                  <div className="mt-3 pt-3 border-t border-nu-success/20">
                    <p className="text-xs text-nu-success font-medium">✓ IDs match — Idempotency working!</p>
                  </div>
                )}
              </div>
            ) : (
              <div className="bg-gray-50 rounded-xl p-4 text-center">
                <p className="text-sm text-nu-text-muted">Submit again to test idempotency</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
