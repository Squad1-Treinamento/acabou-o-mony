import { AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";

type ErrorType = "DECLINED" | "FAILED" | "NETWORK" | "UNKNOWN_TIMEOUT";

const ERROR_CONFIG: Record<ErrorType, { heading: string; body: string; showRetry: boolean }> = {
  DECLINED: {
    heading: "Pagamento recusado",
    body: "Verifique os dados do cartão ou tente com outro método.",
    showRetry: false,
  },
  FAILED: {
    heading: "Erro no processamento",
    body: "Tente novamente.",
    showRetry: true,
  },
  NETWORK: {
    heading: "Erro de conexão",
    body: "Verifique sua conexão e tente novamente.",
    showRetry: true,
  },
  UNKNOWN_TIMEOUT: {
    heading: "Pagamento em verificação",
    body: "Você receberá uma confirmação em breve.",
    showRetry: false,
  },
};

interface StepErrorProps {
  errorType: ErrorType;
  message?: string;
  onRetry?: () => void;
}

export function StepError({ errorType, message, onRetry }: StepErrorProps) {
  const config = ERROR_CONFIG[errorType];

  return (
    <div className="flex flex-col items-center gap-6 py-8">
      <div className="flex flex-col items-center gap-3 text-center">
        <AlertCircle className="w-12 h-12 text-error" />
        <h3 className="text-xl font-semibold text-text-primary">{config.heading}</h3>
        <p className="text-sm text-text-secondary">{message ?? config.body}</p>
      </div>

      {config.showRetry && onRetry && (
        <Button
          onClick={onRetry}
          className="bg-primary hover:bg-primary/90 text-white rounded-btn"
          style={{ height: "var(--height-btn)" }}
        >
          Tentar novamente
        </Button>
      )}
    </div>
  );
}
