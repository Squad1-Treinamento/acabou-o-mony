"use client";

import { useEffect, useRef, useCallback } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Suspense } from "react";
import { ShieldCheck } from "lucide-react";
import { CheckoutShell } from "@/components/checkout/CheckoutShell";
import { LoadingSpinner } from "@/components/shared/LoadingSpinner";
import { useCart } from "@/context/CartContext";

export default function CheckoutPage() {
  const router = useRouter();
  const { total, itemCount, clearCart } = useCart();
  const paymentCompleteRef = useRef(false);

  useEffect(() => {
    if (itemCount === 0 && !paymentCompleteRef.current) {
      router.replace("/store");
    }
  }, [itemCount, router]);

  const handlePaymentComplete = useCallback(() => {
    paymentCompleteRef.current = true;
    clearCart();
  }, [clearCart]);

  if (itemCount === 0 && !paymentCompleteRef.current) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-background">
      <header className="bg-primary text-white">
        <div className="px-6 h-14 flex items-center justify-between">
          <Link href="/" className="font-semibold text-base tracking-tight hover:text-white/80 transition-colors">
            Acabou o Mony
          </Link>
          <div className="flex items-center gap-1.5 text-white/60 text-xs">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Checkout seguro</span>
          </div>
        </div>
      </header>

      <main className="flex-1 flex items-start justify-center px-4 py-10">
        <div className="w-full max-w-[480px]">
          <div className="mb-8">
            <h1 className="text-xl font-semibold text-text-primary tracking-tight">
              Finalizar pagamento
            </h1>
            <p className="text-sm text-text-secondary mt-1.5">
              Preencha os dados do cartão para concluir sua compra.
            </p>
          </div>

          <Suspense
            fallback={
              <div className="flex justify-center py-16">
                <LoadingSpinner size="lg" />
              </div>
            }
          >
            <CheckoutShell amount={total} currency="BRL" onComplete={handlePaymentComplete} />
          </Suspense>
        </div>
      </main>
    </div>
  );
}
