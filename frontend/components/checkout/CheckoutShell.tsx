"use client";

import { useState, useEffect, useCallback } from "react";
import { StepPaymentForm } from "./StepPaymentForm";
import { StepProcessing } from "./StepProcessing";
import { StepSuccess } from "./StepSuccess";
import { StepError } from "./StepError";
import { useCreatePayment, useGetPayment, usePaymentTimeout } from "@/hooks/usePayment";
import { getOrCreateCheckoutKey, clearCheckoutSession } from "@/lib/utils/idempotency";
import { TERMINAL_STATUSES } from "@/types/payment";
import type { PaymentResponse } from "@/types/payment";

type CheckoutStep = "FORM" | "PROCESSING" | "SUCCESS" | "ERROR";
type ErrorType = "DECLINED" | "FAILED" | "NETWORK" | "UNKNOWN_TIMEOUT";


interface CheckoutShellProps {
  amount: number;
  currency: string;
  onComplete?: () => void;
  onPaymentStarted?: () => void;
  onPaymentFailed?: () => void;
  onRetry?: () => void;
}

export function CheckoutShell({ amount, currency, onComplete, onPaymentStarted, onPaymentFailed, onRetry }: CheckoutShellProps) {
  const [step, setStep] = useState<CheckoutStep>("FORM");
  const [transactionId, setTransactionId] = useState<string | null>(null);
  const [awaitingAuth, setAwaitingAuth] = useState(false);
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
      onPaymentFailed?.();
      setStep("ERROR");
      return;
    }

    if (TERMINAL_STATUSES.includes(data.status)) {
      if (data.status === "COMPLETED") {
        clearCheckoutSession();
        onComplete?.();
        setSuccessData(data);
        setStep("SUCCESS");
      } else {
        clearCheckoutSession();
        setErrorType(data.status === "DECLINED" ? "DECLINED" : "FAILED");
        setErrorMessage(data.message);
        onPaymentFailed?.();
        setStep("ERROR");
      }
    }
  }, [pollQuery.data, timedOut]);

  const handleFormSubmit = useCallback(
    async (formData: { card_token_id: string; customer_email?: string }) => {
      const idempotencyKey = getOrCreateCheckoutKey();
      setStep("PROCESSING");
      onPaymentStarted?.();

      try {
        const result = await createMutation.mutateAsync({
          request: {
            amount,
            currency,
            idempotency_key: idempotencyKey,
            payment_method: { card_token_id: formData.card_token_id },
            customer_email: formData.customer_email || undefined,
          },
          idempotencyKey,
        });

        if (result.status === "CHALLENGE_PENDING") {
          sessionStorage.setItem("checkout_transaction_id", result.transaction_id);
          if (result.challenge_id) {
            sessionStorage.setItem("checkout_challenge_id", result.challenge_id);
          }
          setTransactionId(result.transaction_id);
          setAwaitingAuth(true);
          setPollStart(Date.now());
        } else if (TERMINAL_STATUSES.includes(result.status)) {
          if (result.status === "COMPLETED") {
            clearCheckoutSession();
            onComplete?.();
            setSuccessData(result);
            setStep("SUCCESS");
          } else {
            clearCheckoutSession();
            setErrorType(result.status === "DECLINED" ? "DECLINED" : "FAILED");
            setErrorMessage(result.message);
            onPaymentFailed?.();
            setStep("ERROR");
          }
        } else {
          setTransactionId(result.transaction_id);
          setPollStart(Date.now());
        }
      } catch (err: unknown) {
        clearCheckoutSession();
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
        onPaymentFailed?.();
        setStep("ERROR");
      }
    },
    [amount, currency, createMutation]
  );

  const handleRetry = useCallback(() => {
    setStep("FORM");
    setErrorMessage(undefined);
    onRetry?.();
  }, [onRetry]);

  return (
    <div className="w-full">

      {step === "FORM" && (
        <StepPaymentForm
          amount={amount}
          currency={currency}
          onSubmit={handleFormSubmit}
          isSubmitting={createMutation.isPending}
        />
      )}

      {step === "PROCESSING" && (
        <StepProcessing
          message={awaitingAuth ? "Aguardando autenticação..." : "Processando pagamento..."}
        />
      )}

      {step === "SUCCESS" && successData && (
        <StepSuccess payment={successData} />
      )}

      {step === "ERROR" && (
        <StepError
          errorType={errorType}
          message={errorMessage}
          onRetry={handleRetry}
        />
      )}
    </div>
  );
}
