import { useAuth } from './context/AuthContext';
import { LoginForm } from './components/LoginForm';
import { PaymentForm } from './components/PaymentForm';
import { TransactionList } from './components/TransactionList';
import { TransactionDetails } from './components/TransactionDetails';
import { apiClient } from './services/api';
import { useEffect, useState } from 'react';

type Tab = 'payment' | 'transactions' | 'details';

function App() {
  const { isAuthenticated, apiKey, merchantId, logout } = useAuth();
  const [activeTab, setActiveTab] = useState<Tab>('payment');
  const [selectedTransactionId, setSelectedTransactionId] = useState<string | null>(null);

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

  const handleSelectTransaction = (transactionId: string) => {
    setSelectedTransactionId(transactionId);
    setActiveTab('details');
  };

  const handleBackToList = () => {
    setSelectedTransactionId(null);
    setActiveTab('transactions');
  };

  return (
    <div className="min-h-screen bg-ml-bg">
      <header className="bg-ml-blue shadow-sm">
        <div className="container mx-auto px-4 py-3 flex justify-between items-center">
          <h1 className="text-xl font-bold text-white tracking-tight">
            Acabou o Mony
          </h1>
          <div className="flex items-center gap-4">
            <span className="text-sm text-white/80">
              {merchantId}
            </span>
            <button
              onClick={logout}
              className="px-4 py-1.5 bg-white/15 text-white text-sm rounded-md hover:bg-white/25 transition-colors"
            >
              Sair
            </button>
          </div>
        </div>
      </header>

      <nav className="bg-ml-surface border-b border-ml-border">
        <div className="container mx-auto px-4">
          <div className="flex space-x-8">
            <button
              onClick={() => setActiveTab('payment')}
              className={`py-3 px-1 border-b-2 font-medium text-sm transition-colors ${
                activeTab === 'payment'
                  ? 'border-ml-yellow text-ml-text-primary'
                  : 'border-transparent text-ml-text-muted hover:text-ml-text-secondary'
              }`}
            >
              Create Payment
            </button>
            <button
              onClick={() => setActiveTab('transactions')}
              className={`py-3 px-1 border-b-2 font-medium text-sm transition-colors ${
                activeTab === 'transactions' || activeTab === 'details'
                  ? 'border-ml-yellow text-ml-text-primary'
                  : 'border-transparent text-ml-text-muted hover:text-ml-text-secondary'
              }`}
            >
              Transactions
            </button>
          </div>
        </div>
      </nav>

      <main className="container mx-auto px-4 py-6">
        {activeTab === 'payment' && <PaymentForm />}
        {activeTab === 'transactions' && (
          <TransactionList onSelectTransaction={handleSelectTransaction} />
        )}
        {activeTab === 'details' && selectedTransactionId && (
          <TransactionDetails
            transactionId={selectedTransactionId}
            onBack={handleBackToList}
          />
        )}
      </main>
    </div>
  );
}

export default App;
