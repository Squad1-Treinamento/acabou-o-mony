import Link from "next/link";
import Image from "next/image";
import { ApiKeyForm } from "@/components/dashboard/ApiKeyForm";
import { ShieldCheck, ArrowLeft, BarChart3, RefreshCw, Webhook } from "lucide-react";

export default function LoginPage() {
  return (
    <div className="min-h-screen flex flex-col md:flex-row">

      {/* Left — brand panel */}
      <aside
        className="md:w-[42%] xl:w-[38%] flex flex-col px-8 py-10 md:sticky md:top-0 md:h-screen"
        style={{ background: "#0D2B1E" }}
      >
        <div className="flex items-center justify-between mb-auto">
          <Link href="/" className="flex items-center gap-2.5 hover:opacity-80 transition-opacity">
            <Image
              src="/acabou-o-mony-logo.png"
              alt="Acabou o Mony"
              width={36}
              height={36}
              className="rounded-full"
              unoptimized
            />
            <span className="text-sm font-semibold text-white/90">Acabou o Mony</span>
          </Link>
          <Link
            href="/"
            className="flex items-center gap-1 text-xs text-white/40 hover:text-white/80 transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            Voltar
          </Link>
        </div>

        <div className="flex-1 flex flex-col justify-center space-y-8 py-10">
          <div className="space-y-3">
            <h1 className="text-3xl font-bold text-white leading-tight">
              Painel de<br />pagamentos
            </h1>
            <p className="text-sm text-white/50 leading-relaxed max-w-xs">
              Visualize transações, acompanhe status e monitore seu volume de vendas em tempo real.
            </p>
          </div>

          <div className="space-y-3">
            {[
              { icon: BarChart3,  text: "Métricas de transações em tempo real" },
              { icon: RefreshCw,  text: "Reconciliação automática de pagamentos" },
              { icon: Webhook,    text: "Histórico de webhooks e audit trail" },
            ].map(({ icon: Icon, text }) => (
              <div key={text} className="flex items-center gap-3">
                <div className="w-7 h-7 rounded-lg bg-white/8 flex items-center justify-center shrink-0">
                  <Icon className="w-3.5 h-3.5 text-white/60" />
                </div>
                <span className="text-xs text-white/50">{text}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-1.5 text-white/20">
          <ShieldCheck className="w-3.5 h-3.5" />
          <span className="text-xs">Conexão segura · TLS 1.3</span>
        </div>
      </aside>

      {/* Right — form */}
      <div className="flex-1 bg-[#F8F9FB] flex items-center justify-center px-8 py-12">
        <div className="w-full max-w-sm">
          <div className="mb-8">
            <h2 className="text-xl font-semibold text-slate-900">Entrar</h2>
            <p className="text-sm text-slate-500 mt-1">
              Use sua chave de API para acessar o painel.
            </p>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <ApiKeyForm />
          </div>

        </div>
      </div>

    </div>
  );
}
