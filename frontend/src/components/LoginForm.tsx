import { useState } from 'react';
import type { FormEvent } from 'react';
import { useAuth } from '../context/AuthContext';

export function LoginForm() {
  const { login } = useAuth();
  const [merchantId, setMerchantId] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (!merchantId.trim()) {
      setError('Merchant ID is required');
      return;
    }

    if (!apiKey.trim()) {
      setError('API Key is required');
      return;
    }

    login(merchantId.trim(), apiKey.trim());
  };

  const isValid = merchantId.trim() !== '' && apiKey.trim() !== '';

  return (
    <div className="min-h-screen bg-ml-bg flex items-center justify-center">
      <div className="bg-ml-surface p-8 rounded-lg shadow-sm w-full max-w-md">
        <h1 className="text-2xl font-bold text-ml-text-primary mb-1">
          Acabou o Mony
        </h1>
        <p className="text-ml-text-secondary text-sm mb-8">
          Payment Gateway — Merchant Login
        </p>

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label
              htmlFor="merchantId"
              className="block text-sm font-medium text-ml-text-secondary mb-1.5"
            >
              Merchant ID
            </label>
            <input
              id="merchantId"
              type="text"
              value={merchantId}
              onChange={(e) => setMerchantId(e.target.value)}
              placeholder="e.g., m_123"
              className="w-full px-3 py-2.5 border border-ml-border rounded-md text-sm text-ml-text-primary placeholder-ml-text-muted focus:outline-none focus:ring-2 focus:ring-ml-blue focus:border-transparent transition-shadow"
            />
          </div>

          <div className="mb-6">
            <label
              htmlFor="apiKey"
              className="block text-sm font-medium text-ml-text-secondary mb-1.5"
            >
              API Key
            </label>
            <input
              id="apiKey"
              type="password"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder="e.g., sk_test_abc123"
              className="w-full px-3 py-2.5 border border-ml-border rounded-md text-sm text-ml-text-primary placeholder-ml-text-muted focus:outline-none focus:ring-2 focus:ring-ml-blue focus:border-transparent transition-shadow"
            />
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-ml-red/20 rounded-md">
              <p className="text-sm text-ml-red">{error}</p>
            </div>
          )}

          <button
            type="submit"
            disabled={!isValid}
            className={`w-full py-2.5 px-4 rounded-md font-semibold text-sm transition-colors ${
              isValid
                ? 'bg-ml-yellow text-ml-text-primary hover:bg-ml-yellow-dark'
                : 'bg-gray-200 text-ml-text-muted cursor-not-allowed'
            }`}
          >
            Login
          </button>
        </form>

        <div className="mt-6 p-4 bg-blue-50 border border-ml-blue/20 rounded-md">
          <p className="text-sm text-ml-blue font-medium mb-2">
            Test Credentials
          </p>
          <p className="text-xs text-ml-blue/70">
            Merchant ID: <code className="bg-ml-blue/10 px-1.5 py-0.5 rounded text-ml-blue font-mono">m_123</code>
          </p>
          <p className="text-xs text-ml-blue/70 mt-1">
            API Key: <code className="bg-ml-blue/10 px-1.5 py-0.5 rounded text-ml-blue font-mono">sk_test_abc123</code>
          </p>
        </div>
      </div>
    </div>
  );
}
