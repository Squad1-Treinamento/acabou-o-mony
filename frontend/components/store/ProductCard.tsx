"use client";

import { useState } from "react";
import Image from "next/image";
import { ShoppingBag, Check, Plus, Minus } from "lucide-react";
import { formatCurrency } from "@/lib/utils/formatters";
import { useCart } from "@/context/CartContext";
import type { Product } from "@/types/store";

const CATEGORY_GRADIENTS: Record<string, string> = {
  Calçados:    "from-orange-50 to-amber-100",
  Acessórios:  "from-sky-50 to-blue-100",
  Eletrônicos: "from-violet-50 to-indigo-100",
};

interface ProductCardProps {
  product: Product;
}

export function ProductCard({ product }: ProductCardProps) {
  const { items, addToCart, updateQuantity } = useCart();
  const [justAdded, setJustAdded] = useState(false);
  const [imgError, setImgError] = useState(false);

  const cartItem = items.find((i) => i.product.id === product.id);
  const quantity = cartItem?.quantity ?? 0;
  const gradient = CATEGORY_GRADIENTS[product.category] ?? "from-slate-50 to-slate-100";

  function handleAdd() {
    addToCart(product);
    setJustAdded(true);
    setTimeout(() => setJustAdded(false), 1200);
  }

  return (
    <div className="bg-white rounded-2xl overflow-hidden hover:shadow-xl transition-all duration-300 hover:-translate-y-0.5">
      {/* Imagem */}
      <div className={`relative aspect-square bg-gradient-to-br ${gradient} flex items-center justify-center`}>
        {product.image && !imgError ? (
          <Image
            src={product.image}
            alt={product.name}
            fill
            className="object-cover"
            onError={() => setImgError(true)}
          />
        ) : (
          <span className="text-7xl select-none">{product.emoji}</span>
        )}
      </div>

      {/* Info */}
      <div className="px-5 py-4 space-y-3">
        <div>
          <p className="text-[11px] font-medium text-slate-400 uppercase tracking-wider mb-1">
            {product.category}
          </p>
          <h3 className="font-semibold text-slate-900 text-sm leading-snug">{product.name}</h3>
          <p className="text-xs text-slate-500 mt-1 leading-relaxed">{product.description}</p>
        </div>

        <div className="flex items-center justify-between pt-1">
          <span className="text-base font-bold text-slate-900">
            {formatCurrency(product.price, "BRL")}
          </span>

          {quantity === 0 ? (
            <button
              onClick={handleAdd}
              className={`inline-flex items-center gap-1.5 text-xs font-semibold px-4 py-2 rounded-full transition-all ${
                justAdded
                  ? "bg-emerald-500 text-white"
                  : "bg-[#0066CC] text-white hover:bg-[#0055AA]"
              }`}
            >
              {justAdded ? (
                <><Check className="w-3.5 h-3.5" /> Adicionado</>
              ) : (
                <><ShoppingBag className="w-3.5 h-3.5" /> Adicionar</>
              )}
            </button>
          ) : (
            <div className="flex items-center gap-2">
              <button
                onClick={() => updateQuantity(product.id, quantity - 1)}
                className="w-7 h-7 rounded-full bg-slate-100 flex items-center justify-center hover:bg-slate-200 transition-colors"
              >
                <Minus className="w-3 h-3 text-slate-600" />
              </button>
              <span className="w-5 text-center text-sm font-semibold text-slate-900 tabular-nums">
                {quantity}
              </span>
              <button
                onClick={() => addToCart(product)}
                className="w-7 h-7 rounded-full bg-[#0066CC] flex items-center justify-center hover:bg-[#0055AA] transition-colors"
              >
                <Plus className="w-3 h-3 text-white" />
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
