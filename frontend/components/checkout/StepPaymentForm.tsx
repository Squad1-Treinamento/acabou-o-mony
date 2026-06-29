"use client";

import { useForm, Controller } from "react-hook-form";
import { Lock, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { formatCurrency } from "@/lib/utils/formatters";
import { AcceptedBrands } from "./CardBrandIcons";
import type { CardBrandKey } from "./CardBrandIcons";

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

function detectBrand(number: string): CardBrandKey | "" {
  const n = number.replace(/\D/g, "");
  if (n.startsWith("4")) return "visa";
  if (/^5[1-5]/.test(n)) return "mastercard";
  if (n.startsWith("3")) return "amex";
  if (/^6/.test(n)) return "elo";
  return "";
}

function generateToken(cardNumber: string): string {
  const cleaned = cardNumber.replace(/\D/g, "");
  const last4 = cleaned.slice(-4);
  const brand = detectBrand(cleaned) || "card";
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

const inputCls =
  "h-11 rounded-xl border-slate-200 bg-[#F5F5F7] px-3.5 text-sm text-slate-900 " +
  "placeholder:text-slate-300 focus-visible:border-[#0D2B1E] focus-visible:bg-white " +
  "focus-visible:ring-2 focus-visible:ring-[#0D2B1E]/10 transition-all";

const labelCls = "text-xs font-medium text-slate-500 mb-1.5 block";

export function StepPaymentForm({
  amount,
  currency,
  isSubmitting,
  onSubmit,
}: StepPaymentFormProps) {
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
    <form onSubmit={handleSubmit(handleFormSubmit)} noValidate className="space-y-0">

      {/* Heading + accepted brands */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg flex items-center justify-center" style={{ background: "rgba(13,43,30,0.08)" }}>
            <Lock className="w-3.5 h-3.5" style={{ color: "#0D2B1E" }} />
          </div>
          <h2 className="text-sm font-semibold text-slate-900">Informações de pagamento</h2>
        </div>
        <AcceptedBrands activeBrand={brand} />
      </div>

      <div className="space-y-4">

        {/* Card number */}
        <div>
          <label htmlFor="card_number" className={labelCls}>
            Número do cartão
          </label>
          <Controller
            name="card_number"
            control={control}
            rules={{ required: true, validate: (v) => v.replace(/\D/g, "").length === 16 }}
            render={({ field }) => (
              <Input
                {...field}
                id="card_number"
                inputMode="numeric"
                placeholder="1234 5678 9012 3456"
                maxLength={19}
                autoComplete="cc-number"
                onChange={(e) => field.onChange(formatCardNumber(e.target.value))}
                className={`${inputCls} font-mono tracking-wider`}
              />
            )}
          />
        </div>

        {/* Expiry + CVV */}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="expiry" className={labelCls}>
              Validade
            </label>
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
                  autoComplete="cc-exp"
                  onChange={(e) => field.onChange(formatExpiry(e.target.value))}
                  className={`${inputCls} font-mono`}
                />
              )}
            />
          </div>
          <div>
            <label htmlFor="cvv" className={labelCls}>
              CVV
            </label>
            <div className="relative">
              <Input
                id="cvv"
                inputMode="numeric"
                placeholder="•••"
                maxLength={4}
                autoComplete="cc-csc"
                className={`${inputCls} font-mono pr-10`}
                {...register("cvv", { required: true, minLength: 3 })}
              />
              <Lock className="absolute right-3.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-300" />
            </div>
          </div>
        </div>

        {/* Cardholder name */}
        <div>
          <label htmlFor="cardholder_name" className={labelCls}>
            Nome no cartão
          </label>
          <Input
            id="cardholder_name"
            placeholder="NOME SOBRENOME"
            autoComplete="cc-name"
            className={`${inputCls} uppercase placeholder:normal-case`}
            {...register("cardholder_name", { required: true, minLength: 2 })}
          />
        </div>

        {/* Email */}
        <div>
          <label htmlFor="customer_email" className={labelCls}>
            E-mail{" "}
            <span className="font-normal text-slate-400">(opcional)</span>
          </label>
          <Input
            id="customer_email"
            type="email"
            placeholder="seu@email.com"
            autoComplete="email"
            className={inputCls}
            {...register("customer_email", {
              pattern: {
                value: /^$|^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                message: "E-mail inválido",
              },
            })}
          />
          {errors.customer_email && (
            <p className="text-xs text-red-500 mt-1">{errors.customer_email.message}</p>
          )}
        </div>

      </div>

      {/* CTA */}
      <Button
        type="submit"
        className="w-full h-12 rounded-xl mt-6 font-semibold text-sm tracking-tight gap-2"
        style={{ background: "linear-gradient(135deg, #0D2B1E 0%, #1a4532 100%)" }}
        disabled={!isCardComplete || isSubmitting}
      >
        <Lock className="w-3.5 h-3.5 opacity-70" />
        {isSubmitting
          ? "Processando..."
          : `Pagar ${formatCurrency(amount, currency)}`}
      </Button>

      {/* Trust line */}
      <div className="mt-4 flex items-center justify-center gap-1.5 text-slate-400">
        <ShieldCheck className="w-3 h-3" />
        <span className="text-[11px]">Seus dados estão protegidos com criptografia de 256 bits</span>
      </div>

    </form>
  );
}
