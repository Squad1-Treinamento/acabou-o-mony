"use client";

import { useState, useEffect, useCallback } from "react";
import { StepIndicator } from "@/components/shared/StepIndicator";
import { StepPaymentForm } from "./StepPaymentForm";
import { StepProcessing } from "./StepProcessing";
import { StepThreeDs } from "./StepThreeDs";
import { StepSuccess } from "./StepSuccess";
import { StepError } from "./StepError";
import { useCreatePayment, useGetPayment, usePaymentTimeout } from "@/hooks/usePayment";
import { getOrCreateCheckoutKey, clearCheckoutSession } from "@/lib/utils/idempotency";
import { TERMINAL_STATUSES } from "@/types/payment";
import type { PaymentResponse } from "@/types/payment";

type CheckoutStep = "FORM" | "PROCESSING" | "THREE_DS" | "SUCCESS" | "ERROR";
type ErrorType = "DECLINED" | "FAILED" | "NETWORK" | "UNKNOWN_TIMEOUT";

const STEP_LABELS = ["Dados", "Processando", "Confirmação"];
const STEP_INDEX: Record<CheckoutStep, number> = {
  FORM: 0,
  PROCESSING: 1,
  THREE_DS: 1,
  SUCCESS: 2,
  ERROR: 2,
};

interface CheckoutShellProps {
  amount: number;
  currency: string;
  cardTokenId?: string;
  maskedCard?: string;
}

export function CheckoutShell({ amount, currency, cardTokenId, maskedCard }: CheckoutShellProps) {
  const [step, setStep] = useState<CheckoutStep>("FORM");
  const [transactionId, setTransactionId] = useState<string | null>(null);
  const [acsUrl, setAcsUrl] = useState<string | null>(null);
  const [errorType, setErrorType] = useState<ErrorType>("FAILED");
  const [errorMessage, setErrorMessage] = useState<string | undefined>();
  const [successData, setSuccessData] = useState<PaymentResponse | null>(null);
  const [pollStart, setPollStart] = useState<number | null>(null);

  const createMutation = useCreatePayment();
  const pollQuery = useGetPayment(
    step === "PROCESSING" ? transactionId : null,
    step === "PROCESSING"
  );
  const timedOut = usePaymentTimeout(pollStart, step === "PROCESSING");

  // Resume after 3DS redirect
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const is3dsComplete = params.get("3ds_complete") === "true";
    const savedId = sessionStorage.getItem("checkout_transaction_id");

    if (is3dsComplete && savedId) {
      setTransactionId(savedId);
      setPollStart(Date.now());
      setStep("PROCESSING");
    }
  }, []);

  // React to polling results
  useEffect(() => {
    const data = pollQuery.data;
    if (!data) return;

    if (timedOut) {
      setErrorType("UNKNOWN_TIMEOUT");
      setStep("ERROR");
      return;
    }

    if (TERMINAL_STATUSES.includes(data.status)) {
      if (data.status === "COMPLETED") {
        clearCheckoutSession();
        setSuccessData(data);
        setStep("SUCCESS");
      } else {
        setErrorType(data.status === "DECLINED" ? "DECLINED" : "FAILED");
        setErrorMessage(data.message);
        setStep("ERROR");
      }
    }
  }, [pollQuery.data, timedOut]);

  const handleFormSubmit = useCallback(
    async (formData: { card_token_id: string; customer_email?: string }) => {
      const idempotencyKey = getOrCreateCheckoutKey();
      setStep("PROCESSING");

      try {
        const result = await createMutation.mutateAsync({
          request: {
            amount,
            currency,
            idempotency_key: idempotencyKey,
            payment_method: { card_token_id: formData.card_token_id, masked_card: maskedCard },
            customer_email: formData.customer_email || undefined,
          },
          idempotencyKey,
        });

        if (result.status === "CHALLENGE_PENDING" && result.acs_url) {
          sessionStorage.setItem("checkout_transaction_id", result.transaction_id);
          if (result.challenge_id) {
            sessionStorage.setItem("checkout_challenge_id", result.challenge_id);
          }
          setAcsUrl(result.acs_url);
          setTransactionId(result.transaction_id);
          setStep("THREE_DS");
        } else if (TERMINAL_STATUSES.includes(result.status)) {
          if (result.status === "COMPLETED") {
            clearCheckoutSession();
            setSuccessData(result);
            setStep("SUCCESS");
          } else {
            setErrorType(result.status === "DECLINED" ? "DECLINED" : "FAILED");
            setErrorMessage(result.message);
            setStep("ERROR");
          }
        } else {
          setTransactionId(result.transaction_id);
          setPollStart(Date.now());
        }
      } catch (err: unknown) {
        const isApiError = err !== null && typeof err === "object" && "status" in err;
        if (isApiError) {
          const status = (err as { status: number }).status;
          if (status === 402 || status === 422) {
            setErrorType("DECLINED");
            setErrorMessage((err as { message?: string }).message);
          } else {
            setErrorType("FAILED");
            setErrorMessage((err as { message?: string }).message);
          }
        } else {
          setErrorType("NETWORK");
          setErrorMessage("Falha na conexão com o servidor. Verifique se o backend está rodando.");
        }
        setStep("ERROR");
      }
    },
    [amount, currency, maskedCard, createMutation]
  );

  const handleRetry = useCallback(() => {
    setStep("FORM");
    setErrorMessage(undefined);
  }, []);

  return (
    <div className="w-full max-w-[480px] mx-auto">
      <StepIndicator steps={STEP_LABELS} currentStep={STEP_INDEX[step]} />

      {step === "FORM" && (
        <StepPaymentForm
          amount={amount}
          currency={currency}
          cardTokenId={cardTokenId}
          maskedCard={maskedCard}
          onSubmit={handleFormSubmit}
          isSubmitting={createMutation.isPending}
        />
      )}

      {step === "PROCESSING" && (
        <StepProcessing
          message={
            transactionId && sessionStorage.getItem("checkout_transaction_id")
              ? "Verificando autenticação..."
              : "Processando pagamento..."
          }
        />
      )}

      {step === "THREE_DS" && acsUrl && (
        <StepThreeDs acsUrl={acsUrl} />
      )}

      {step === "SUCCESS" && successData && (
        <StepSuccess payment={successData} />
      )}

      {step === "ERROR" && (
        <StepError
          errorType={errorType}
          message={errorMessage}
          onRetry={errorType !== "DECLINED" ? handleRetry : undefined}
        />
      )}
    </div>
  );
}
