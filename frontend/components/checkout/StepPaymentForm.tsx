"use client";

import { useForm } from "react-hook-form";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import { SecurityBadge } from "@/components/shared/SecurityBadge";
import { formatCurrency } from "@/lib/utils/formatters";

interface FormValues {
  card_token_id: string;
  customer_email: string;
}

interface StepPaymentFormProps {
  amount: number;
  currency: string;
  cardTokenId?: string;
  maskedCard?: string;
  isSubmitting: boolean;
  onSubmit: (data: { card_token_id: string; customer_email?: string }) => void;
}

export function StepPaymentForm({
  amount,
  currency,
  cardTokenId,
  maskedCard,
  isSubmitting,
  onSubmit,
}: StepPaymentFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
  } = useForm<FormValues>({
    mode: "onChange",
    defaultValues: {
      card_token_id: cardTokenId ?? "",
      customer_email: "",
    },
  });

  function handleFormSubmit(data: FormValues) {
    onSubmit({
      card_token_id: data.card_token_id,
      customer_email: data.customer_email || undefined,
    });
  }

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} noValidate className="space-y-6">
      <Card className="rounded-card border-border shadow-sm">
        <CardContent className="p-8 space-y-1">
          <p className="text-sm text-text-secondary">Valor a pagar</p>
          <p className="text-2xl font-semibold text-text-primary">
            {formatCurrency(amount, currency)}
          </p>
          {maskedCard && (
            <p className="text-sm text-text-secondary mt-1">
              Cartão: <span className="font-medium text-text-primary">{maskedCard}</span>
            </p>
          )}
        </CardContent>
      </Card>

      <div className="space-y-5">
        <div className="space-y-2">
          <Label htmlFor="card_token_id">Token do cartão</Label>
          <Input
            id="card_token_id"
            placeholder="tok_..."
            readOnly={!!cardTokenId}
            aria-invalid={!!errors.card_token_id}
            className="h-[var(--height-input)] rounded-input border-input-border px-4 text-base"
            {...register("card_token_id", {
              required: "Campo obrigatório",
              maxLength: { value: 100, message: "Token muito longo" },
            })}
          />
          {errors.card_token_id && (
            <p className="text-xs text-error">{errors.card_token_id.message}</p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="customer_email">
            E-mail <span className="text-text-secondary font-normal">(opcional)</span>
          </Label>
          <Input
            id="customer_email"
            type="email"
            placeholder="cliente@exemplo.com"
            aria-invalid={!!errors.customer_email}
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

      <div className="space-y-4">
        <Button
          type="submit"
          className="w-full bg-primary hover:bg-primary/90 text-white font-semibold rounded-btn"
          style={{ height: "var(--height-btn)" }}
          disabled={!isValid || isSubmitting}
        >
          {isSubmitting ? "Processando..." : "Pagar"}
        </Button>

        <div className="flex justify-center">
          <SecurityBadge />
        </div>
      </div>
    </form>
  );
}
