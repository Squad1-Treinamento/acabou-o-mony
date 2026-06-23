import { Badge } from "@/components/ui/badge";
import type { PaymentStatus } from "@/types/payment";

interface StatusConfig {
  label: string;
  className: string;
}

const STATUS_CONFIG: Record<PaymentStatus, StatusConfig> = {
  COMPLETED: {
    label: "Concluído",
    className: "bg-[#EAF7F0] text-[#1F8F53] border-[#1F8F53]/20 hover:bg-[#EAF7F0]",
  },
  DECLINED: {
    label: "Recusado",
    className: "bg-[#FEF2F2] text-[#DC2626] border-[#DC2626]/20 hover:bg-[#FEF2F2]",
  },
  FAILED: {
    label: "Falhou",
    className: "bg-[#FEF2F2] text-[#DC2626] border-[#DC2626]/20 hover:bg-[#FEF2F2]",
  },
  PROCESSING: {
    label: "Processando",
    className: "bg-primary/10 text-primary border-primary/20 hover:bg-primary/10",
  },
  AUTHENTICATED: {
    label: "Autenticado",
    className: "bg-primary/10 text-primary border-primary/20 hover:bg-primary/10",
  },
  UNKNOWN: {
    label: "Desconhecido",
    className: "bg-[#FFFBEB] text-[#D97706] border-[#D97706]/20 hover:bg-[#FFFBEB]",
  },
  CHALLENGE_PENDING: {
    label: "Aguard. 3DS",
    className: "bg-[#F0F2F4] text-[#6B7280] border-[#E5E7EB] hover:bg-[#F0F2F4]",
  },
  CREATED: {
    label: "Criado",
    className: "bg-[#F0F2F4] text-[#6B7280] border-[#E5E7EB] hover:bg-[#F0F2F4]",
  },
  VALIDATED: {
    label: "Validado",
    className: "bg-[#F0F2F4] text-[#6B7280] border-[#E5E7EB] hover:bg-[#F0F2F4]",
  },
};

interface StatusBadgeProps {
  status: PaymentStatus;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = STATUS_CONFIG[status] ?? STATUS_CONFIG.UNKNOWN;
  return (
    <Badge variant="outline" className={`text-xs font-medium ${config.className}`}>
      {config.label}
    </Badge>
  );
}
