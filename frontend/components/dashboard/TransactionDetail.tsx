"use client";

import { Skeleton } from "@/components/ui/skeleton";
import { StatusBadge } from "./StatusBadge";
import { CopyButton } from "@/components/shared/CopyButton";
import { ErrorState } from "@/components/shared/ErrorState";
import { formatCurrency, formatRelativeTime, truncateUUID } from "@/lib/utils/formatters";
import { useGetPayment } from "@/hooks/usePayment";

interface TransactionDetailProps {
  transactionId: string;
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between py-3.5 border-b border-slate-100 last:border-0">
      <span className="text-xs font-medium text-slate-400 uppercase tracking-wider w-44 shrink-0">
        {label}
      </span>
      <div className="text-sm text-slate-800 text-right flex items-center gap-1">
        {children}
      </div>
    </div>
  );
}

export function TransactionDetail({ transactionId }: TransactionDetailProps) {
  const { data, isLoading, error, refetch } = useGetPayment(transactionId, true, 10_000);

  if (isLoading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-7 w-36 rounded-lg" />
        <Skeleton className="h-72 w-full rounded-xl" />
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
    <div className="space-y-5">
      {/* Status banner for UNKNOWN */}
      {tx.status === "UNKNOWN" && (
        <div className="flex items-start gap-3 rounded-xl bg-amber-50 border border-amber-200/60 px-4 py-3">
          <div className="w-1.5 h-1.5 rounded-full bg-amber-400 mt-1.5 shrink-0" />
          <p className="text-sm text-amber-700">
            Esta transação está em reconciliação. O status será atualizado automaticamente.
          </p>
        </div>
      )}

      {/* Status banner for stuck VALIDATED */}
      {tx.status === "VALIDATED" && (
        <div className="flex items-start gap-3 rounded-xl bg-red-50 border border-red-200/60 px-4 py-3">
          <div className="w-1.5 h-1.5 rounded-full bg-red-400 mt-1.5 shrink-0" />
          <p className="text-sm text-red-700">
            Esta transação ficou presa na validação devido a um erro interno. Não foi cobrada e não requer ação do portador.
          </p>
        </div>
      )}

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <p className="font-mono text-xs text-slate-400 mb-1">{truncateUUID(tx.transaction_id)}</p>
          <p className="text-2xl font-bold text-slate-900 tabular-nums">
            {formatCurrency(tx.amount, tx.currency)}
          </p>
        </div>
        <StatusBadge status={tx.status} />
      </div>

      {/* Detail rows */}
      <div className="bg-white rounded-xl border border-slate-200 px-5">
        <Row label="ID da transação">
          <span className="font-mono text-xs text-slate-600">{truncateUUID(tx.transaction_id)}</span>
          <CopyButton value={tx.transaction_id} label="Copiar ID" />
        </Row>

        <Row label="Valor">
          <span className="font-semibold">{formatCurrency(tx.amount, tx.currency)}</span>
        </Row>

        <Row label="Moeda">
          <span className="font-mono text-xs">{tx.currency}</span>
        </Row>

        {tx.idempotency_key && (
          <Row label="Idempotência">
            <span className="font-mono text-xs text-slate-500 truncate max-w-[200px]">
              {tx.idempotency_key}
            </span>
            <CopyButton value={tx.idempotency_key} label="Copiar chave" />
          </Row>
        )}

        {tx.challenge_id && (
          <Row label="Challenge ID">
            <span className="font-mono text-xs text-slate-500">{tx.challenge_id}</span>
          </Row>
        )}

        <Row label="Criado em">
          <span>
            {formatRelativeTime(tx.created_at)}{" "}
            <span className="text-xs text-slate-400">
              · {new Date(tx.created_at).toLocaleString("pt-BR")}
            </span>
          </span>
        </Row>

        <Row label="Atualizado em">
          {formatRelativeTime(tx.updated_at)}
        </Row>
      </div>
    </div>
  );
}
