import { Suspense } from "react";
import Link from "next/link";
import { CheckoutShell } from "@/components/checkout/CheckoutShell";
import { LoadingSpinner } from "@/components/shared/LoadingSpinner";
import { ShieldCheck } from "lucide-react";

export default function CheckoutPage() {
  return (
    <div className="min-h-screen flex flex-col bg-background">
      {/* Header */}
      <header className="bg-primary text-white">
        <div className="max-w-2xl mx-auto px-4 h-14 flex items-center justify-between">
          <Link href="/" className="font-semibold text-base tracking-tight hover:text-white/80 transition-colors">Acabou o Mony</Link>
          <div className="flex items-center gap-1.5 text-white/60 text-xs">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Checkout seguro</span>
          </div>
        </div>
      </header>

      {/* Content */}
      <main className="flex-1 flex items-start justify-center px-4 py-10">
        <div className="w-full max-w-[480px]">
          <div className="mb-8">
            <h1 className="text-xl font-semibold text-text-primary tracking-tight">
              Finalizar pagamento
            </h1>
            <p className="text-sm text-text-secondary mt-1.5">
              Preencha os dados abaixo para concluir sua compra.
            </p>
          </div>

          <Suspense
            fallback={
              <div className="flex justify-center py-16">
                <LoadingSpinner size="lg" />
              </div>
            }
          >
            <CheckoutShell amount={24990} currency="BRL" />
          </Suspense>
        </div>
      </main>
    </div>
  );
}
