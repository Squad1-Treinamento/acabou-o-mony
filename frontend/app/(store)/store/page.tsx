"use client";

import Link from "next/link";
import Image from "next/image";
import { ShoppingBag, Search, User, Heart } from "lucide-react";
import { ProductCard } from "@/components/store/ProductCard";
import { useCart } from "@/context/CartContext";
import { PRODUCTS } from "@/types/store";
import { formatCurrency } from "@/lib/utils/formatters";

const NAV_LINKS = ["Novidades", "Calçados", "Acessórios", "Eletrônicos", "Promoções"];

export default function StorePage() {
  const { itemCount, total } = useCart();

  return (
    <div className="min-h-screen bg-[#F5F5F7] flex flex-col">

      {/* Announcement bar */}
      <div className="bg-slate-900 text-white text-xs text-center py-2 px-4 tracking-wide">
        ✦ Pagamento 100% seguro · Devolução grátis em 30 dias ✦
      </div>

      {/* Header — Apple style */}
      <header
        className="sticky top-0 z-50"
        style={{
          background: "rgba(255,255,255,0.88)",
          backdropFilter: "saturate(180%) blur(20px)",
          WebkitBackdropFilter: "saturate(180%) blur(20px)",
          borderBottom: "1px solid rgba(0,0,0,0.07)",
        }}
      >
        {/* Main nav row */}
        <div className="w-full px-8 h-20 flex items-center justify-between gap-6">
          <Link href="/" className="hover:opacity-75 transition-opacity shrink-0">
            <Image
              src="/vibe-store.png"
              alt="Vibe Store"
              width={400}
              height={533}
              className="h-16 w-auto"
              unoptimized
            />
          </Link>

          {/* Center nav links */}
          <nav className="hidden md:flex items-center gap-6">
            {NAV_LINKS.map((label) => (
              <span
                key={label}
                className="text-sm text-slate-600 hover:text-slate-900 transition-colors cursor-pointer whitespace-nowrap"
              >
                {label}
              </span>
            ))}
          </nav>

          {/* Right icons */}
          <div className="flex items-center gap-4 shrink-0">
            <Search className="w-4 h-4 text-slate-500 hover:text-slate-900 transition-colors cursor-pointer" />
            <User className="w-4 h-4 text-slate-500 hover:text-slate-900 transition-colors cursor-pointer" />
            <Heart className="w-4 h-4 text-slate-500 hover:text-slate-900 transition-colors cursor-pointer" />
            <Link href="/store/cart" className="relative">
              <ShoppingBag className="w-4 h-4 text-slate-500 hover:text-slate-900 transition-colors" />
              {itemCount > 0 && (
                <span className="absolute -top-2 -right-2 bg-[#0066CC] text-white text-[9px] font-bold rounded-full w-4 h-4 flex items-center justify-center">
                  {itemCount}
                </span>
              )}
            </Link>
          </div>
        </div>

      </header>

      {/* Hero */}
      <div className="px-8 pt-12 pb-8 max-w-6xl mx-auto w-full">
        <div className="flex items-center gap-2 mb-4">
          <span className="text-[11px] font-semibold text-white bg-[#0066CC] px-2.5 py-1 rounded-full">Nova coleção</span>
          <span className="text-[11px] font-semibold text-white bg-rose-500 px-2.5 py-1 rounded-full">Sale</span>
        </div>
        <h1 className="text-4xl font-bold text-slate-900 tracking-tight leading-tight">
          Tudo que você precisa,<br className="hidden sm:block" /> num só lugar.
        </h1>
        <p className="text-slate-500 mt-3 text-sm">{PRODUCTS.length} produtos disponíveis</p>
      </div>

      {/* Grid */}
      <main className="flex-1 max-w-6xl mx-auto w-full px-8 pb-24">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {PRODUCTS.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </main>

      {/* Sticky cart bar */}
      {itemCount > 0 && (
        <div
          className="sticky bottom-0 px-8 py-4"
          style={{
            background: "rgba(255,255,255,0.92)",
            backdropFilter: "saturate(180%) blur(20px)",
            WebkitBackdropFilter: "saturate(180%) blur(20px)",
            borderTop: "1px solid rgba(0,0,0,0.06)",
          }}
        >
          <div className="max-w-6xl mx-auto flex items-center justify-between gap-4">
            <div>
              <p className="text-xs text-slate-500">
                {itemCount} {itemCount === 1 ? "item" : "itens"} no carrinho
              </p>
              <p className="font-bold text-slate-900 text-sm">{formatCurrency(total, "BRL")}</p>
            </div>
            <Link
              href="/store/cart"
              className="inline-flex items-center gap-2 h-10 px-6 rounded-full bg-[#0066CC] text-white text-xs font-semibold hover:bg-[#0055AA] transition-colors"
            >
              <ShoppingBag className="w-4 h-4" />
              Ver carrinho
            </Link>
          </div>
        </div>
      )}

    </div>
  );
}
