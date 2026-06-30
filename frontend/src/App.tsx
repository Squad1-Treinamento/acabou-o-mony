import { useAuth } from './context/AuthContext';
import { LoginForm } from './components/LoginForm';
import { apiClient } from './services/api';
import { useEffect } from 'react';

function App() {
  const { isAuthenticated, apiKey } = useAuth();

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
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-gray-900">
          Dashboard
        </h1>
        <p className="mt-2 text-gray-600">
          You are logged in!
        </p>
      </div>
    </div>
  );
}

export default App;
