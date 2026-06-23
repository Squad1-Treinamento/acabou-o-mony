"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { StatusBadge } from "./StatusBadge";
import { CopyButton } from "@/components/shared/CopyButton";
import { ErrorState } from "@/components/shared/ErrorState";
import { formatCurrency, formatRelativeTime, truncateUUID } from "@/lib/utils/formatters";
import { useGetPayment } from "@/hooks/usePayment";

interface TransactionDetailProps {
  transactionId: string;
}

export function TransactionDetail({ transactionId }: TransactionDetailProps) {
  const { data, isLoading, error, refetch } = useGetPayment(transactionId, true);

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-64 w-full rounded-card" />
      </div>
    );
  }

  if (error || !data) {
    return (
      <ErrorState
        message="Não foi possível carregar os detalhes da transação."
        onRetry={() => refetch()}
      />
    );
  }

  const tx = data;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link
          href="/dashboard"
          className="flex items-center gap-1.5 text-sm text-text-secondary hover:text-text-primary transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Voltar
        </Link>
      </div>

      {tx.status === "UNKNOWN" && (
        <Alert className="border-[#D97706]/30 bg-[#FFFBEB]">
          <AlertDescription className="text-[#D97706] text-sm">
            Esta transação está em reconciliação. O status será atualizado automaticamente.
          </AlertDescription>
        </Alert>
      )}

      <Card className="rounded-card border-border shadow-sm">
        <CardContent className="p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-text-primary">Detalhes da transação</h2>
            <StatusBadge status={tx.status} />
          </div>

          <Separator />

          <div className="grid gap-3">
            <DetailRow label="ID da transação">
              <div className="flex items-center gap-1">
                <span className="font-mono text-sm">{truncateUUID(tx.transaction_id)}</span>
                <CopyButton value={tx.transaction_id} label="Copiar ID" />
              </div>
            </DetailRow>

            <Separator />

            <DetailRow label="Valor">
              <span className="font-semibold">{formatCurrency(tx.amount, tx.currency)}</span>
            </DetailRow>

            <Separator />

            <DetailRow label="Moeda">
              <span>{tx.currency}</span>
            </DetailRow>

            {tx.masked_card && (
              <>
                <Separator />
                <DetailRow label="Cartão">{tx.masked_card}</DetailRow>
              </>
            )}

            {tx.idempotency_key && (
              <>
                <Separator />
                <DetailRow label="Chave de idempotência">
                  <div className="flex items-center gap-1">
                    <span className="font-mono text-xs text-text-secondary truncate max-w-[200px]">
                      {tx.idempotency_key}
                    </span>
                    <CopyButton value={tx.idempotency_key} label="Copiar chave" />
                  </div>
                </DetailRow>
              </>
            )}

            {tx.challenge_id && (
              <>
                <Separator />
                <DetailRow label="Challenge ID">
                  <span className="font-mono text-xs text-text-secondary">{tx.challenge_id}</span>
                </DetailRow>
              </>
            )}

            <Separator />

            <DetailRow label="Criado em">
              <span className="text-text-secondary text-sm">
                {formatRelativeTime(tx.created_at)}
                <span className="ml-2 text-xs opacity-60">
                  ({new Date(tx.created_at).toLocaleString("pt-BR")})
                </span>
              </span>
            </DetailRow>

            <Separator />

            <DetailRow label="Atualizado em">
              <span className="text-text-secondary text-sm">
                {formatRelativeTime(tx.updated_at)}
              </span>
            </DetailRow>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function DetailRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <span className="text-sm text-text-secondary whitespace-nowrap">{label}</span>
      <div className="text-sm text-text-primary text-right">{children}</div>
    </div>
  );
}
