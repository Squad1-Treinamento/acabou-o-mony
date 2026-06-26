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
      const transactions = await apiClient.getTransactions(merchantId);
      setTestResult(`Success! Found ${transactions.length} transactions`);
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
              onClick={() => login('m_123', 'sk_test_abc')}
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

