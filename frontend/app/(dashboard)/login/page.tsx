import Link from "next/link";
import { ApiKeyForm } from "@/components/dashboard/ApiKeyForm";
import { ShieldCheck, ArrowLeft } from "lucide-react";

export default function LoginPage() {
  return (
    <div className="min-h-screen flex flex-col md:flex-row">
      {/* Left — brand panel */}
      <div className="bg-primary text-white flex flex-col justify-between p-8 md:w-2/5 md:min-h-screen">
        <div className="flex items-center justify-between">
          <Link href="/" className="font-semibold text-lg tracking-tight hover:text-white/80 transition-colors">
            Acabou o Mony
          </Link>
          <Link
            href="/"
            className="flex items-center gap-1.5 text-white/60 hover:text-white transition-colors text-sm"
          >
            <ArrowLeft className="w-4 h-4" />
            Voltar
          </Link>
        </div>

        <div className="space-y-4 py-8 md:py-0">
          <h1 className="text-3xl font-bold leading-tight">
            Painel de<br />pagamentos
          </h1>
          <p className="text-white/70 text-sm leading-relaxed max-w-xs">
            Visualize transações, acompanhe status e monitore seu volume de vendas em tempo real.
          </p>
        </div>

        <div className="flex items-center gap-2 text-white/50 text-xs">
          <ShieldCheck className="w-4 h-4" />
          <span>Conexão segura · TLS 1.3</span>
        </div>
      </div>

      {/* Right — form */}
      <div className="flex-1 flex items-center justify-center p-8 bg-background">
        <div className="w-full max-w-sm space-y-8">
          <div>
            <h2 className="text-xl font-semibold text-text-primary">Entrar</h2>
            <p className="text-sm text-text-secondary mt-1">
              Use sua chave de API para acessar o painel.
            </p>
          </div>
          <ApiKeyForm />
        </div>
      </div>
    </div>
  );
}
