import Link from "next/link";
import { ShieldCheck, BarChart3 } from "lucide-react";

export default function Home() {
  return (
    <div className="relative min-h-screen bg-[#1A4D35] text-white flex flex-col overflow-hidden">

      {/* Camada de fundo — grid + orbs abaixo de tudo */}
      <div className="pointer-events-none absolute inset-0" style={{ zIndex: 0 }}>

        {/* Grid sutil */}
        <div
          aria-hidden
          className="absolute inset-0 opacity-20"
          style={{
            backgroundImage:
              "linear-gradient(rgba(255,255,255,0.08) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.08) 1px, transparent 1px)",
            backgroundSize: "52px 52px",
          }}
        />

        {/* Orb — branco, top-left */}
        <div
          aria-hidden
          className="absolute rounded-full blur-3xl"
          style={{
            width: 700,
            height: 700,
            top: "-20%",
            left: "-15%",
            background: "radial-gradient(circle, #ffffff 0%, transparent 65%)",
            opacity: 0.22,
            animation: "float-a 13s ease-in-out infinite",
          }}
        />

        {/* Orb — branco, bottom-right */}
        <div
          aria-hidden
          className="absolute rounded-full blur-3xl"
          style={{
            width: 580,
            height: 580,
            bottom: "-18%",
            right: "-12%",
            background: "radial-gradient(circle, #ffffff 0%, transparent 65%)",
            opacity: 0.18,
            animation: "float-b 10s ease-in-out infinite",
          }}
        />

        {/* Orb — branco, centro-direita */}
        <div
          aria-hidden
          className="absolute rounded-full blur-3xl"
          style={{
            width: 420,
            height: 420,
            top: "25%",
            right: "5%",
            background: "radial-gradient(circle, #ffffff 0%, transparent 65%)",
            opacity: 0.14,
            animation: "float-c 16s ease-in-out infinite",
          }}
        />

      </div>

      {/* Header — Apple style */}
      <header
        className="sticky top-0"
        style={{
          zIndex: 50,
          background: "rgba(12, 38, 24, 0.88)",
          backdropFilter: "saturate(180%) blur(20px)",
          WebkitBackdropFilter: "saturate(180%) blur(20px)",
          borderBottom: "1px solid rgba(255,255,255,0.08)",
        }}
      >
        <div className="w-full px-8 h-11 flex items-center justify-between">
          <Link
            href="/"
            className="text-sm font-semibold text-white/90 hover:text-white transition-colors"
          >
            Acabou o Mony
          </Link>
          <nav className="flex items-center gap-7">
            <Link href="/store" className="text-xs text-white/65 hover:text-white transition-colors">
              Loja
            </Link>
            <Link href="/login" className="text-xs text-white/65 hover:text-white transition-colors">
              Merchant
            </Link>
          </nav>
        </div>
      </header>

      {/* Hero */}
      <main className="relative flex-1 flex items-center justify-center px-6 py-20" style={{ zIndex: 1 }}>
        <div className="max-w-xl text-center space-y-8">
          <div className="space-y-4">
            <h1 className="text-5xl font-bold tracking-tight leading-tight text-white/95">
              Pagamentos simples,<br />seguros e rápidos.
            </h1>
            <p className="text-white/65 text-lg leading-relaxed">
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
              className="inline-flex items-center justify-center gap-2 h-[52px] px-7 rounded-[10px] border border-white/30 text-white font-semibold hover:bg-white/10 transition-colors"
            >
              <BarChart3 className="w-4 h-4" />
              Painel do Merchant
            </Link>
          </div>
        </div>
      </main>

      {/* Footer — Apple style */}
      <footer
        className="relative py-3"
        style={{
          zIndex: 10,
          background: "rgba(8, 28, 18, 0.88)",
          backdropFilter: "saturate(180%) blur(20px)",
          WebkitBackdropFilter: "saturate(180%) blur(20px)",
          borderTop: "1px solid rgba(255,255,255,0.08)",
        }}
      >
        <div className="w-full px-8 flex items-center justify-between text-xs text-white/40">
          <span>Acabou o Mony © 2025</span>
          <span>PCI DSS · TLS 1.3 · AES-256</span>
        </div>
      </footer>

    </div>
  );
}
