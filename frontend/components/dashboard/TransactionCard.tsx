import Link from "next/link";
import { StatusBadge } from "./StatusBadge";
import { formatCurrency, formatRelativeTime, truncateUUID } from "@/lib/utils/formatters";
import type { PaymentResponse } from "@/types/payment";

interface TransactionCardProps {
  transaction: PaymentResponse;
}

export function TransactionCard({ transaction }: TransactionCardProps) {
  return (
    <Link
      href={`/dashboard/transactions/${transaction.transaction_id}`}
      className="block rounded-card border border-border bg-white p-4 hover:border-primary/30 transition-colors"
    >
      <div className="flex items-start justify-between gap-2">
        <div className="space-y-1">
          <p className="text-xs font-mono text-text-secondary">
            {truncateUUID(transaction.transaction_id)}
          </p>
          <p className="text-base font-semibold text-text-primary">
            {formatCurrency(transaction.amount, transaction.currency)}
          </p>
          {transaction.masked_card && (
            <p className="text-xs text-text-secondary">{transaction.masked_card}</p>
          )}
        </div>
        <div className="flex flex-col items-end gap-1.5">
          <StatusBadge status={transaction.status} />
          <span className="text-xs text-text-secondary">
            {formatRelativeTime(transaction.created_at)}
          </span>
        </div>
      </div>
    </Link>
  );
}
