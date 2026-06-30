import { useAuth } from './context/AuthContext';
import { LoginForm } from './components/LoginForm';
import { PaymentForm } from './components/PaymentForm';
import { apiClient } from './services/api';
import { useEffect } from 'react';

function App() {
  const { isAuthenticated, apiKey, merchantId, logout } = useAuth();

  useEffect(() => {
    if (apiKey) {
      apiClient.setApiKey(apiKey);
    } else {
      apiClient.clearApiKey();
    }
  }, [apiKey]);

  if (!isAuthenticated) {
    return <LoginForm />;
  }

  return (
    <div className="min-h-screen bg-gray-100">
      <header className="bg-white shadow">
        <div className="container mx-auto px-4 py-4 flex justify-between items-center">
          <h1 className="text-2xl font-bold text-gray-900">
            Acabou o Mony
          </h1>
          <div className="flex items-center gap-4">
            <span className="text-sm text-gray-600">
              Merchant: {merchantId}
            </span>
            <button
              onClick={logout}
              className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <main className="container mx-auto px-4 py-8">
        <PaymentForm />
      </main>
    </div>
  );
}

export default App;
