import { Lock } from "lucide-react";

export function SecurityBadge() {
  return (
    <div className="flex items-center gap-1.5 text-text-secondary">
      <Lock className="w-4 h-4" />
      <span className="text-[13px] font-medium">Pagamento seguro</span>
    </div>
  );
}
