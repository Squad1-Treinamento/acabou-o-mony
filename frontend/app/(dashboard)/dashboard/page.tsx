"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { TransactionTable } from "@/components/dashboard/TransactionTable";
import { TransactionLookup } from "@/components/dashboard/TransactionLookup";
import { FilterBar } from "@/components/dashboard/FilterBar";
import { useTransactions, filterTransactions } from "@/hooks/useTransactions";
import type { TransactionFilters } from "@/hooks/useTransactions";

export default function DashboardPage() {
  const router = useRouter();
  const [transactionIds, setTransactionIds] = useState<string[]>([]);
  const [filters, setFilters] = useState<TransactionFilters>({});

  useEffect(() => {
    const key = sessionStorage.getItem("mony_api_key");
    if (!key) router.replace("/login");
  }, [router]);

  const { results, isLoading, refetchAll } = useTransactions(transactionIds);
  const filtered = filterTransactions(results, filters);

  const handleSearch = useCallback((ids: string[]) => {
    setTransactionIds(ids);
    setFilters({});
  }, []);

  return (
    <main className="min-h-screen bg-background">
      <header className="border-b border-border bg-white sticky top-0 z-10">
        <div className="max-w-6xl mx-auto px-4 py-4 flex items-center justify-between">
          <h1 className="text-xl font-bold text-text-primary">Transações</h1>
          <div className="flex items-center gap-3">
            <Button
              variant="outline"
              size="sm"
              onClick={refetchAll}
              className="text-text-secondary"
            >
              Atualizar
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                sessionStorage.removeItem("mony_api_key");
                router.push("/login");
              }}
              className="text-text-secondary"
            >
              Sair
            </Button>
          </div>
        </div>
      </header>

      <div className="max-w-6xl mx-auto px-4 py-8">
        <TransactionLookup onSearch={handleSearch} />

        {transactionIds.length > 0 && (
          <>
            <FilterBar filters={filters} onChange={setFilters} />
            <TransactionTable transactions={filtered} isLoading={isLoading} />
          </>
        )}
      </div>
    </main>
  );
}
