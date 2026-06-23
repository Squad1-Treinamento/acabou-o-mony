import { TransactionDetail } from "@/components/dashboard/TransactionDetail";
import { AppHeader } from "@/components/layout/AppHeader";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function TransactionDetailPage({ params }: Props) {
  const { id } = await params;

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <AppHeader backHref="/dashboard" backLabel="Transações" showLogout />
      <main className="flex-1">
        <div className="max-w-2xl mx-auto px-4 py-8">
          <TransactionDetail transactionId={id} />
        </div>
      </main>
    </div>
  );
}
