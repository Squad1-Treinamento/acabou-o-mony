"use client";

import { useEffect } from "react";
import Link from "next/link";
import { CheckCircle2, ShoppingBag, Copy } from "lucide-react";
import { formatCurrency, formatRelativeTime, truncateUUID } from "@/lib/utils/formatters";
import { clearCheckoutSession } from "@/lib/utils/idempotency";
import type { PaymentResponse } from "@/types/payment";

interface StepSuccessProps {
  payment: PaymentResponse;
}

function Row({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="flex items-center justify-between py-3 border-b border-slate-100 last:border-0">
      <span className="text-sm text-slate-500">{label}</span>
      <span className={`text-sm font-medium text-slate-900 ${mono ? "font-mono" : ""}`}>
        {value}
      </span>
    </div>
  );
}

export function StepSuccess({ payment }: StepSuccessProps) {
  useEffect(() => {
    clearCheckoutSession();
  }, []);

  function copyId() {
    navigator.clipboard.writeText(payment.transaction_id);
  }

  return (
    <div className="space-y-6">
      {/* Status */}
      <div className="flex flex-col items-center text-center gap-3 py-4">
        <div className="w-14 h-14 rounded-full bg-emerald-50 flex items-center justify-center">
          <CheckCircle2 className="w-7 h-7 text-emerald-600" strokeWidth={1.75} />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Pagamento confirmado</h2>
          <p className="text-sm text-slate-500 mt-1">
            Sua transação foi processada com sucesso.
          </p>
        </div>
      </div>

      {/* Receipt */}
      <div className="bg-slate-50 rounded-xl px-5 py-1">
        <Row
          label="Valor pago"
          value={formatCurrency(payment.amount, payment.currency)}
        />
        {payment.masked_card && (
          <Row label="Cartão" value={payment.masked_card} />
        )}
        <Row
          label="Data"
          value={formatRelativeTime(payment.updated_at)}
        />
        <div className="flex items-center justify-between py-3">
          <span className="text-sm text-slate-500">ID da transação</span>
          <button
            onClick={copyId}
            className="flex items-center gap-1.5 text-sm font-mono text-slate-700 hover:text-slate-900 transition-colors"
          >
            {truncateUUID(payment.transaction_id)}
            <Copy className="w-3 h-3 text-slate-400" />
          </button>
        </div>
      </div>

      {/* CTA */}
      <Link
        href="/store"
        className="flex items-center justify-center gap-2 w-full h-12 rounded-xl font-semibold text-sm text-white transition-colors"
        style={{ background: "#0D2B1E" }}
      >
        <ShoppingBag className="w-4 h-4" />
        Continuar comprando
      </Link>
    </div>
  );
}
