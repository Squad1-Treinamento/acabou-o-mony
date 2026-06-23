import { Suspense } from "react";
import { CheckoutShell } from "@/components/checkout/CheckoutShell";
import { LoadingSpinner } from "@/components/shared/LoadingSpinner";

export default function CheckoutPage() {
  return (
    <main className="min-h-screen flex items-start justify-center px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <h1 className="text-xl font-bold text-text-primary">Finalizar pagamento</h1>
          <p className="text-sm text-text-secondary mt-1">Preencha os dados para concluir</p>
        </div>

        <Suspense fallback={<div className="flex justify-center py-16"><LoadingSpinner size="lg" /></div>}>
          <CheckoutShell
            amount={24990}
            currency="BRL"
          />
        </Suspense>
      </div>
    </main>
  );
}
