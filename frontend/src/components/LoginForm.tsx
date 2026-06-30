import { useState, FormEvent } from 'react';
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
    <div className="min-h-screen bg-gray-100 flex items-center justify-center">
      <div className="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">
          Acabou o Mony
        </h1>
        <p className="text-gray-600 mb-6">
          Payment Gateway - Merchant Login
        </p>

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label
              htmlFor="merchantId"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              Merchant ID
            </label>
            <input
              id="merchantId"
              type="text"
              value={merchantId}
              onChange={(e) => setMerchantId(e.target.value)}
              placeholder="e.g., m_123"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div className="mb-6">
            <label
              htmlFor="apiKey"
              className="block text-sm font-medium text-gray-700 mb-2"
            >
              API Key
            </label>
            <input
              id="apiKey"
              type="password"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder="e.g., sk_test_abc123"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-md">
              <p className="text-sm text-red-600">{error}</p>
            </div>
          )}

          <button
            type="submit"
            disabled={!isValid}
            className={`w-full py-2 px-4 rounded-md font-medium ${
              isValid
                ? 'bg-blue-600 text-white hover:bg-blue-700'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            }`}
          >
            Login
          </button>
        </form>

        <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-md">
          <p className="text-sm text-blue-800 font-medium mb-2">
            Test Credentials:
          </p>
          <p className="text-xs text-blue-700">
            Merchant ID: <code className="bg-blue-100 px-1 rounded">m_123</code>
          </p>
          <p className="text-xs text-blue-700">
            API Key: <code className="bg-blue-100 px-1 rounded">sk_test_abc123</code>
          </p>
        </div>
      </div>
    </div>
  );
}
