import { Lock, ShieldCheck, Clock, ArrowLeft, Tag, Truck, ChevronRight } from "lucide-react";
import Link from "next/link";

const ITEMS = [
  { name: "Air Jordan 1 Retro High OG", category: "Calçados", emoji: "👟", price: 89990, qty: 1, color: "Vermelho / Branco" },
  { name: "Óculos Aviador Dourado", category: "Acessórios", emoji: "🕶️", price: 24990, qty: 2, color: "Dourado / Verde" },
];

const SUBTOTAL = ITEMS.reduce((s, i) => s + i.price * i.qty, 0);
const DESCONTO = 1000;
const TOTAL = SUBTOTAL - DESCONTO;

function fmt(cents: number) {
  return (cents / 100).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

const STEPS = ["Carrinho", "Entrega", "Pagamento", "Confirmação"];
const ACTIVE_STEP = 2;

const CARD_BG: Record<string, string> = {
  Calçados:   "linear-gradient(135deg,#fff7ed,#fed7aa)",
  Acessórios: "linear-gradient(135deg,#f0fdf4,#bbf7d0)",
};

export default function CheckoutPreview() {
  return (
    <div className="min-h-screen" style={{ background: "#F5F5F7" }}>

      {/* ── Announcement bar ── */}
      <div
        className="text-white text-xs text-center py-2 px-4 tracking-wide font-medium"
        style={{ background: "#0D2B1E" }}
      >
        ✦ Pagamento 100% seguro · Entrega expressa disponível · Devolução grátis em 30 dias ✦
      </div>

      {/* ── Header ── */}
      <header
        className="sticky top-0 z-50 px-6"
        style={{
          background: "rgba(255,255,255,0.88)",
          backdropFilter: "saturate(180%) blur(20px)",
          WebkitBackdropFilter: "saturate(180%) blur(20px)",
          borderBottom: "1px solid rgba(0,0,0,0.07)",
        }}
      >
        <div className="max-w-5xl mx-auto h-16 flex items-center justify-between gap-6">
          {/* Brand */}
          <Link href="/" className="flex items-center gap-2.5 hover:opacity-75 transition-opacity">
            <div
              className="w-7 h-7 rounded-lg flex items-center justify-center text-white text-xs font-black"
              style={{ background: "#0D2B1E" }}
            >
              A
            </div>
            <span className="text-sm font-bold text-slate-900 tracking-tight">Acabou o Mony</span>
          </Link>

          {/* Step indicator */}
          <div className="hidden sm:flex items-center gap-1">
            {STEPS.map((step, i) => (
              <div key={step} className="flex items-center gap-1">
                <div className="flex items-center gap-1.5">
                  <div
                    className="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold"
                    style={
                      i < ACTIVE_STEP
                        ? { background: "#0D2B1E", color: "#fff" }
                        : i === ACTIVE_STEP
                        ? { background: "#0D2B1E", color: "#fff", boxShadow: "0 0 0 2px #0D2B1E, 0 0 0 4px rgba(13,43,30,0.15)" }
                        : { background: "#e5e7eb", color: "#9ca3af" }
                    }
                  >
                    {i < ACTIVE_STEP ? "✓" : i + 1}
                  </div>
                  <span
                    className="text-[11px] font-medium"
                    style={{ color: i <= ACTIVE_STEP ? "#0D2B1E" : "#9ca3af" }}
                  >
                    {step}
                  </span>
                </div>
                {i < STEPS.length - 1 && (
                  <div className="w-6 h-px mx-1" style={{ background: i < ACTIVE_STEP ? "#0D2B1E" : "#d1d5db" }} />
                )}
              </div>
            ))}
          </div>

          {/* Timer */}
          <div className="flex items-center gap-1.5 text-slate-400">
            <Clock className="w-3.5 h-3.5" />
            <span className="text-xs tabular-nums font-medium">4:58</span>
          </div>
        </div>
      </header>

      {/* ── Main content ── */}
      <main className="max-w-5xl mx-auto px-4 py-8 grid grid-cols-1 lg:grid-cols-[1fr_420px] gap-6">

        {/* ── LEFT: Order summary ── */}
        <div className="space-y-4">

          {/* Back link */}
          <Link href="/store/cart" className="inline-flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-800 transition-colors mb-1">
            <ArrowLeft className="w-3.5 h-3.5" />
            Voltar ao carrinho
          </Link>

          {/* Products card */}
          <div className="bg-white rounded-2xl shadow-sm border border-black/[0.04] overflow-hidden">
            <div className="px-6 pt-5 pb-3 flex items-center justify-between">
              <h2 className="text-sm font-semibold text-slate-900">Resumo do pedido</h2>
              <span className="text-xs text-slate-400">{ITEMS.reduce((s, i) => s + i.qty, 0)} itens</span>
            </div>

            <div className="divide-y divide-slate-100">
              {ITEMS.map((item) => (
                <div key={item.name} className="px-6 py-4 flex items-center gap-4">
                  {/* Thumbnail */}
                  <div
                    className="w-14 h-14 rounded-xl shrink-0 flex items-center justify-center text-2xl relative"
                    style={{ background: CARD_BG[item.category] ?? "linear-gradient(135deg,#f1f5f9,#e2e8f0)" }}
                  >
                    {item.emoji}
                    {item.qty > 1 && (
                      <span
                        className="absolute -top-1.5 -right-1.5 text-white text-[9px] font-bold rounded-full w-5 h-5 flex items-center justify-center"
                        style={{ background: "#0D2B1E" }}
                      >
                        {item.qty}
                      </span>
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-slate-900 truncate">{item.name}</p>
                    <p className="text-xs text-slate-400 mt-0.5">{item.color}</p>
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-sm font-semibold text-slate-900">{fmt(item.price * item.qty)}</p>
                    {item.qty > 1 && (
                      <p className="text-[11px] text-slate-400 mt-0.5">{fmt(item.price)} cada</p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Coupon card */}
          <div className="bg-white rounded-2xl shadow-sm border border-black/[0.04] px-6 py-4">
            <div className="flex items-center gap-3">
              <Tag className="w-4 h-4 text-slate-400 shrink-0" />
              <input
                className="flex-1 text-sm text-slate-700 placeholder:text-slate-300 outline-none bg-transparent"
                defaultValue="PROMO10"
                readOnly
              />
              <span
                className="text-xs font-semibold px-3 py-1.5 rounded-lg cursor-pointer"
                style={{ background: "rgba(13,43,30,0.08)", color: "#0D2B1E" }}
              >
                Aplicado ✓
              </span>
            </div>
          </div>

          {/* Totals card */}
          <div className="bg-white rounded-2xl shadow-sm border border-black/[0.04] px-6 py-5 space-y-3">
            <div className="flex justify-between text-sm text-slate-600">
              <span>Subtotal</span>
              <span className="tabular-nums">{fmt(SUBTOTAL)}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-slate-600">Desconto</span>
              <span className="tabular-nums font-medium" style={{ color: "#0D2B1E" }}>− {fmt(DESCONTO)}</span>
            </div>
            <div className="flex justify-between text-sm text-slate-600 items-center">
              <span className="flex items-center gap-1.5">
                <Truck className="w-3.5 h-3.5" />
                Frete
              </span>
              <span className="text-emerald-600 font-medium">Grátis</span>
            </div>
            <div className="border-t border-slate-100 pt-3 flex justify-between items-baseline">
              <span className="text-sm font-semibold text-slate-900">Total</span>
              <div className="text-right">
                <p className="text-2xl font-bold text-slate-900 tabular-nums">{fmt(TOTAL)}</p>
                <p className="text-[11px] text-slate-400 mt-0.5">ou 12× de {fmt(Math.ceil(TOTAL / 12))} sem juros</p>
              </div>
            </div>
          </div>

          {/* Trust strip */}
          <div className="bg-white rounded-2xl shadow-sm border border-black/[0.04] px-6 py-4">
            <div className="grid grid-cols-3 gap-3">
              {[
                { icon: <ShieldCheck className="w-4 h-4" />, label: "Compra protegida" },
                { icon: <Lock className="w-4 h-4" />, label: "Criptografia SSL" },
                { icon: <Truck className="w-4 h-4" />, label: "Entrega garantida" },
              ].map(({ icon, label }) => (
                <div key={label} className="flex flex-col items-center gap-1.5 text-center">
                  <div className="w-8 h-8 rounded-xl flex items-center justify-center" style={{ background: "rgba(13,43,30,0.07)", color: "#0D2B1E" }}>
                    {icon}
                  </div>
                  <span className="text-[10px] font-medium text-slate-500 leading-tight">{label}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ── RIGHT: Payment form ── */}
        <div className="space-y-4">

          {/* Card brands */}
          <div className="bg-white rounded-2xl shadow-sm border border-black/[0.04] px-6 py-4">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Formas aceitas</span>
              <div className="flex items-center gap-1.5">
                {["VISA", "MC", "AMEX", "ELO", "PIX"].map((b) => (
                  <div
                    key={b}
                    className="text-[8px] font-bold px-1.5 py-1 rounded-md border"
                    style={{ borderColor: "rgba(0,0,0,0.1)", color: "#475569", background: "#f8fafc" }}
                  >
                    {b}
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Form card */}
          <div className="bg-white rounded-2xl shadow-sm border border-black/[0.04] px-6 py-6">
            <div className="flex items-center gap-2 mb-6">
              <div className="w-7 h-7 rounded-lg flex items-center justify-center" style={{ background: "rgba(13,43,30,0.08)" }}>
                <Lock className="w-3.5 h-3.5" style={{ color: "#0D2B1E" }} />
              </div>
              <h2 className="text-sm font-semibold text-slate-900">Informações de pagamento</h2>
            </div>

            <div className="space-y-4">

              {/* Card number */}
              <div>
                <label className="text-xs font-medium text-slate-500 mb-1.5 block">Número do cartão</label>
                <div className="relative">
                  <input
                    className="w-full h-11 rounded-xl border border-slate-200 bg-[#F5F5F7] px-3.5 text-sm font-mono tracking-wider text-slate-900 outline-none pr-14"
                    defaultValue="4111 1111 1111 1111"
                    readOnly
                  />
                  <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-[10px] font-bold text-[#1A56DB] bg-blue-50 px-1.5 py-0.5 rounded">VISA</span>
                </div>
              </div>

              {/* Expiry + CVV */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-medium text-slate-500 mb-1.5 block">Validade</label>
                  <input
                    className="w-full h-11 rounded-xl border border-slate-200 bg-[#F5F5F7] px-3.5 text-sm font-mono text-slate-900 outline-none"
                    defaultValue="12/28"
                    readOnly
                  />
                </div>
                <div>
                  <label className="text-xs font-medium text-slate-500 mb-1.5 block">CVV</label>
                  <div className="relative">
                    <input
                      className="w-full h-11 rounded-xl border border-slate-200 bg-[#F5F5F7] px-3.5 pr-9 text-sm font-mono text-slate-900 outline-none"
                      defaultValue="•••"
                      readOnly
                    />
                    <Lock className="absolute right-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-300" />
                  </div>
                </div>
              </div>

              {/* Cardholder */}
              <div>
                <label className="text-xs font-medium text-slate-500 mb-1.5 block">Nome no cartão</label>
                <input
                  className="w-full h-11 rounded-xl border border-slate-200 bg-[#F5F5F7] px-3.5 text-sm uppercase text-slate-900 outline-none"
                  defaultValue="PATRICIA BORGES"
                  readOnly
                />
              </div>

              {/* Email */}
              <div>
                <label className="text-xs font-medium text-slate-500 mb-1.5 block">
                  E-mail <span className="font-normal text-slate-400">(opcional)</span>
                </label>
                <input
                  className="w-full h-11 rounded-xl border border-slate-200 bg-[#F5F5F7] px-3.5 text-sm text-slate-900 outline-none"
                  defaultValue="patricia@email.com"
                  readOnly
                />
              </div>
            </div>

            {/* Installments */}
            <div className="mt-4">
              <label className="text-xs font-medium text-slate-500 mb-1.5 block">Parcelamento</label>
              <div className="w-full h-11 rounded-xl border border-slate-200 bg-[#F5F5F7] px-3.5 flex items-center justify-between text-sm text-slate-700">
                <span>12× de {fmt(Math.ceil(TOTAL / 12))} sem juros</span>
                <ChevronRight className="w-4 h-4 text-slate-400" />
              </div>
            </div>

            {/* CTA */}
            <button
              className="w-full h-12 rounded-xl mt-6 font-semibold text-sm text-white tracking-tight flex items-center justify-center gap-2"
              style={{ background: "linear-gradient(135deg, #0D2B1E 0%, #1a4532 100%)" }}
            >
              <Lock className="w-3.5 h-3.5 opacity-70" />
              Pagar {fmt(TOTAL)}
            </button>

            {/* Trust line */}
            <div className="mt-4 flex items-center justify-center gap-1.5 text-slate-400">
              <ShieldCheck className="w-3 h-3" />
              <span className="text-[11px]">Seus dados estão protegidos com criptografia de 256 bits</span>
            </div>
          </div>

          {/* PCI badge */}
          <div
            className="rounded-2xl px-5 py-3.5 flex items-center gap-3"
            style={{ background: "rgba(13,43,30,0.05)", border: "1px solid rgba(13,43,30,0.08)" }}
          >
            <ShieldCheck className="w-5 h-5 shrink-0" style={{ color: "#0D2B1E" }} />
            <p className="text-[11px] text-slate-600 leading-relaxed">
              Checkout certificado <strong className="text-slate-800">PCI DSS nível 1</strong>. Seus dados de cartão nunca são armazenados.
            </p>
          </div>

        </div>
      </main>

    </div>
  );
}
