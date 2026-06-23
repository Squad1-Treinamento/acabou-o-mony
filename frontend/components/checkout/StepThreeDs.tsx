"use client";

import { useEffect } from "react";
import { LoadingSpinner } from "@/components/shared/LoadingSpinner";
import { ErrorState } from "@/components/shared/ErrorState";

interface StepThreeDsProps {
  acsUrl: string | null;
}

const REDIRECT_DELAY_MS = 500;

export function StepThreeDs({ acsUrl }: StepThreeDsProps) {
  useEffect(() => {
    if (!acsUrl) return;

    const timer = setTimeout(() => {
      window.location.href = acsUrl;
    }, REDIRECT_DELAY_MS);

    return () => clearTimeout(timer);
  }, [acsUrl]);

  if (!acsUrl) {
    return (
      <ErrorState message="URL de autenticação não encontrada" />
    );
  }

  return (
    <div className="flex flex-col items-center justify-center py-16 gap-6">
      <LoadingSpinner size="lg" />
      <p className="text-sm text-text-secondary text-center">
        Redirecionando para autenticação...
      </p>
    </div>
  );
}
