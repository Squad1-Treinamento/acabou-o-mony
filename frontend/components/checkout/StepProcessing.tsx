import { LoadingSpinner } from "@/components/shared/LoadingSpinner";

interface StepProcessingProps {
  message?: string;
}

export function StepProcessing({ message = "Processando pagamento..." }: StepProcessingProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 gap-6">
      <LoadingSpinner size="lg" />
      <p className="text-sm text-text-secondary text-center">{message}</p>
    </div>
  );
}
