import { useAuth } from './context/AuthContext';
import { LoginForm } from './components/LoginForm';
import { PaymentForm } from './components/PaymentForm';
import { TransactionList } from './components/TransactionList';
import { TransactionDetails } from './components/TransactionDetails';
import { apiClient } from './services/api';
import { useState } from 'react';

type Tab = 'payment' | 'transactions' | 'details';

function App() {
  const { isAuthenticated, apiKey, logout } = useAuth();
  const [activeTab, setActiveTab] = useState<Tab>('payment');
  const [selectedTransactionId, setSelectedTransactionId] = useState<string | null>(null);

  if (apiKey) {
    apiClient.setApiKey(apiKey);
  } else {
    apiClient.clearApiKey();
  }

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
    <div className="min-h-screen bg-nu-bg">
      <header className="bg-nu-surface shadow-nu-card sticky top-0 z-10">
        <div className="container mx-auto px-4 py-3 flex justify-between items-center">
          <h1 className="text-lg font-bold text-nu-purple tracking-tight">
            Acabou o Mony
          </h1>
          <div className="flex items-center gap-4">
            <span className="text-sm text-nu-text-secondary">
              Admin Dashboard
            </span>
            <button
              onClick={logout}
              className="nu-btn-ghost"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <nav className="bg-nu-surface px-4">
        <div className="container mx-auto">
          <div className="flex gap-8">
            <button
              onClick={() => setActiveTab('payment')}
              className={`py-3 px-1 border-b-2 font-medium text-sm transition-all duration-200 ${
                activeTab === 'payment'
                  ? 'border-nu-purple text-nu-purple'
                  : 'border-transparent text-nu-text-muted hover:text-nu-text-secondary'
              }`}
            >
              Create Payment
            </button>
            <button
              onClick={() => setActiveTab('transactions')}
              className={`py-3 px-1 border-b-2 font-medium text-sm transition-all duration-200 ${
                activeTab === 'transactions' || activeTab === 'details'
                  ? 'border-nu-purple text-nu-purple'
                  : 'border-transparent text-nu-text-muted hover:text-nu-text-secondary'
              }`}
            >
              Transactions
            </button>
          </div>
        </div>
      </nav>

      <main className="container mx-auto px-4 py-8 max-w-4xl">
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
