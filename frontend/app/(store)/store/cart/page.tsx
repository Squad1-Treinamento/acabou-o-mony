"use client";

import Link from "next/link";
import Image from "next/image";
import { useState } from "react";
import { ArrowLeft, Trash2, ShoppingBag, Search, User, Heart } from "lucide-react";
import { useCart } from "@/context/CartContext";
import { formatCurrency } from "@/lib/utils/formatters";
import type { Product } from "@/types/store";

const CATEGORY_GRADIENTS: Record<string, string> = {
  Calçados:    "from-orange-50 to-amber-100",
  Acessórios:  "from-sky-50 to-blue-100",
  Eletrônicos: "from-violet-50 to-indigo-100",
};

function ProductThumb({ product }: { product: Product }) {
  const [imgError, setImgError] = useState(false);
  const gradient = CATEGORY_GRADIENTS[product.category] ?? "from-slate-50 to-slate-100";
  return (
    <div className={`relative w-14 h-14 rounded-xl bg-gradient-to-br ${gradient} flex items-center justify-center shrink-0 overflow-hidden`}>
      {product.image && !imgError ? (
        <Image src={product.image} alt={product.name} fill className="object-cover" onError={() => setImgError(true)} />
      ) : (
        <span className="text-2xl select-none">{product.emoji}</span>
      )}
    </div>
  );
}

const NAV_LINKS = ["Novidades", "Calçados", "Acessórios", "Eletrônicos", "Promoções"];

function StoreHeader({ itemCount }: { itemCount: number }) {
  return (
    <>
      <div className="bg-slate-900 text-white text-xs text-center py-2 px-4 tracking-wide">
        ✦ Pagamento 100% seguro · Devolução grátis em 30 dias ✦
      </div>
      <header
        className="sticky top-0 z-50"
        style={{
          background: "rgba(255,255,255,0.88)",
          backdropFilter: "saturate(180%) blur(20px)",
          WebkitBackdropFilter: "saturate(180%) blur(20px)",
          borderBottom: "1px solid rgba(0,0,0,0.07)",
        }}
      >
        <div className="w-full px-8 h-14 flex items-center justify-between gap-6">
          <Link
            href="/"
            className="font-bold text-base text-slate-900 hover:text-slate-600 transition-colors shrink-0"
          >
            Vibe Store
          </Link>
          <nav className="hidden md:flex items-center gap-6">
            {NAV_LINKS.map((label) => (
              <span key={label} className="text-sm text-slate-600 hover:text-slate-900 transition-colors cursor-pointer whitespace-nowrap">
                {label}
              </span>
            ))}
          </nav>
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
    </>
  );
}

export default function CartPage() {
  const { items, updateQuantity, removeFromCart, total, itemCount } = useCart();

  if (itemCount === 0) {
    return (
      <div className="min-h-screen bg-[#F5F5F7] flex flex-col">
        <StoreHeader itemCount={0} />
        <div className="flex-1 flex flex-col items-center justify-center gap-4 text-center px-6">
          <div className="w-16 h-16 rounded-2xl bg-slate-100 flex items-center justify-center">
            <ShoppingBag className="w-7 h-7 text-slate-400" />
          </div>
          <p className="font-semibold text-slate-900 text-lg">Carrinho vazio</p>
          <p className="text-sm text-slate-500">Adicione produtos antes de finalizar a compra.</p>
          <Link
            href="/store"
            className="inline-flex items-center gap-2 h-10 px-6 rounded-full bg-[#0066CC] text-white text-xs font-semibold hover:bg-[#0055AA] transition-colors mt-2"
          >
            Ver produtos
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F5F5F7] flex flex-col">
      <StoreHeader itemCount={itemCount} />

      <main className="flex-1 max-w-2xl mx-auto w-full px-6 py-10">

        <Link
          href="/store"
          className="inline-flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-900 transition-colors mb-8"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          Continuar comprando
        </Link>

        <h1 className="text-2xl font-bold text-slate-900 mb-8">
          Carrinho <span className="text-slate-400 font-normal text-lg">({itemCount} {itemCount === 1 ? "item" : "itens"})</span>
        </h1>

        {/* Items */}
        <div className="space-y-3 mb-6">
          {items.map(({ product, quantity }) => (
            <div
              key={product.id}
              className="bg-white rounded-2xl p-4 flex items-center gap-4"
            >
              <ProductThumb product={product} />

              <div className="flex-1 min-w-0">
                <p className="font-semibold text-slate-900 text-sm truncate">{product.name}</p>
                <p className="text-xs text-slate-400 mt-0.5">
                  {formatCurrency(product.price, "BRL")} cada
                </p>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <button
                  onClick={() => updateQuantity(product.id, quantity - 1)}
                  className="w-7 h-7 rounded-full bg-slate-100 flex items-center justify-center hover:bg-slate-200 transition-colors text-sm font-bold text-slate-600"
                >
                  −
                </button>
                <span className="w-5 text-center text-sm font-semibold tabular-nums text-slate-900">{quantity}</span>
                <button
                  onClick={() => updateQuantity(product.id, quantity + 1)}
                  className="w-7 h-7 rounded-full bg-slate-100 flex items-center justify-center hover:bg-slate-200 transition-colors text-sm font-bold text-slate-600"
                >
                  +
                </button>
              </div>

              <div className="text-right shrink-0 min-w-[72px]">
                <p className="font-bold text-slate-900 text-sm">
                  {formatCurrency(product.price * quantity, "BRL")}
                </p>
              </div>

              <button
                onClick={() => removeFromCart(product.id)}
                className="text-slate-300 hover:text-rose-400 transition-colors shrink-0"
                aria-label="Remover item"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>

        {/* Resumo */}
        <div className="bg-white rounded-2xl p-5 space-y-3">
          {items.map(({ product, quantity }) => (
            <div key={product.id} className="flex justify-between text-sm">
              <span className="text-slate-500">
                {product.name} × {quantity}
              </span>
              <span className="text-slate-900 font-medium">
                {formatCurrency(product.price * quantity, "BRL")}
              </span>
            </div>
          ))}
          <div className="border-t border-slate-100 pt-4 flex justify-between items-baseline">
            <span className="font-semibold text-slate-900">Total</span>
            <span className="text-2xl font-bold text-slate-900">{formatCurrency(total, "BRL")}</span>
          </div>
        </div>

        <div className="mt-5">
          <Link
            href="/checkout"
            className="flex items-center justify-center gap-2 w-full h-12 rounded-full bg-[#0066CC] text-white font-semibold text-sm hover:bg-[#0055AA] transition-colors"
          >
            Finalizar compra
          </Link>
        </div>

      </main>
    </div>
  );
}
