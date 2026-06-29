"use client";

import { useEffect, useRef, useCallback, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Suspense } from "react";
import { ArrowLeft, ShieldCheck, Clock, Lock, Truck, Tag } from "lucide-react";
import Image from "next/image";
import { CheckoutShell } from "@/components/checkout/CheckoutShell";
import { LoadingSpinner } from "@/components/shared/LoadingSpinner";
import { useCart } from "@/context/CartContext";
import { formatCurrency } from "@/lib/utils/formatters";

const CHECKOUT_SESSION_SECS = 600;

function formatTime(secs: number) {
  const m = Math.floor(secs / 60);
  const s = secs % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

const CATEGORY_GRADIENTS: Record<string, string> = {
  Calçados:    "linear-gradient(135deg,#fff7ed,#fed7aa)",
  Acessórios:  "linear-gradient(135deg,#f0fdf4,#bbf7d0)",
  Eletrônicos: "linear-gradient(135deg,#f1f5f9,#e2e8f0)",
};

const STEPS = ["Carrinho", "Entrega", "Pagamento", "Confirmação"];
const ACTIVE_STEP = 2;

function ProductThumb({ product }: {
  product: { name: string; emoji: string; category: string; image?: string };
}) {
  const [imgError, setImgError] = useState(false);
  const bg = CATEGORY_GRADIENTS[product.category] ?? "linear-gradient(135deg,#f1f5f9,#e2e8f0)";
  return (
    <div
      className="w-14 h-14 rounded-xl shrink-0 flex items-center justify-center text-2xl relative overflow-hidden"
      style={{ background: bg }}
    >
      {product.image && !imgError ? (
        <Image
          src={product.image}
          alt={product.name}
          fill
          className="object-cover"
          onError={() => setImgError(true)}
        />
      ) : (
        <span className="text-xl select-none">{product.emoji}</span>
      )}
    </div>
  );
}

export default function CheckoutPage() {
  const router = useRouter();
  const { total: liveTotal, itemCount, clearCart, items: liveItems } = useCart();
  const paymentCompleteRef = useRef(false);

  const [snapshot] = useState(() => ({ items: liveItems, total: liveTotal }));
  const { items, total } = snapshot;

  const [sessionSecondsLeft, setSessionSecondsLeft] = useState(CHECKOUT_SESSION_SECS);
  const [timerActive, setTimerActive] = useState(false);
  const sessionExpired = sessionSecondsLeft <= 0 && timerActive;

  const [paymentOutcome, setPaymentOutcome] = useState<"success" | "error" | null>(null);

  const [couponInput, setCouponInput] = useState("");
  const [couponError, setCouponError] = useState(false);

  const handleApplyCoupon = useCallback(() => {
    if (couponInput.trim()) setCouponError(true);
  }, [couponInput]);

  useEffect(() => {
    if (!timerActive) return;
    const id = setInterval(() => {
      if (paymentCompleteRef.current) { clearInterval(id); return; }
      setSessionSecondsLeft((s) => {
        if (s <= 1) { clearInterval(id); return 0; }
        return s - 1;
      });
    }, 1000);
    return () => clearInterval(id);
  }, [timerActive]);

  const handlePaymentStarted = useCallback(() => {
    setTimerActive(true);
  }, []);

  useEffect(() => {
    if (itemCount === 0 && !paymentCompleteRef.current) router.replace("/store");
  }, [itemCount, router]);

  const handlePaymentComplete = useCallback(() => {
    paymentCompleteRef.current = true;
    setPaymentOutcome("success");
    setTimerActive(false);
    clearCart();
  }, [clearCart]);

  const handlePaymentFailed = useCallback(() => {
    setPaymentOutcome("error");
    setTimerActive(false);
  }, []);

  const handleRetry = useCallback(() => {
    setPaymentOutcome(null);
    setTimerActive(false);
    setSessionSecondsLeft(CHECKOUT_SESSION_SECS);
  }, []);

  if (itemCount === 0 && !paymentCompleteRef.current) {
    return (
      <div className="min-h-screen flex items-center justify-center" style={{ background: "#F5F5F7" }}>
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  const timerColor =
    sessionSecondsLeft <= 30 ? "text-red-500"
    : sessionSecondsLeft <= 60 ? "text-amber-500"
    : "";

  const timerPulse = sessionSecondsLeft <= 30 ? "animate-pulse" : "";

  return (
    <div className="min-h-screen" style={{ background: "#F5F5F7" }}>

      {/* ── Brand stripe ── */}
      <div style={{ height: 4, background: "linear-gradient(90deg, #0D2B1E 0%, #1a4532 60%, #2d6a4f 100%)" }} />

      {/* ── Header ── */}
      <header
        className="sticky top-0 z-50 px-6"
        style={{
          background: "rgba(255,255,255,0.92)",
          backdropFilter: "saturate(180%) blur(20px)",
          WebkitBackdropFilter: "saturate(180%) blur(20px)",
          borderBottom: "1px solid rgba(13,43,30,0.1)",
        }}
      >
        <div className="max-w-5xl mx-auto h-16 flex items-center relative">
          {/* Brand — left */}
          <Link href="/" className="flex items-center gap-2.5 hover:opacity-75 transition-opacity shrink-0">
            <div
              className="w-7 h-7 rounded-lg flex items-center justify-center text-white text-xs font-black"
              style={{ background: "#0D2B1E" }}
            >
              A
            </div>
            <span className="text-sm font-bold text-slate-900 tracking-tight">Acabou o Mony</span>
          </Link>

          {/* Step indicator — absolutely centered */}
          <div className="hidden sm:flex items-center gap-1 absolute left-1/2 -translate-x-1/2">
            {STEPS.map((step, i) => {
              const isConfirmation = i === STEPS.length - 1;
              const isSuccess = isConfirmation && paymentOutcome === "success";
              const isError = isConfirmation && paymentOutcome === "error";
              const isDone = i < ACTIVE_STEP || (i === ACTIVE_STEP && paymentOutcome !== null) || isSuccess;
              const isActive = i === ACTIVE_STEP && paymentOutcome === null;

              const bubbleStyle = isError
                ? { background: "#dc2626", color: "#fff" }
                : isDone
                ? { background: "#0D2B1E", color: "#fff" }
                : isActive
                ? { background: "#0D2B1E", color: "#fff", boxShadow: "0 0 0 2px #0D2B1E, 0 0 0 4px rgba(13,43,30,0.15)" }
                : { background: "#e5e7eb", color: "#9ca3af" };

              const labelColor = isError ? "#dc2626" : (isDone || isActive) ? "#0D2B1E" : "#9ca3af";
              const lineColor = i < ACTIVE_STEP ? "#0D2B1E" : "#d1d5db";

              return (
                <div key={step} className="flex items-center gap-1">
                  <div className="flex items-center gap-1.5">
                    <div
                      className="w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold"
                      style={bubbleStyle}
                    >
                      {isError ? "✕" : isDone ? "✓" : i + 1}
                    </div>
                    <span className="text-[11px] font-medium" style={{ color: labelColor }}>
                      {step}
                    </span>
                  </div>
                  {i < STEPS.length - 1 && (
                    <div className="w-6 h-px mx-1" style={{ background: lineColor }} />
                  )}
                </div>
              );
            })}
          </div>

          {/* Session timer — right */}
          <div className="ml-auto shrink-0">
            {timerActive && !sessionExpired && !paymentCompleteRef.current && (
              <div
                className={`flex items-center gap-1.5 tabular-nums text-xs font-medium transition-colors ${timerColor} ${timerPulse}`}
                style={!timerColor ? { color: "#0D2B1E" } : undefined}
              >
                <Clock className="w-3.5 h-3.5" />
                <span>{formatTime(sessionSecondsLeft)}</span>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* ── Main ── */}
      <main className="max-w-5xl mx-auto px-4 py-8 grid grid-cols-1 lg:grid-cols-[1fr_420px] gap-6">

        {/* ── LEFT: Order summary ── */}
        <div className="space-y-4">
          <Link href="/store/cart" className="inline-flex items-center gap-1.5 text-xs font-medium transition-colors" style={{ color: "#0D2B1E" }}>
            <ArrowLeft className="w-3.5 h-3.5" />
            Voltar ao carrinho
          </Link>

          {/* Products card */}
          <div className="bg-white rounded-2xl shadow-sm border border-black/[0.04] overflow-hidden">
            <div className="px-6 pt-5 pb-3 flex items-center justify-between border-b" style={{ borderColor: "rgba(13,43,30,0.07)" }}>
              <div className="flex items-center gap-2">
                <div className="w-1 h-4 rounded-full" style={{ background: "#0D2B1E" }} />
                <h2 className="text-sm font-semibold text-slate-900">Resumo do pedido</h2>
              </div>
              <span className="text-xs font-medium px-2 py-0.5 rounded-full" style={{ background: "rgba(13,43,30,0.07)", color: "#0D2B1E" }}>
                {items.reduce((s, i) => s + i.quantity, 0)} {items.reduce((s, i) => s + i.quantity, 0) === 1 ? "item" : "itens"}
              </span>
            </div>
            <div className="divide-y divide-slate-100">
              {items.map(({ product, quantity }) => (
                <div key={product.id} className="px-6 py-4 flex items-center gap-4">
                  <div className="relative">
                    <ProductThumb product={product} />
                    {quantity > 1 && (
                      <span
                        className="absolute -top-1.5 -right-1.5 text-white text-[9px] font-bold rounded-full w-5 h-5 flex items-center justify-center"
                        style={{ background: "#0D2B1E" }}
                      >
                        {quantity}
                      </span>
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-slate-900 truncate">{product.name}</p>
                    <p className="text-xs text-slate-400 mt-0.5">{product.category}</p>
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-sm font-semibold text-slate-900">{formatCurrency(product.price * quantity, "BRL")}</p>
                    {quantity > 1 && (
                      <p className="text-[11px] text-slate-400 mt-0.5">{formatCurrency(product.price, "BRL")} cada</p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Coupon card */}
          <div
            className="bg-white rounded-2xl shadow-sm px-6 py-4"
            style={{ border: couponError ? "1px solid #fca5a5" : "1px solid rgba(0,0,0,0.04)" }}
          >
            <div className="flex items-center gap-3">
              <Tag className="w-4 h-4 shrink-0" style={{ color: couponError ? "#f87171" : "#cbd5e1" }} />
              <input
                className="flex-1 text-sm placeholder:text-slate-300 outline-none bg-transparent"
                style={{ color: couponError ? "#ef4444" : "#374151" }}
                placeholder="Código de cupom"
                value={couponInput}
                onChange={(e) => { setCouponInput(e.target.value); setCouponError(false); }}
                onKeyDown={(e) => e.key === "Enter" && handleApplyCoupon()}
              />
              <button
                className="text-xs font-semibold px-3 py-1.5 rounded-lg transition-colors"
                style={{ background: "rgba(13,43,30,0.07)", color: "#0D2B1E" }}
                onClick={handleApplyCoupon}
              >
                Aplicar
              </button>
            </div>
            {couponError && (
              <p className="mt-2 text-xs text-red-500 flex items-center gap-1">
                <span>✕</span> Cupom não ativo ou inválido
              </p>
            )}
          </div>

          {/* Totals card */}
          <div className="bg-white rounded-2xl shadow-sm border border-black/[0.04] px-6 py-5 space-y-3">
            <div className="flex justify-between text-sm text-slate-600">
              <span>Subtotal</span>
              <span className="tabular-nums">{formatCurrency(total, "BRL")}</span>
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
                <p className="text-2xl font-bold tabular-nums" style={{ color: "#0D2B1E" }}>{formatCurrency(total, "BRL")}</p>
                <p className="text-[11px] mt-0.5" style={{ color: "rgba(13,43,30,0.5)" }}>
                  ou 12× de {formatCurrency(Math.ceil(total / 12), "BRL")} sem juros
                </p>
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
                  <div
                    className="w-8 h-8 rounded-xl flex items-center justify-center"
                    style={{ background: "rgba(13,43,30,0.07)", color: "#0D2B1E" }}
                  >
                    {icon}
                  </div>
                  <span className="text-[10px] font-medium text-slate-500 leading-tight">{label}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ── RIGHT: Payment steps ── */}
        <div className="space-y-4">

          {sessionExpired ? (
            /* Session expired */
            <div className="bg-white rounded-2xl shadow-sm border border-black/[0.04] px-6 py-12 flex flex-col items-center text-center">
              <div className="w-14 h-14 rounded-full bg-red-50 flex items-center justify-center mb-5">
                <Clock className="w-7 h-7 text-red-400" />
              </div>
              <h2 className="text-lg font-semibold text-slate-900 mb-2">Sessão expirada</h2>
              <p className="text-sm text-slate-500 leading-relaxed max-w-xs mb-6">
                O tempo para concluir o pagamento esgotou. Por segurança, a sessão foi encerrada automaticamente.
              </p>
              <Link
                href="/store/cart"
                className="inline-flex items-center gap-2 rounded-xl px-6 h-12 text-sm font-semibold text-white transition-opacity hover:opacity-85"
                style={{ background: "#0D2B1E" }}
              >
                Voltar ao carrinho
              </Link>
            </div>
          ) : (
            <div className="bg-white rounded-2xl shadow-sm border border-black/[0.04] px-6 py-6">
              <Suspense
                fallback={
                  <div className="flex justify-center py-16">
                    <LoadingSpinner size="lg" />
                  </div>
                }
              >
                <CheckoutShell
                  amount={total}
                  currency="BRL"
                  onComplete={handlePaymentComplete}
                  onPaymentStarted={handlePaymentStarted}
                  onPaymentFailed={handlePaymentFailed}
                  onRetry={handleRetry}
                />
              </Suspense>
            </div>
          )}

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
