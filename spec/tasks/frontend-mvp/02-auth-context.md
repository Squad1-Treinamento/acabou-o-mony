# Task 02: Authentication Context

**Status**: ✅ Completed  
**Estimated Time**: 1 hour

## Goal

Create React Context for mock authentication (merchant_id + api_key storage).

## Acceptance Criteria

- [ ] AuthContext created with TypeScript types
- [ ] Context provides login/logout functions
- [ ] Credentials stored in context state
- [ ] isAuthenticated computed property
- [ ] Context provider wraps App

## Implementation Steps

### 1. Create Types

**`src/types/auth.ts`**:
```typescript
export interface AuthContextType {
  merchantId: string | null;
  apiKey: string | null;
  login: (merchantId: string, apiKey: string) => void;
  logout: () => void;
  isAuthenticated: boolean;
}
```

### 2. Create AuthContext

**`src/context/AuthContext.tsx`**:
```typescript
import { createContext, useContext, useState, ReactNode } from 'react';
import { AuthContextType } from '../types/auth';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [merchantId, setMerchantId] = useState<string | null>(null);
  const [apiKey, setApiKey] = useState<string | null>(null);

  const login = (newMerchantId: string, newApiKey: string) => {
    setMerchantId(newMerchantId);
    setApiKey(newApiKey);
  };

  const logout = () => {
    setMerchantId(null);
    setApiKey(null);
  };

  const isAuthenticated = merchantId !== null && apiKey !== null;

  return (
    <AuthContext.Provider value={{ merchantId, apiKey, login, logout, isAuthenticated }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
```

### 3. Wrap App with Provider

**`src/main.tsx`**:
```typescript
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'
import { AuthProvider } from './context/AuthContext.tsx'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </React.StrictMode>,
)
```

### 4. Test in App.tsx

**`src/App.tsx`** (temporary test):
```typescript
import { useAuth } from './context/AuthContext';

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
```

## Validation

```bash
# Type checking
npm run type-check

# Start dev server
npm run dev

# Manual testing:
# 1. Click "Test Login" button
# 2. Should show "Authenticated: Yes" and "Merchant ID: m_123"
# 3. Click "Logout" button
# 4. Should show "Authenticated: No" and "Merchant ID: None"
```

## Files Created

- `src/types/auth.ts`
- `src/context/AuthContext.tsx`

## Files Modified

- `src/main.tsx` (wrapped with AuthProvider)
- `src/App.tsx` (temporary test code)

## Next Task

`03-api-client.md` - Create Axios client with authentication headers
