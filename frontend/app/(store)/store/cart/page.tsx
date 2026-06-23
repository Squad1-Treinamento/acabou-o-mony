"use client";

import Link from "next/link";
import { ArrowLeft, Trash2, ShoppingBag } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useCart } from "@/context/CartContext";
import { formatCurrency } from "@/lib/utils/formatters";

export default function CartPage() {
  const { items, updateQuantity, removeFromCart, total, itemCount } = useCart();

  if (itemCount === 0) {
    return (
      <div className="min-h-screen bg-background flex flex-col">
        <header className="bg-primary text-white">
          <div className="px-6 h-14 flex items-center gap-4">
            <Link
              href="/store"
              className="flex items-center gap-1.5 text-white/70 hover:text-white transition-colors text-sm"
            >
              <ArrowLeft className="w-4 h-4" />
              Voltar
            </Link>
            <Link
              href="/"
              className="font-semibold text-base tracking-tight hover:text-white/80 transition-colors"
            >
              Vibe Store
            </Link>
          </div>
        </header>
        <div className="flex-1 flex flex-col items-center justify-center gap-4 text-center px-6">
          <ShoppingBag className="w-12 h-12 text-text-secondary/40" />
          <p className="font-semibold text-text-primary">Carrinho vazio</p>
          <p className="text-sm text-text-secondary">Adicione produtos antes de finalizar a compra.</p>
          <Link
            href="/store"
            className="inline-flex items-center gap-2 h-11 px-6 rounded-btn bg-primary text-white text-sm font-semibold hover:bg-primary/90 transition-colors"
          >
            Ver produtos
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background flex flex-col">
      <header className="bg-primary text-white">
        <div className="px-6 h-14 flex items-center">
          <Link
            href="/"
            className="font-semibold text-base tracking-tight hover:text-white/80 transition-colors"
          >
            Vibe Store
          </Link>
        </div>
      </header>

      <main className="flex-1 max-w-xl mx-auto w-full px-6 py-10">
        <Link
          href="/store"
          className="inline-flex items-center gap-1.5 text-sm text-text-secondary hover:text-text-primary transition-colors mb-6"
        >
          <ArrowLeft className="w-4 h-4" />
          Continuar comprando
        </Link>

        <h1 className="text-xl font-bold text-text-primary mb-6">
          Carrinho ({itemCount} {itemCount === 1 ? "item" : "itens"})
        </h1>

        <div className="space-y-3">
          {items.map(({ product, quantity }) => (
            <div
              key={product.id}
              className="bg-white rounded-card border border-border p-4 flex items-center gap-4"
            >
              <div className="text-3xl leading-none w-10 shrink-0 text-center">{product.emoji}</div>

              <div className="flex-1 min-w-0">
                <p className="font-medium text-text-primary text-sm truncate">{product.name}</p>
                <p className="text-xs text-text-secondary mt-0.5">
                  {formatCurrency(product.price, "BRL")} cada
                </p>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                <button
                  onClick={() => updateQuantity(product.id, quantity - 1)}
                  className="w-7 h-7 rounded-full border border-border flex items-center justify-center hover:bg-background transition-colors text-sm font-bold text-text-secondary"
                >
                  −
                </button>
                <span className="w-5 text-center text-sm font-semibold tabular-nums">{quantity}</span>
                <button
                  onClick={() => updateQuantity(product.id, quantity + 1)}
                  className="w-7 h-7 rounded-full border border-border flex items-center justify-center hover:bg-background transition-colors text-sm font-bold text-text-secondary"
                >
                  +
                </button>
              </div>

              <div className="text-right shrink-0 min-w-[72px]">
                <p className="font-semibold text-text-primary text-sm">
                  {formatCurrency(product.price * quantity, "BRL")}
                </p>
              </div>

              <button
                onClick={() => removeFromCart(product.id)}
                className="text-text-secondary/50 hover:text-error transition-colors shrink-0"
                aria-label="Remover item"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>

        <div className="mt-6 bg-white rounded-card border border-border p-5 space-y-3">
          {items.map(({ product, quantity }) => (
            <div key={product.id} className="flex justify-between text-sm">
              <span className="text-text-secondary">
                {product.name} × {quantity}
              </span>
              <span className="text-text-primary font-medium">
                {formatCurrency(product.price * quantity, "BRL")}
              </span>
            </div>
          ))}
          <div className="border-t border-border pt-3 flex justify-between items-baseline">
            <span className="font-semibold text-text-primary">Total</span>
            <span className="text-2xl font-bold text-text-primary">{formatCurrency(total, "BRL")}</span>
          </div>
        </div>

        <div className="mt-6">
          <Link
            href="/checkout"
            className="flex items-center justify-center gap-2 w-full h-[52px] rounded-btn bg-primary text-white font-semibold hover:bg-primary/90 transition-colors"
          >
            Ir para o checkout
          </Link>
        </div>
      </main>
    </div>
  );
}
