"use client";

import { useEffect } from "react";
import { CheckCircle } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { CopyButton } from "@/components/shared/CopyButton";
import { formatCurrency, formatRelativeTime, truncateUUID } from "@/lib/utils/formatters";
import { clearCheckoutSession } from "@/lib/utils/idempotency";
import type { PaymentResponse } from "@/types/payment";

interface StepSuccessProps {
  payment: PaymentResponse;
}

export function StepSuccess({ payment }: StepSuccessProps) {
  useEffect(() => {
    clearCheckoutSession();
  }, []);

  return (
    <div className="flex flex-col items-center gap-6">
      <div className="flex flex-col items-center gap-3 text-center">
        <CheckCircle className="w-12 h-12 text-success" />
        <h3 className="text-xl font-semibold text-text-primary">Pagamento confirmado</h3>
        <p className="text-sm text-text-secondary">
          Sua transação foi processada com sucesso.
        </p>
      </div>

      <Card className="w-full rounded-card border-border shadow-sm">
        <CardContent className="p-5 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-sm text-text-secondary">ID da transação</span>
            <div className="flex items-center gap-1">
              <span className="text-sm font-mono text-text-primary">
                {truncateUUID(payment.transaction_id)}
              </span>
              <CopyButton value={payment.transaction_id} label="Copiar ID" />
            </div>
          </div>

          <Separator />

          <div className="flex items-center justify-between">
            <span className="text-sm text-text-secondary">Valor</span>
            <span className="text-sm font-semibold text-text-primary">
              {formatCurrency(payment.amount, payment.currency)}
            </span>
          </div>

          {payment.masked_card && (
            <>
              <Separator />
              <div className="flex items-center justify-between">
                <span className="text-sm text-text-secondary">Cartão</span>
                <span className="text-sm text-text-primary">{payment.masked_card}</span>
              </div>
            </>
          )}

          <Separator />

          <div className="flex items-center justify-between">
            <span className="text-sm text-text-secondary">Confirmado</span>
            <span className="text-sm text-text-primary">
              {formatRelativeTime(payment.updated_at)}
            </span>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
