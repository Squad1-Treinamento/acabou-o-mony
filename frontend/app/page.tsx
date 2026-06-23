import Link from "next/link";
import { ShieldCheck, BarChart3 } from "lucide-react";

export default function Home() {
  return (
    <div className="relative min-h-screen bg-primary text-white flex flex-col overflow-hidden">
      {/* Background: subtle grid */}
      <div
        aria-hidden
        className="animate-grid pointer-events-none absolute inset-0"
        style={{
          backgroundImage:
            "linear-gradient(rgba(255,255,255,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.04) 1px, transparent 1px)",
          backgroundSize: "48px 48px",
        }}
      />

      {/* Background: floating orbs */}
      <div aria-hidden className="pointer-events-none absolute inset-0 overflow-hidden">
        {/* orb A — top-left area */}
        <div
          className="animate-orb-a absolute rounded-full opacity-20 blur-3xl"
          style={{ width: 480, height: 480, top: "-10%", left: "-8%", background: "radial-gradient(circle, #22c55e 0%, transparent 70%)" }}
        />
        {/* orb B — bottom-right area */}
        <div
          className="animate-orb-b absolute rounded-full opacity-15 blur-3xl"
          style={{ width: 520, height: 520, bottom: "-12%", right: "-6%", background: "radial-gradient(circle, #16a34a 0%, transparent 70%)" }}
        />
        {/* orb C — center-right accent */}
        <div
          className="animate-orb-c absolute rounded-full opacity-10 blur-2xl"
          style={{ width: 300, height: 300, top: "30%", right: "15%", background: "radial-gradient(circle, #86efac 0%, transparent 70%)" }}
        />
      </div>

      {/* Header */}
      <header className="relative z-10 bg-primary border-b border-white/10">
        <div className="px-6 h-14 flex items-center">
          <Link href="/" className="font-semibold text-base tracking-tight hover:text-white/80 transition-colors">Acabou o Mony</Link>
        </div>
      </header>

      {/* Hero */}
      <main className="relative flex-1 flex items-center justify-center px-6 py-20">
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
              href="/store"
              className="inline-flex items-center justify-center gap-2 h-[52px] px-7 rounded-[10px] bg-white text-primary font-semibold hover:bg-white/90 transition-colors"
            >
              <ShieldCheck className="w-4 h-4" />
              Ver produtos
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
      <footer className="relative z-10 bg-primary border-t border-white/10 px-6 py-4">
        <div className="flex items-center justify-between text-xs text-white/30">
          <span>Acabou o Mony © 2025</span>
          <span>PCI DSS · TLS 1.3 · AES-256</span>
        </div>
      </footer>
    </div>
  );
}
