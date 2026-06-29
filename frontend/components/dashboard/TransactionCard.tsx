import Link from "next/link";
import { StatusBadge } from "./StatusBadge";
import { formatCurrency, formatRelativeTime, truncateUUID } from "@/lib/utils/formatters";
import type { PaymentResponse } from "@/types/payment";

interface TransactionCardProps {
  transaction: PaymentResponse;
}

export function TransactionCard({ transaction: tx }: TransactionCardProps) {
  return (
    <Link
      href={`/dashboard/transactions/${tx.transaction_id}`}
      className={`block rounded-xl border px-4 py-3.5 transition-colors hover:border-slate-300 ${
        tx.status === "UNKNOWN"
          ? "bg-amber-50/60 border-amber-200/60"
          : "bg-white border-slate-200"
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-1 min-w-0">
          <p className="font-mono text-[11px] text-slate-400 truncate">
            {truncateUUID(tx.transaction_id)}
          </p>
          <p className="text-base font-bold text-slate-900 tabular-nums">
            {formatCurrency(tx.amount, tx.currency)}
          </p>
          {tx.idempotency_key && (
            <p className="font-mono text-[11px] text-slate-400 truncate">
              {tx.idempotency_key.slice(0, 8)}…
            </p>
          )}
        </div>
        <div className="flex flex-col items-end gap-1.5 shrink-0">
          <StatusBadge status={tx.status} />
          <span className="text-[11px] text-slate-400">
            {formatRelativeTime(tx.created_at)}
          </span>
        </div>
      </div>
    </Link>
  );
}
