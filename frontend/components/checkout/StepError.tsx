import { XCircle, Clock, WifiOff } from "lucide-react";
import { Button } from "@/components/ui/button";

type ErrorType = "DECLINED" | "FAILED" | "NETWORK" | "UNKNOWN_TIMEOUT";

const ERROR_CONFIG: Record<
  ErrorType,
  { icon: typeof XCircle; heading: string; body: string; iconColor: string; bgColor: string }
> = {
  DECLINED: {
    icon: XCircle,
    heading: "Pagamento recusado",
    body: "Verifique os dados do cartão ou tente com outro método de pagamento.",
    iconColor: "text-red-500",
    bgColor: "bg-red-50",
  },
  FAILED: {
    icon: XCircle,
    heading: "Erro no processamento",
    body: "Ocorreu um erro ao processar o pagamento. Tente novamente.",
    iconColor: "text-red-500",
    bgColor: "bg-red-50",
  },
  NETWORK: {
    icon: WifiOff,
    heading: "Erro de conexão",
    body: "Verifique sua conexão com a internet e tente novamente.",
    iconColor: "text-slate-500",
    bgColor: "bg-slate-100",
  },
  UNKNOWN_TIMEOUT: {
    icon: Clock,
    heading: "Pagamento em verificação",
    body: "Seu pagamento está sendo processado. Você receberá uma confirmação em breve.",
    iconColor: "text-amber-600",
    bgColor: "bg-amber-50",
  },
};

interface StepErrorProps {
  errorType: ErrorType;
  message?: string;
  onRetry?: () => void;
}

export function StepError({ errorType, message, onRetry }: StepErrorProps) {
  const { icon: Icon, heading, body, iconColor, bgColor } = ERROR_CONFIG[errorType];

  return (
    <div className="space-y-6">
      <div className="flex flex-col items-center text-center gap-3 py-4">
        <div className={`w-14 h-14 rounded-full ${bgColor} flex items-center justify-center`}>
          <Icon className={`w-7 h-7 ${iconColor}`} strokeWidth={1.75} />
        </div>
        <div>
          <h2 className="text-lg font-semibold text-slate-900">{heading}</h2>
          <p className="text-sm text-slate-500 mt-1">{message ?? body}</p>
        </div>
      </div>

      {onRetry && (
        <Button
          onClick={onRetry}
          className="w-full h-12 rounded-xl font-semibold text-sm text-white"
          style={{ background: "#0D2B1E" }}
        >
          Tentar novamente
        </Button>
      )}
    </div>
  );
}
