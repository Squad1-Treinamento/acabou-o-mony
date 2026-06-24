"use client";

import { useEffect } from "react";
import { LoadingSpinner } from "@/components/shared/LoadingSpinner";
import { ErrorState } from "@/components/shared/ErrorState";

interface StepThreeDsProps {
  acsUrl: string | null;
}

const REDIRECT_DELAY_MS = 500;

function isSafeAcsUrl(url: string): boolean {
  try {
    const parsed = new URL(url);
    return parsed.protocol === "https:";
  } catch {
    return false;
  }
}

export function StepThreeDs({ acsUrl }: StepThreeDsProps) {
  const safeUrl = acsUrl && isSafeAcsUrl(acsUrl) ? acsUrl : null;

  useEffect(() => {
    if (!safeUrl) return;

    const timer = setTimeout(() => {
      window.location.href = safeUrl;
    }, REDIRECT_DELAY_MS);

    return () => clearTimeout(timer);
  }, [safeUrl]);

  if (!safeUrl) {
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
