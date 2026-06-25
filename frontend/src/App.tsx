import { useAuth } from './context/AuthContext.tsx';

function App() {
  const { isAuthenticated, merchantId, login, logout } = useAuth();

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
            <button
              onClick={logout}
              className="mt-2 px-4 py-2 bg-red-600 text-white rounded"
            >
              Logout
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export default App;

