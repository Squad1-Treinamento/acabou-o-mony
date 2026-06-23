"use client";

import { useState } from "react";
import { ShoppingCart, Check, Plus, Minus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { formatCurrency } from "@/lib/utils/formatters";
import { useCart } from "@/context/CartContext";
import type { Product } from "@/types/store";

const CATEGORY_COLORS: Record<string, string> = {
  Calçados: "bg-emerald-50 text-emerald-700",
  Acessórios: "bg-blue-50 text-blue-700",
  Eletrônicos: "bg-violet-50 text-violet-700",
};

interface ProductCardProps {
  product: Product;
}

export function ProductCard({ product }: ProductCardProps) {
  const { items, addToCart, updateQuantity } = useCart();
  const [justAdded, setJustAdded] = useState(false);

  const cartItem = items.find((i) => i.product.id === product.id);
  const quantity = cartItem?.quantity ?? 0;

  function handleAdd() {
    addToCart(product);
    setJustAdded(true);
    setTimeout(() => setJustAdded(false), 1200);
  }

  return (
    <div className="bg-white rounded-card border border-border p-5 flex flex-col gap-4 hover:shadow-md transition-shadow">
      <div className="flex items-start justify-between gap-3">
        <div className="text-4xl leading-none">{product.emoji}</div>
        <span
          className={`text-[11px] font-medium px-2 py-0.5 rounded-full ${
            CATEGORY_COLORS[product.category] ?? "bg-gray-100 text-gray-600"
          }`}
        >
          {product.category}
        </span>
      </div>

      <div className="flex-1 space-y-1">
        <h3 className="font-semibold text-text-primary text-sm leading-snug">{product.name}</h3>
        <p className="text-xs text-text-secondary leading-relaxed">{product.description}</p>
      </div>

      <div className="flex items-center justify-between gap-3 pt-1">
        <span className="text-lg font-bold text-text-primary">
          {formatCurrency(product.price, "BRL")}
        </span>

        {quantity === 0 ? (
          <Button
            size="sm"
            onClick={handleAdd}
            className={`rounded-btn text-xs font-semibold transition-all ${
              justAdded
                ? "bg-success text-white"
                : "bg-primary hover:bg-primary/90 text-white"
            }`}
          >
            {justAdded ? (
              <>
                <Check className="w-3.5 h-3.5 mr-1" />
                Adicionado
              </>
            ) : (
              <>
                <ShoppingCart className="w-3.5 h-3.5 mr-1" />
                Adicionar
              </>
            )}
          </Button>
        ) : (
          <div className="flex items-center gap-2">
            <button
              onClick={() => updateQuantity(product.id, quantity - 1)}
              className="w-7 h-7 rounded-full border border-border flex items-center justify-center hover:bg-background transition-colors"
            >
              <Minus className="w-3 h-3 text-text-secondary" />
            </button>
            <span className="w-5 text-center text-sm font-semibold text-text-primary tabular-nums">
              {quantity}
            </span>
            <button
              onClick={() => addToCart(product)}
              className="w-7 h-7 rounded-full border border-border flex items-center justify-center hover:bg-background transition-colors"
            >
              <Plus className="w-3 h-3 text-text-secondary" />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
