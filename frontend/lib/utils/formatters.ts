export function formatCurrency(amount: number, currency: string): string {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
  }).format(amount / 100);
}

export function formatRelativeTime(isoString: string): string {
  const diff = Date.now() - new Date(isoString).getTime();
  const rtf = new Intl.RelativeTimeFormat("pt-BR", { numeric: "auto" });

  const seconds = Math.round(diff / 1000);
  if (Math.abs(seconds) < 60) return rtf.format(-seconds, "second");

  const minutes = Math.round(diff / 60_000);
  if (Math.abs(minutes) < 60) return rtf.format(-minutes, "minute");

  const hours = Math.round(diff / 3_600_000);
  if (Math.abs(hours) < 24) return rtf.format(-hours, "hour");

  const days = Math.round(diff / 86_400_000);
  return rtf.format(-days, "day");
}

export function truncateUUID(uuid: string): string {
  return uuid.slice(0, 8) + "…";
}
