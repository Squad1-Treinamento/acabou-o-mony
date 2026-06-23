"use client";

import Link from "next/link";
import { ShoppingCart } from "lucide-react";
import { ProductCard } from "@/components/store/ProductCard";
import { useCart } from "@/context/CartContext";
import { PRODUCTS } from "@/types/store";
import { formatCurrency } from "@/lib/utils/formatters";

export default function StorePage() {
  const { itemCount, total } = useCart();

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <header className="bg-primary text-white">
        <div className="px-6 h-14 flex items-center justify-between">
          <Link
            href="/"
            className="font-semibold text-base tracking-tight hover:text-white/80 transition-colors"
          >
            Vibe Store
          </Link>

          <Link
            href="/store/cart"
            className="relative flex items-center gap-2 text-white/80 hover:text-white transition-colors text-sm"
          >
            <span className="hidden sm:inline">Carrinho</span>
            <div className="relative">
              <ShoppingCart className="w-5 h-5" />
              {itemCount > 0 && (
                <span className="absolute -top-2 -right-2 bg-white text-primary text-[10px] font-bold rounded-full w-4 h-4 flex items-center justify-center">
                  {itemCount}
                </span>
              )}
            </div>
          </Link>
        </div>
      </header>

      <main className="flex-1 max-w-5xl mx-auto w-full px-6 py-10">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-text-primary tracking-tight">Loja</h1>
          <p className="text-text-secondary text-sm mt-1">Adicione itens ao carrinho e finalize sua compra</p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {PRODUCTS.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </main>

      {itemCount > 0 && (
        <div className="sticky bottom-0 bg-white border-t border-border px-6 py-4 shadow-lg">
          <div className="max-w-5xl mx-auto flex items-center justify-between gap-4">
            <div>
              <p className="text-sm text-text-secondary">
                {itemCount} {itemCount === 1 ? "item" : "itens"} no carrinho
              </p>
              <p className="font-bold text-text-primary">{formatCurrency(total, "BRL")}</p>
            </div>
            <Link
              href="/store/cart"
              className="inline-flex items-center gap-2 h-11 px-6 rounded-btn bg-primary text-white text-sm font-semibold hover:bg-primary/90 transition-colors"
            >
              <ShoppingCart className="w-4 h-4" />
              Ver carrinho
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
