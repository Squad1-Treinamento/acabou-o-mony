interface EmptyStateProps {
  title: string;
  subtitle?: string;
}

export function EmptyState({ title, subtitle }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <p className="text-base font-medium text-text-primary">{title}</p>
      {subtitle && (
        <p className="mt-2 text-sm text-text-secondary">{subtitle}</p>
      )}
    </div>
  );
}
