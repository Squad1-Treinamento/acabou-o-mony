# Task 02: Update Navigation Structure

**Status**: Ready to implement  
**Estimated Time**: 1 hour  
**Dependencies**: Task 01 (Dashboard component)

---

## Goal

Reorganize the application navigation to separate **merchant features** (Dashboard, Transactions) from **developer tools** (Create Payment, Idempotency Test). Make the interface feel like a real merchant platform.

---

## Context

Current navigation shows all features at the same level:
```
[Create Payment] [Transactions] [Details] [Idempotency Test]
```

New navigation separates concerns:
```
[🏠 Dashboard] [💳 Transactions]  |  [🧪 Developer Tools ▼]
                                      ├─ Create Payment
                                      └─ Idempotency Test
```

This makes it clear that:
- **Dashboard** and **Transactions** are for merchants (primary use case)
- **Developer Tools** are for testing (secondary use case)

---

## What You're Building

### Desktop Navigation

```
┌─────────────────────────────────────────────────────────┐
│ Acabou o Mony                      Admin Dashboard  [Logout] │
├─────────────────────────────────────────────────────────┤
│ [🏠 Dashboard] [💳 Transactions]    [🧪 Developer Tools ▼] │
└─────────────────────────────────────────────────────────┘

When Developer Tools is clicked:
┌─────────────────────────────────────────────────────────┐
│ [🏠 Dashboard] [💳 Transactions]    [🧪 Developer Tools ▼] │
├─────────────────────────────────────────────────────────┤
│                    [Create Payment] [Idempotency Test]  │
└─────────────────────────────────────────────────────────┘
```

---

## Files to Modify

### 1. `frontend/src/App.tsx`

Update the main App component to support new navigation structure.

#### Changes Required:

1. **Update Tab Types**

```typescript
// OLD
type Tab = 'payment' | 'transactions' | 'details';

// NEW
type Tab = 'dashboard' | 'transactions' | 'details' | 'developer-tools';
type DevToolsTab = 'create-payment' | 'idempotency-test';
```

2. **Add State for Developer Tools**

```typescript
const [activeTab, setActiveTab] = useState<Tab>('dashboard'); // Changed default
const [showDevTools, setShowDevTools] = useState(false);
const [devToolsTab, setDevToolsTab] = useState<DevToolsTab>('create-payment');
```

3. **Update Navigation JSX**

```typescript
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
```

4. **Update Main Content Rendering**

```typescript
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
```

5. **Update Helper Functions**

```typescript
const handleSelectTransaction = (transactionId: string) => {
  setSelectedTransactionId(transactionId);
  setActiveTab('details');
  setShowDevTools(false); // Close dev tools when viewing transaction
};

const handleBackToList = () => {
  setSelectedTransactionId(null);
  setActiveTab('transactions');
};
```

---

### Complete Updated App.tsx

```typescript
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
```

---

## Acceptance Criteria

- [ ] Dashboard is the default tab after login
- [ ] Dashboard tab is visually highlighted when active
- [ ] Transactions tab works as before
- [ ] Developer Tools button is on the right side of navigation
- [ ] Clicking Developer Tools toggles submenu visibility
- [ ] Submenu shows "Create Payment" and "Idempotency Test" options
- [ ] Submenu has slide-down animation
- [ ] Active submenu item is visually highlighted
- [ ] Clicking merchant tabs (Dashboard, Transactions) closes Developer Tools submenu
- [ ] Clicking transaction navigates to details and closes Developer Tools
- [ ] Back button from details returns to Transactions
- [ ] All navigation transitions are smooth
- [ ] No console errors

---

## Testing Checklist

### Manual Testing

1. **Default State**
   - [ ] Dashboard is active on login
   - [ ] Developer Tools is collapsed

2. **Navigation Flow**
   - [ ] Click Dashboard → shows Dashboard
   - [ ] Click Transactions → shows Transactions
   - [ ] Click Developer Tools → expands submenu
   - [ ] Click Create Payment → shows PaymentForm
   - [ ] Click Idempotency Test → shows IdempotencyTest

3. **State Management**
   - [ ] Clicking Dashboard closes Developer Tools
   - [ ] Clicking Transactions closes Developer Tools
   - [ ] Viewing transaction details closes Developer Tools
   - [ ] Back from details returns to Transactions

4. **Visual Feedback**
   - [ ] Active tab is highlighted
   - [ ] Active submenu item is highlighted
   - [ ] Hover states work correctly
   - [ ] Animations are smooth

---

## Validation Commands

```bash
# Type checking
npm run type-check

# Linting
npm run lint

# Development server
npm run dev
```

---

## Notes

- The `animate-fadeIn` class is already defined in `index.css`
- Developer Tools submenu uses conditional rendering (no React Router needed)
- All state is managed in App.tsx (simple, no context needed)
- Navigation is keyboard accessible (buttons are focusable)

---

**Next Task**: `03-developer-tools-warnings.md`
