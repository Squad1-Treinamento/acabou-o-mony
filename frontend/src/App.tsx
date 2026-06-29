import { useAuth } from './context/AuthContext.tsx';
import { apiClient } from './services/api.ts';
import { useEffect, useState } from 'react';

function App() {
  const { isAuthenticated, merchantId, apiKey, login, logout } = useAuth();
  const [testResult, setTestResult] = useState<string>('');

  useEffect(() => {
    if (apiKey) {
      apiClient.setApiKey(apiKey);
    } else {
      apiClient.clearApiKey();
    }
  }, [apiKey]);

      const testApiCall = async () => {
    if (!merchantId) return;
    
    try {
      // Create a test payment with correct structure
      const payment = await apiClient.createPayment({
        amount: 10000, // $100.00 in cents
        currency: 'USD',
        idempotency_key: crypto.randomUUID(),
        payment_method: {
          card_token_id: 'test_card_token_123',
          masked_card: '411111XXXXXX1111',
        },
      });
      setTestResult(`Success! Payment created: ${payment.transaction_id} - Status: ${payment.status}`);
    } catch (error: any) {
      setTestResult(`Error: ${error.message} (status: ${error.status})`);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100">
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-gray-900">
          Acabou o Mony - Payment Gateway
        </h1>
        
        <div className="mt-4">
          <p>Authenticated: {isAuthenticated ? 'Yes' : 'No'}</p>
          <p>Merchant ID: {merchantId || 'None'}</p>
          
          {!isAuthenticated ? (
            <button
              onClick={() => login('00000000-0000-0000-0000-000000000001', 'teste_key')}
              className="mt-2 px-4 py-2 bg-blue-600 text-white rounded"
            >
              Test Login
            </button>
          ) : (
            <>
              <button
                onClick={logout}
                className="mt-2 px-4 py-2 bg-red-600 text-white rounded mr-2"
              >
                Logout
              </button>
              <button
                onClick={testApiCall}
                className="mt-2 px-4 py-2 bg-green-600 text-white rounded"
              >
                Test API Call
              </button>
            </>
          )}
          
          {testResult && (
            <p className="mt-4 text-sm text-gray-700">{testResult}</p>
          )}
        </div>
      </div>
    </div>
  );
}

export default App;

