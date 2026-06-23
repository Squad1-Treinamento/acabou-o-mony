import { TransactionDetail } from "@/components/dashboard/TransactionDetail";

interface Props {
  params: Promise<{ id: string }>;
}

export default async function TransactionDetailPage({ params }: Props) {
  const { id } = await params;

  return (
    <main className="min-h-screen bg-background">
      <div className="max-w-2xl mx-auto px-4 py-8">
        <TransactionDetail transactionId={id} />
      </div>
    </main>
  );
}
