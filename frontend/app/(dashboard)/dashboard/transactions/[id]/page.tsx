import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { TransactionDetail } from "@/components/dashboard/TransactionDetail";
import { AppHeader } from "@/components/layout/AppHeader";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function TransactionDetailPage({ params }: Props) {
  const { id } = await params;

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <AppHeader showLogout />
      <main className="flex-1">
        <div className="max-w-2xl mx-auto px-4 py-8">
          <Link
            href="/dashboard"
            className="inline-flex items-center gap-1.5 text-sm text-text-secondary hover:text-text-primary transition-colors mb-6"
          >
            <ArrowLeft className="w-4 h-4" />
            Transações
          </Link>
          <TransactionDetail transactionId={id} />
        </div>
      </main>
    </div>
  );
}
