import { useAuth } from './context/AuthContext';
import { LoginForm } from './components/LoginForm';
import { Dashboard } from './components/Dashboard';
import { PaymentForm } from './components/PaymentForm';
import { TransactionList } from './components/TransactionList';
import { TransactionDetails } from './components/TransactionDetails';
import { IdempotencyTest } from './components/IdempotencyTest';
import { apiClient } from './services/api';
import { useState } from 'react';

type Tab = 'dashboard' | 'transactions' | 'details' | 'developer-tools';
type DevToolsTab = 'create-payment' | 'idempotency-test';

function App() {
  const { isAuthenticated, apiKey, logout } = useAuth();
  const [activeTab, setActiveTab] = useState<Tab>('dashboard');
  const [showDevTools, setShowDevTools] = useState(false);
  const [devToolsTab, setDevToolsTab] = useState<DevToolsTab>('create-payment');
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
    setShowDevTools(false);
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
          <div className="flex items-center gap-8">
            {/* Merchant Section */}
            <button
              onClick={() => {
                setActiveTab('dashboard');
                setShowDevTools(false);
              }}
              className={`py-3 px-1 border-b-2 font-medium text-sm transition-all duration-200 ${
                activeTab === 'dashboard'
                  ? 'border-nu-purple text-nu-purple'
                  : 'border-transparent text-nu-text-muted hover:text-nu-text-secondary'
              }`}
            >
              🏠 Dashboard
            </button>
            
            <button
              onClick={() => {
                setActiveTab('transactions');
                setShowDevTools(false);
              }}
              className={`py-3 px-1 border-b-2 font-medium text-sm transition-all duration-200 ${
                activeTab === 'transactions' || activeTab === 'details'
                  ? 'border-nu-purple text-nu-purple'
                  : 'border-transparent text-nu-text-muted hover:text-nu-text-secondary'
              }`}
            >
              💳 Transactions
            </button>

            {/* Developer Tools - Right Side */}
            <div className="ml-auto">
              <button
                onClick={() => {
                  setShowDevTools(!showDevTools);
                  if (!showDevTools) {
                    setActiveTab('developer-tools');
                  } else {
                    setActiveTab('dashboard');
                  }
                }}
                className={`py-3 px-4 rounded-full font-medium text-sm transition-all duration-200 ${
                  activeTab === 'developer-tools'
                    ? 'bg-nu-purple-light text-nu-purple'
                    : 'bg-transparent text-nu-text-muted hover:bg-nu-purple-light/50 hover:text-nu-purple'
                }`}
              >
                🧪 Developer Tools {showDevTools ? '▼' : '▶'}
              </button>
            </div>
          </div>

          {/* Developer Tools Submenu */}
          {showDevTools && (
            <div className="flex gap-4 pl-8 py-2 border-t border-nu-border mt-2 animate-fadeIn">
              <button
                onClick={() => setDevToolsTab('create-payment')}
                className={`py-2 px-3 rounded-lg text-sm font-medium transition-all ${
                  devToolsTab === 'create-payment'
                    ? 'bg-nu-purple-light text-nu-purple'
                    : 'text-nu-text-muted hover:bg-nu-purple-light/30 hover:text-nu-purple'
                }`}
              >
                Create Payment
              </button>
              <button
                onClick={() => setDevToolsTab('idempotency-test')}
                className={`py-2 px-3 rounded-lg text-sm font-medium transition-all ${
                  devToolsTab === 'idempotency-test'
                    ? 'bg-nu-purple-light text-nu-purple'
                    : 'text-nu-text-muted hover:bg-nu-purple-light/30 hover:text-nu-purple'
                }`}
              >
                Idempotency Test
              </button>
            </div>
          )}
        </div>
      </nav>

            <main className="container mx-auto px-4 py-8 max-w-6xl">
        {activeTab === 'dashboard' && (
          <Dashboard
            onNavigateToTransactions={() => setActiveTab('transactions')}
            onNavigateToDevTools={() => {
              setActiveTab('developer-tools');
              setShowDevTools(true);
            }}
            onSelectTransaction={handleSelectTransaction}
          />
        )}
        
        {activeTab === 'transactions' && (
          <TransactionList onSelectTransaction={handleSelectTransaction} />
        )}
        
        {activeTab === 'details' && selectedTransactionId && (
          <TransactionDetails
            transactionId={selectedTransactionId}
            onBack={handleBackToList}
          />
        )}
        
        {activeTab === 'developer-tools' && (
          <>
            {devToolsTab === 'create-payment' && <PaymentForm />}
            {devToolsTab === 'idempotency-test' && <IdempotencyTest />}
          </>
        )}
      </main>
    </div>
  );
}

export default App;
