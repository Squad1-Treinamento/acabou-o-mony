import Link from "next/link";
import { ShieldCheck, BarChart3 } from "lucide-react";

export default function Home() {
  return (
    <div className="min-h-screen bg-primary text-white flex flex-col">
      {/* Header */}
      <header className="border-b border-white/10">
        <div className="max-w-5xl mx-auto px-6 h-14 flex items-center">
          <Link href="/" className="font-semibold text-base tracking-tight hover:text-white/80 transition-colors">Acabou o Mony</Link>
        </div>
      </header>

      {/* Hero */}
      <main className="flex-1 flex items-center justify-center px-6 py-20">
        <div className="max-w-xl text-center space-y-8">
          <div className="space-y-4">
            <h1 className="text-5xl font-bold tracking-tight leading-tight">
              Pagamentos simples,<br />seguros e rápidos.
            </h1>
            <p className="text-white/60 text-lg leading-relaxed">
              Plataforma de pagamentos para merchants e consumidores.
            </p>
          </div>

          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Link
              href="/checkout"
              className="inline-flex items-center justify-center gap-2 h-[52px] px-7 rounded-[10px] bg-white text-primary font-semibold hover:bg-white/90 transition-colors"
            >
              <ShieldCheck className="w-4 h-4" />
              Ir para o Checkout
            </Link>
            <Link
              href="/login"
              className="inline-flex items-center justify-center gap-2 h-[52px] px-7 rounded-[10px] border border-white/20 text-white font-semibold hover:bg-white/10 transition-colors"
            >
              <BarChart3 className="w-4 h-4" />
              Painel do Merchant
            </Link>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-white/10 px-6 py-4">
        <div className="max-w-5xl mx-auto flex items-center justify-between text-xs text-white/30">
          <span>Acabou o Mony © 2025</span>
          <span>PCI DSS · TLS 1.3 · AES-256</span>
        </div>
      </footer>
    </div>
  );
}
