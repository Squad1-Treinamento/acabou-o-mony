"use client";

import { useForm, Controller } from "react-hook-form";
import { CreditCard, Lock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SecurityBadge } from "@/components/shared/SecurityBadge";
import { formatCurrency } from "@/lib/utils/formatters";
import { useCart } from "@/context/CartContext";

interface CardFormValues {
  card_number: string;
  cardholder_name: string;
  expiry: string;
  cvv: string;
  customer_email: string;
}

interface StepPaymentFormProps {
  amount: number;
  currency: string;
  isSubmitting: boolean;
  onSubmit: (data: { card_token_id: string; customer_email?: string }) => void;
}

function detectBrand(number: string): string {
  const n = number.replace(/\D/g, "");
  if (n.startsWith("4")) return "visa";
  if (/^5[1-5]/.test(n)) return "mc";
  if (n.startsWith("3")) return "amex";
  if (/^6/.test(n)) return "elo";
  return "card";
}

function generateToken(cardNumber: string): string {
  const cleaned = cardNumber.replace(/\D/g, "");
  const last4 = cleaned.slice(-4);
  const brand = detectBrand(cleaned);
  return `tok-${brand}-${last4}`;
}

function formatCardNumber(value: string): string {
  return value
    .replace(/\D/g, "")
    .slice(0, 16)
    .replace(/(.{4})/g, "$1 ")
    .trim();
}

function formatExpiry(value: string): string {
  const cleaned = value.replace(/\D/g, "").slice(0, 4);
  if (cleaned.length >= 3) return cleaned.slice(0, 2) + "/" + cleaned.slice(2);
  return cleaned;
}

const BRAND_LABEL: Record<string, string> = {
  visa: "Visa",
  mc: "Mastercard",
  amex: "Amex",
  elo: "Elo",
  card: "Cartão",
};

export function StepPaymentForm({
  amount,
  currency,
  isSubmitting,
  onSubmit,
}: StepPaymentFormProps) {
  const { items } = useCart();

  const {
    register,
    control,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<CardFormValues>({
    mode: "onChange",
    defaultValues: {
      card_number: "",
      cardholder_name: "",
      expiry: "",
      cvv: "",
      customer_email: "",
    },
  });

  const cardNumber = watch("card_number");
  const cardholderName = watch("cardholder_name");
  const expiry = watch("expiry");
  const cvv = watch("cvv");

  const isCardComplete =
    cardNumber.replace(/\D/g, "").length === 16 &&
    cardholderName.trim().length > 1 &&
    expiry.length === 5 &&
    cvv.length >= 3;

  const brand = detectBrand(cardNumber);

  function handleFormSubmit(data: CardFormValues) {
    onSubmit({
      card_token_id: generateToken(data.card_number),
      customer_email: data.customer_email || undefined,
    });
  }

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} noValidate className="space-y-5">
      {/* Cart summary */}
      <div className="bg-white rounded-card border border-border p-5 space-y-2.5">
        {items.map(({ product, quantity }) => (
          <div key={product.id} className="flex items-center justify-between text-sm">
            <span className="text-text-secondary">
              {product.emoji} {product.name}
              {quantity > 1 && (
                <span className="ml-1 text-text-secondary/70">× {quantity}</span>
              )}
            </span>
            <span className="font-medium text-text-primary tabular-nums">
              {formatCurrency(product.price * quantity, currency)}
            </span>
          </div>
        ))}
        <div className="border-t border-border pt-2.5 flex justify-between items-baseline">
          <span className="font-semibold text-text-primary text-sm">Total</span>
          <span className="text-xl font-bold text-text-primary tabular-nums">
            {formatCurrency(amount, currency)}
          </span>
        </div>
      </div>

      {/* Card form */}
      <div className="space-y-4">
        <div className="flex items-center gap-2 text-sm font-medium text-text-primary">
          <CreditCard className="w-4 h-4 text-primary" />
          Dados do cartão
        </div>

        {/* Card number */}
        <div className="space-y-1.5">
          <Label htmlFor="card_number" className="text-xs font-medium text-text-secondary">
            Número do cartão
          </Label>
          <div className="relative">
            <Controller
              name="card_number"
              control={control}
              rules={{
                required: true,
                validate: (v) => v.replace(/\D/g, "").length === 16,
              }}
              render={({ field }) => (
                <Input
                  {...field}
                  id="card_number"
                  inputMode="numeric"
                  placeholder="1234 5678 9012 3456"
                  maxLength={19}
                  onChange={(e) => field.onChange(formatCardNumber(e.target.value))}
                  className="h-[var(--height-input)] rounded-input border-input-border px-4 pr-16 text-base font-mono tracking-wider"
                />
              )}
            />
            {brand !== "card" && cardNumber.length > 0 && (
              <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-text-secondary">
                {BRAND_LABEL[brand]}
              </span>
            )}
          </div>
        </div>

        {/* Cardholder name */}
        <div className="space-y-1.5">
          <Label htmlFor="cardholder_name" className="text-xs font-medium text-text-secondary">
            Nome no cartão
          </Label>
          <Input
            id="cardholder_name"
            placeholder="NOME SOBRENOME"
            autoComplete="cc-name"
            className="h-[var(--height-input)] rounded-input border-input-border px-4 text-base uppercase placeholder:normal-case"
            {...register("cardholder_name", { required: true, minLength: 2 })}
          />
        </div>

        {/* Expiry + CVV */}
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1.5">
            <Label htmlFor="expiry" className="text-xs font-medium text-text-secondary">
              Validade
            </Label>
            <Controller
              name="expiry"
              control={control}
              rules={{ required: true, validate: (v) => v.length === 5 }}
              render={({ field }) => (
                <Input
                  {...field}
                  id="expiry"
                  inputMode="numeric"
                  placeholder="MM/AA"
                  maxLength={5}
                  onChange={(e) => field.onChange(formatExpiry(e.target.value))}
                  className="h-[var(--height-input)] rounded-input border-input-border px-4 text-base font-mono"
                />
              )}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="cvv" className="text-xs font-medium text-text-secondary">
              CVV
            </Label>
            <div className="relative">
              <Input
                id="cvv"
                inputMode="numeric"
                placeholder="123"
                maxLength={4}
                className="h-[var(--height-input)] rounded-input border-input-border px-4 text-base font-mono"
                {...register("cvv", { required: true, minLength: 3 })}
              />
              <Lock className="absolute right-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-text-secondary/40" />
            </div>
          </div>
        </div>

        {/* Email (optional) */}
        <div className="space-y-1.5">
          <Label htmlFor="customer_email" className="text-xs font-medium text-text-secondary">
            E-mail{" "}
            <span className="font-normal text-text-secondary/70">(opcional)</span>
          </Label>
          <Input
            id="customer_email"
            type="email"
            placeholder="cliente@exemplo.com"
            className="h-[var(--height-input)] rounded-input border-input-border px-4 text-base"
            {...register("customer_email", {
              pattern: {
                value: /^$|^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                message: "E-mail inválido",
              },
            })}
          />
          {errors.customer_email && (
            <p className="text-xs text-error">{errors.customer_email.message}</p>
          )}
        </div>
      </div>

      <Button
        type="submit"
        className="w-full bg-primary hover:bg-primary/90 text-white font-semibold rounded-btn"
        style={{ height: "var(--height-btn)" }}
        disabled={!isCardComplete || isSubmitting}
      >
        {isSubmitting ? "Processando..." : `Pagar ${formatCurrency(amount, currency)}`}
      </Button>

      <div className="flex justify-center">
        <SecurityBadge />
      </div>
    </form>
  );
}
