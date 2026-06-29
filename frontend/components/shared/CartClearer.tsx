"use client";

import { useEffect } from "react";
import { useCart } from "@/context/CartContext";

export function CartClearer() {
  const { clearCart } = useCart();

  useEffect(() => {
    clearCart();
  }, [clearCart]);

  return null;
}
