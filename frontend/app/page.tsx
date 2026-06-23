import Link from "next/link";

export default function Home() {
  return (
    <main className="min-h-screen flex items-center justify-center bg-background px-4">
      <div className="text-center space-y-6">
        <h1 className="text-3xl font-bold text-text-primary">Acabou o Mony</h1>
        <p className="text-text-secondary">Plataforma de pagamentos</p>
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link
            href="/checkout"
            className="inline-flex items-center justify-center h-[52px] px-6 rounded-[10px] bg-primary text-white font-semibold hover:bg-primary/90 transition-colors"
          >
            Checkout
          </Link>
          <Link
            href="/login"
            className="inline-flex items-center justify-center h-[52px] px-6 rounded-[10px] border border-border text-text-primary font-semibold hover:bg-surface transition-colors"
          >
            Painel do Merchant
          </Link>
        </div>
      </div>
    </main>
  );
}
