import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { TransactionDetail } from "@/components/dashboard/TransactionDetail";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function TransactionDetailPage({ params }: Props) {
  const { id } = await params;

  return (
    <div className="max-w-2xl mx-auto w-full px-6 py-8">
      <Link
        href="/dashboard"
        className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-700 transition-colors mb-8"
      >
        <ArrowLeft className="w-3.5 h-3.5" />
        Transações
      </Link>
      <TransactionDetail transactionId={id} />
    </div>
  );
}
