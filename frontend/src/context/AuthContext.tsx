import { createContext, useContext, useState, type ReactNode } from 'react';
import type { AuthContextType } from '../types/auth.ts';

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
