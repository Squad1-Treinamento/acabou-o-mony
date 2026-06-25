export interface AuthContextType {
  merchantId: string | null;
  apiKey: string | null;
  login: (merchantId: string, apiKey: string) => void;
  logout: () => void;
  isAuthenticated: boolean;
}
