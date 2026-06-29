import Link from "next/link";
import Image from "next/image";
import {
  ShieldCheck,
  BarChart3,
  Zap,
  RefreshCw,
  Webhook,
  Lock,
  ArrowRight,
  CheckCircle2,
  TrendingUp,
  CreditCard,
} from "lucide-react";
import { CartClearer } from "@/components/shared/CartClearer";
import { DeviceMockups } from "@/components/shared/DeviceMockups";

const FEATURES = [
  { icon: Zap,      title: "Checkout em segundos",     desc: "Fluxo otimizado com tokenização inline. Sem redirecionamentos desnecessários.", accent: "#0D2B1E", bg: "#EAF7F0" },
  { icon: Lock,     title: "3DS 2.x nativo",           desc: "Autenticação forte embutida no fluxo. O banco aprova, o cliente mal percebe.",  accent: "#1D4ED8", bg: "#EFF6FF" },
  { icon: BarChart3,title: "Dashboard em tempo real",  desc: "Transações, receita e webhooks — tudo em um só lugar, sempre atualizado.",      accent: "#7C3AED", bg: "#F5F3FF" },
  { icon: RefreshCw,title: "Reconciliação automática", desc: "Pagamentos UNKNOWN resolvidos por um worker em background, sem intervenção.",    accent: "#B45309", bg: "#FFFBEB" },
  { icon: Webhook,  title: "Webhooks em tempo real",   desc: "Eventos payment.completed, declined e failed via outbox pattern transacional.",  accent: "#BE185D", bg: "#FDF2F8" },
  { icon: ShieldCheck,title:"Alta disponibilidade",    desc: "Redis, circuit breaker e idempotência de duas camadas para zero falha dupla.",  accent: "#065F46", bg: "#ECFDF5" },
];

const STEPS_CONSUMER = [
  { n: "01", title: "Navegue pela loja demo",   desc: "Produtos simulados para testar o fluxo de compra completo." },
  { n: "02", title: "Informe o cartão de teste", desc: "Tokenização segura — nenhum dado real é necessário." },
  { n: "03", title: "Veja o gateway em ação",    desc: "3DS, aprovação e confirmação — exatamente como em produção." },
];

const STEPS_MERCHANT = [
  { n: "01", title: "Acesse o painel",        desc: "Entre com sua chave de API e veja tudo em tempo real." },
  { n: "02", title: "Monitore transações",    desc: "Filtre por status, período e acompanhe a receita." },
  { n: "03", title: "Configure integrações",  desc: "Defina URLs de webhook e receba eventos automaticamente." },
];

const STATS = [
  { value: "< 1s",    label: "Tempo de resposta médio"  },
  { value: "3DS 2.x", label: "Autenticação forte nativa" },
  { value: "AES-256", label: "Criptografia em repouso"   },
  { value: "99.9%",   label: "Disponibilidade garantida" },
];

const TRUST = ["PCI DSS", "TLS 1.3", "AES-256", "3DS 2.x", "Argon2", "Redis Cache"];

const SECURITY_ITEMS = [
  "Tokenização de cartão — sem PAN ou CVV nos logs",
  "Argon2 para hash de chaves de API",
  "Audit trail INSERT-ONLY com checksum SHA-256",
  "3DS 2.x com JWT HS256 e sessão em Redis",
  "Headers de segurança HTTP (CSP, HSTS, X-Frame)",
];

const AUDIT_ROWS = [
  { status: "CRIADO",      actor: "lojista",    color: "#1D4ED8", bg: "#EFF6FF" },
  { status: "VALIDADO",    actor: "sistema",    color: "#7C3AED", bg: "#F5F3FF" },
  { status: "CHALLENGE",   actor: "3ds-engine", color: "#0891B2", bg: "#ECFEFF" },
  { status: "AUTENTICADO", actor: "portador",   color: "#0369A1", bg: "#F0F9FF" },
  { status: "PROCESSANDO", actor: "adquirente", color: "#B45309", bg: "#FFFBEB" },
  { status: "CONCLUÍDO",   actor: "adquirente", color: "#065F46", bg: "#ECFDF5" },
];

export default function Home() {
  return (
    <div className="overflow-x-hidden">
      <CartClearer />

      {/* ══ NAV — branco ══════════════════════════════════════════════════════ */}
      <header className="sticky top-0 z-50" style={{ background: "rgba(13,43,30,0.92)", backdropFilter: "saturate(180%) blur(20px)", WebkitBackdropFilter: "saturate(180%) blur(20px)", borderBottom: "1px solid rgba(255,255,255,0.07)" }}>
        <div className="max-w-6xl mx-auto px-8 h-12 flex items-center justify-between">
          <Link href="/" className="flex items-center gap-2 hover:opacity-85 transition-opacity">
              <Image
                src="/acabou-o-mony-logo.png"
                alt="Acabou o Mony"
                width={28}
                height={28}
                className="rounded-full"
                unoptimized
              />
              <span className="text-sm font-semibold tracking-tight text-white/90">Acabou o Mony</span>
            </Link>
          <nav className="flex items-center gap-6">
            <Link href="/store" className="text-xs text-white/55 hover:text-white transition-colors">Demo</Link>
            <Link href="/login" className="text-xs font-medium px-4 py-1.5 rounded-lg transition-colors hover:bg-white/15" style={{ background: "rgba(255,255,255,0.10)", color: "rgba(255,255,255,0.85)" }}>
              Merchant
            </Link>
          </nav>
        </div>
      </header>

      {/* ══ 1 — HERO — branco ═════════════════════════════════════════════════ */}
      <section className="bg-white py-20 overflow-hidden">
        <div className="max-w-6xl mx-auto px-8">
          <div className="grid md:grid-cols-2 gap-10 items-center">

            {/* Texto */}
            <div>
              <div
                className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full text-xs font-medium mb-7"
                style={{ background: "#EAF7F0", border: "1px solid #BBF7D0", color: "#065F46" }}
              >
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                Ambiente de demonstração — loja simulada incluída
              </div>

              <h1 className="text-5xl md:text-6xl font-bold tracking-tight leading-none mb-5" style={{ letterSpacing: "-0.03em", color: "#0D2B1E" }}>
                Pagamentos que
                <br />
                <span style={{ background: "linear-gradient(135deg, #0D2B1E 0%, #1D4ED8 50%, #7C3AED 100%)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent", backgroundClip: "text" }}>
                  funcionam de verdade.
                </span>
              </h1>

              <p className="text-slate-500 text-base leading-relaxed max-w-md mb-8">
                Gateway de pagamentos completo — com uma loja simulada para testar o checkout de ponta a ponta: 3DS, reconciliação e webhooks em tempo real.
              </p>

              <div className="flex flex-col sm:flex-row gap-3 mb-10">
                <Link
                  href="/store"
                  className="inline-flex items-center justify-center gap-2 h-[52px] px-8 rounded-xl font-semibold text-sm text-white transition-all hover:opacity-90 hover:scale-[1.02]"
                  style={{ background: "#0D2B1E" }}
                >
                  <CreditCard className="w-4 h-4" />
                  Testar o checkout
                  <ArrowRight className="w-4 h-4" />
                </Link>
                <Link
                  href="/login"
                  className="inline-flex items-center justify-center gap-2 h-[52px] px-8 rounded-xl font-semibold text-sm transition-all hover:bg-slate-50"
                  style={{ border: "1px solid #D1D5DB", color: "#374151" }}
                >
                  <BarChart3 className="w-4 h-4" />
                  Acessar o painel
                </Link>
              </div>

              <div className="flex flex-wrap gap-x-5 gap-y-2">
                {TRUST.map((t) => (
                  <span key={t} className="flex items-center gap-1.5 text-xs text-slate-400">
                    <CheckCircle2 className="w-3 h-3 text-emerald-500" />
                    {t}
                  </span>
                ))}
              </div>
            </div>

            {/* Mockups — oculto no mobile */}
            <div className="hidden md:flex justify-end items-center">
              <DeviceMockups />
            </div>

          </div>
        </div>
      </section>

      {/* ══ 2 — STATS — verde escuro ══════════════════════════════════════════ */}
      <section className="relative overflow-hidden py-16" style={{ background: "#0D2B1E" }}>
        <div className="pointer-events-none absolute inset-0 opacity-10" style={{ backgroundImage: "linear-gradient(rgba(255,255,255,0.06) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.06) 1px, transparent 1px)", backgroundSize: "60px 60px" }} />
        <div className="relative max-w-6xl mx-auto px-8 grid grid-cols-2 md:grid-cols-4 gap-8">
          {STATS.map(({ value, label }) => (
            <div key={label} className="text-center space-y-1.5">
              <p className="text-3xl font-bold tabular-nums tracking-tight text-white/95">{value}</p>
              <p className="text-xs text-white/35">{label}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ══ 3 — FEATURES — branco/cinza ═══════════════════════════════════════ */}
      <section className="bg-[#F8F9FB] py-20">
        <div className="max-w-6xl mx-auto px-8">
          <div className="text-center mb-12">
            <p className="text-xs font-semibold uppercase tracking-widest mb-3" style={{ color: "#0D2B1E" }}>Funcionalidades</p>
            <h2 className="text-3xl md:text-4xl font-bold tracking-tight" style={{ color: "#0D2B1E" }}>
              Tudo que um pagamento precisa ter
            </h2>
            <p className="text-slate-500 text-sm mt-3 max-w-md mx-auto">
              Infraestrutura robusta para merchants modernos — sem complexidade desnecessária.
            </p>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {FEATURES.map(({ icon: Icon, title, desc, accent, bg }) => (
              <div key={title} className="bg-white rounded-2xl p-6 border border-slate-100 flex flex-col gap-4 hover:-translate-y-1 transition-transform duration-200">
                <div className="w-10 h-10 rounded-xl flex items-center justify-center shrink-0" style={{ background: bg }}>
                  <Icon className="w-5 h-5" style={{ color: accent }} />
                </div>
                <div className="space-y-1.5">
                  <p className="text-sm font-semibold text-slate-900">{title}</p>
                  <p className="text-xs text-slate-500 leading-relaxed">{desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ══ 4 — COMO FUNCIONA — verde escuro ══════════════════════════════════ */}
      <section className="relative overflow-hidden py-20" style={{ background: "#0D2B1E" }}>
        <div className="pointer-events-none absolute inset-0">
          <div className="absolute rounded-full blur-3xl" style={{ width: 700, height: 700, top: "-20%", left: "-15%", background: "radial-gradient(circle, rgba(74,222,128,0.10) 0%, transparent 65%)", animation: "float-a 14s ease-in-out infinite" }} />
          <div className="absolute inset-0 opacity-10" style={{ backgroundImage: "linear-gradient(rgba(255,255,255,0.05) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.05) 1px, transparent 1px)", backgroundSize: "60px 60px" }} />
        </div>
        <div className="relative max-w-6xl mx-auto px-8">
          <div className="text-center mb-14">
            <p className="text-xs font-semibold text-emerald-400/70 uppercase tracking-widest mb-3">Como funciona</p>
            <h2 className="text-3xl md:text-4xl font-bold text-white/95 tracking-tight">
              Simples para quem usa, poderoso por baixo
            </h2>
          </div>
          <div className="grid md:grid-cols-2 gap-8">
            {[
              { icon: CreditCard, color: "text-emerald-400", bg: "rgba(74,222,128,0.15)", label: "Loja simulada", sub: "Ambiente de demonstração do checkout", steps: STEPS_CONSUMER, link: "/store", linkLabel: "Abrir a demo", linkColor: "text-emerald-400 hover:text-emerald-300" },
              { icon: TrendingUp, color: "text-blue-400",    bg: "rgba(96,165,250,0.15)", label: "Para merchants",    sub: "Gestão e visibilidade total",   steps: STEPS_MERCHANT, link: "/login",  linkLabel: "Acessar o painel", linkColor: "text-blue-400 hover:text-blue-300" },
            ].map(({ icon: Icon, color, bg, label, sub, steps, link, linkLabel, linkColor }) => (
              <div key={label} className="rounded-2xl p-8" style={{ background: "rgba(255,255,255,0.05)", border: "1px solid rgba(255,255,255,0.09)" }}>
                <div className="flex items-center gap-3 mb-7">
                  <div className="w-9 h-9 rounded-xl flex items-center justify-center" style={{ background: bg }}>
                    <Icon className={`w-4 h-4 ${color}`} />
                  </div>
                  <div>
                    <p className="text-sm font-semibold text-white/90">{label}</p>
                    <p className="text-xs text-white/35">{sub}</p>
                  </div>
                </div>
                <div className="space-y-5">
                  {steps.map(({ n, title, desc }) => (
                    <div key={n} className="flex gap-4">
                      <div className="text-xs font-mono font-bold shrink-0 w-8 h-8 rounded-lg flex items-center justify-center mt-0.5" style={{ background: "rgba(255,255,255,0.07)", color: "rgba(255,255,255,0.35)" }}>{n}</div>
                      <div>
                        <p className="text-sm font-semibold text-white/85">{title}</p>
                        <p className="text-xs text-white/40 mt-0.5 leading-relaxed">{desc}</p>
                      </div>
                    </div>
                  ))}
                </div>
                <Link href={link} className={`inline-flex items-center gap-1.5 text-xs font-medium transition-colors mt-7 ${linkColor}`}>
                  {linkLabel} <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ══ 5 — SEGURANÇA — branco ════════════════════════════════════════════ */}
      <section className="bg-white py-20">
        <div className="max-w-6xl mx-auto px-8 grid md:grid-cols-2 gap-14 items-center">
          <div className="space-y-6">
            <p className="text-xs font-semibold uppercase tracking-widest" style={{ color: "#7C3AED" }}>Segurança</p>
            <h2 className="text-3xl md:text-4xl font-bold tracking-tight leading-tight" style={{ color: "#0D2B1E" }}>
              Construído com segurança<br />como primeiro princípio
            </h2>
            <p className="text-slate-500 text-sm leading-relaxed max-w-sm">
              Dados de cartão nunca trafegam pelo servidor — apenas tokens opacos. Chaves protegidas com Argon2 e cache criptografado no Redis.
            </p>
            <div className="space-y-3 pt-2">
              {SECURITY_ITEMS.map((item) => (
                <div key={item} className="flex items-start gap-2.5">
                  <ShieldCheck className="w-4 h-4 mt-0.5 shrink-0" style={{ color: "#0D2B1E" }} />
                  <span className="text-xs text-slate-600">{item}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-2xl p-7 border border-slate-100 bg-[#F8F9FB]">
            <div className="flex items-center gap-1.5 mb-6">
              <div className="w-2.5 h-2.5 rounded-full bg-red-400" />
              <div className="w-2.5 h-2.5 rounded-full bg-yellow-400" />
              <div className="w-2.5 h-2.5 rounded-full bg-emerald-400" />
              <span className="text-xs text-slate-400 ml-2 font-mono">audit_logs</span>
            </div>
            <div className="space-y-2">
              {AUDIT_ROWS.map(({ status, actor, color, bg }, i) => (
                <div key={status} className="flex items-center gap-3 px-3 py-2.5 rounded-lg font-mono text-xs border" style={{ background: bg, borderColor: `${color}20`, opacity: 1 - i * 0.07 }}>
                  <span className="w-1.5 h-1.5 rounded-full shrink-0" style={{ background: color }} />
                  <span className="font-semibold" style={{ color }}>{status}</span>
                  <span className="text-slate-400 ml-auto">{actor}</span>
                  <span className="text-slate-300">sha256·····</span>
                </div>
              ))}
            </div>
            <p className="text-[10px] text-slate-400 pt-4 text-center">
              Cada transição de estado registrada com checksum imutável
            </p>
          </div>
        </div>
      </section>

      {/* ══ 6 — CTA FINAL — verde escuro ══════════════════════════════════════ */}
      <section className="relative overflow-hidden py-24" style={{ background: "#0D2B1E" }}>
        <div className="pointer-events-none absolute inset-0">
          <div className="absolute rounded-full blur-3xl" style={{ width: 600, height: 600, top: "-20%", right: "-10%", background: "radial-gradient(circle, rgba(74,222,128,0.10) 0%, transparent 65%)" }} />
          <div className="absolute rounded-full blur-3xl" style={{ width: 400, height: 400, bottom: "-20%", left: "10%", background: "radial-gradient(circle, rgba(96,165,250,0.07) 0%, transparent 65%)" }} />
        </div>
        <div className="relative max-w-2xl mx-auto px-8 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-white/95 tracking-tight mb-4">Pronto para testar?</h2>
          <p className="text-white/40 text-sm mb-8">
            Use a chave{" "}
            <span className="font-mono text-white/70 bg-white/10 px-1.5 py-0.5 rounded">teste_key</span>
            {" "}no painel ou simule uma compra na loja demo — sem cartão real necessário.
          </p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Link href="/store" className="inline-flex items-center justify-center gap-2 h-12 px-8 rounded-xl font-semibold text-sm transition-all hover:opacity-90 hover:scale-[1.02]" style={{ background: "#4ADE80", color: "#0D2B1E" }}>
              <CreditCard className="w-4 h-4" />
              Simular uma compra
            </Link>
            <Link href="/login" className="inline-flex items-center justify-center gap-2 h-12 px-8 rounded-xl font-semibold text-sm transition-all hover:bg-white/10" style={{ border: "1px solid rgba(255,255,255,0.18)", color: "rgba(255,255,255,0.85)" }}>
              <BarChart3 className="w-4 h-4" />
              Acessar o painel
            </Link>
          </div>
        </div>
      </section>

      {/* ══ FOOTER — branco ═══════════════════════════════════════════════════ */}
      <footer className="bg-white border-t border-slate-100">
        <div className="max-w-6xl mx-auto px-8 py-8 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <Image
              src="/acabou-o-mony-logo.png"
              alt="Acabou o Mony"
              width={32}
              height={32}
              className="rounded-full"
              unoptimized
            />
            <div className="space-y-0.5 text-center md:text-left">
              <p className="text-sm font-semibold text-slate-900">Acabou o Mony</p>
              <p className="text-xs text-slate-400">Plataforma de pagamentos — ambiente de demonstração</p>
            </div>
          </div>
          <div className="flex items-center gap-6">
            <Link href="/store" className="text-xs text-slate-400 hover:text-slate-700 transition-colors">Demo</Link>
            <Link href="/login" className="text-xs text-slate-400 hover:text-slate-700 transition-colors">Merchant</Link>
            <span className="text-xs text-slate-300">PCI DSS · TLS 1.3 · © 2026</span>
          </div>
        </div>
      </footer>

    </div>
  );
}
