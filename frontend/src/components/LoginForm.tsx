import { useAuth } from '../context/AuthContext';

const ADMIN_MERCHANT_ID = '00000000-0000-0000-0000-000000000001';
const ADMIN_API_KEY = 'teste_key';

export function LoginForm() {
  const { login } = useAuth();

  const handleLogin = () => {
    login(ADMIN_MERCHANT_ID, ADMIN_API_KEY);
  };

  return (
    <div className="min-h-screen bg-nu-bg flex items-center justify-center p-4">
      <div className="nu-card w-full max-w-sm text-center animate-fadeIn">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-nu-purple mb-1">
            Acabou o Mony
          </h1>
          <p className="text-sm text-nu-text-secondary">
            Payment Gateway — Admin Panel
          </p>
        </div>

        <button
          onClick={handleLogin}
          className="nu-btn-primary mb-6"
        >
          Enter Admin Dashboard
        </button>

        <div className="p-4 bg-nu-purple-light rounded-xl text-left">
          <p className="text-sm font-semibold text-nu-purple mb-2">
            Using credentials
          </p>
          <p className="text-xs text-nu-purple/70">
            Merchant ID:{' '}
            <code className="bg-white/50 px-1.5 py-0.5 rounded text-nu-purple font-mono">
              {ADMIN_MERCHANT_ID}
            </code>
          </p>
          <p className="text-xs text-nu-purple/70 mt-1">
            API Key:{' '}
            <code className="bg-white/50 px-1.5 py-0.5 rounded text-nu-purple font-mono">
              {ADMIN_API_KEY}
            </code>
          </p>
        </div>
      </div>
    </div>
  );
}
