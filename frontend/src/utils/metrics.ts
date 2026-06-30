import type { TransactionDetails } from '../types/payment';

/**
 * Filter transactions by date range
 */
export function filterByDate(
  transactions: TransactionDetails[],
  range: 'today' | 'yesterday' | 'last7days' | 'last30days'
): TransactionDetails[] {
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const startOfYesterday = new Date(startOfToday);
  startOfYesterday.setDate(startOfYesterday.getDate() - 1);

  switch (range) {
    case 'today':
      return transactions.filter((tx) => {
        const txDate = new Date(tx.created_at);
        return txDate >= startOfToday;
      });

    case 'yesterday':
      return transactions.filter((tx) => {
        const txDate = new Date(tx.created_at);
        return txDate >= startOfYesterday && txDate < startOfToday;
      });

    case 'last7days': {
      const sevenDaysAgo = new Date(startOfToday);
      sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
      return transactions.filter((tx) => {
        const txDate = new Date(tx.created_at);
        return txDate >= sevenDaysAgo;
      });
    }

    case 'last30days': {
      const thirtyDaysAgo = new Date(startOfToday);
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
      return transactions.filter((tx) => {
        const txDate = new Date(tx.created_at);
        return txDate >= thirtyDaysAgo;
      });
    }

    default:
      return transactions;
  }
}

/**
 * Calculate total revenue from completed transactions
 * @param transactions - Array of transactions
 * @returns Total revenue in cents
 */
export function calculateRevenue(transactions: TransactionDetails[]): number {
  return transactions
    .filter((tx) => tx.status === 'COMPLETED')
    .reduce((sum, tx) => sum + tx.amount, 0);
}

/**
 * Calculate success rate (percentage of completed transactions)
 * @param transactions - Array of transactions
 * @returns Success rate as percentage (0-100)
 */
export function calculateSuccessRate(transactions: TransactionDetails[]): number {
  if (transactions.length === 0) return 0;

  const completedCount = transactions.filter((tx) => tx.status === 'COMPLETED').length;
  return (completedCount / transactions.length) * 100;
}

/**
 * Calculate average transaction value from completed transactions
 * @param transactions - Array of transactions
 * @returns Average transaction value in cents
 */
export function calculateAverage(transactions: TransactionDetails[]): number {
  const completed = transactions.filter((tx) => tx.status === 'COMPLETED');
  if (completed.length === 0) return 0;

  const total = completed.reduce((sum, tx) => sum + tx.amount, 0);
  return total / completed.length;
}

/**
 * Calculate trend between two values
 * @param current - Current value
 * @param previous - Previous value
 * @returns Trend object with percentage change and direction
 */
export function calculateTrend(
  current: number,
  previous: number
): { value: string; trend: 'up' | 'down' | 'neutral' } {
  if (previous === 0) {
    if (current === 0) {
      return { value: '0%', trend: 'neutral' };
    }
    return { value: '100%', trend: 'up' };
  }

  const percentChange = ((current - previous) / previous) * 100;
  const absChange = Math.abs(percentChange);

  // Consider changes less than 0.1% as neutral
  if (absChange < 0.1) {
    return { value: '0%', trend: 'neutral' };
  }

  const trend = percentChange > 0 ? 'up' : 'down';
  const value = `${absChange.toFixed(1)}%`;

  return { value, trend };
}

/**
 * Format currency value from cents to display format
 * @param amountInCents - Amount in cents
 * @param currency - Currency code (BRL, USD, etc.)
 * @returns Formatted currency string
 */
export function formatCurrency(amountInCents: number, currency: string): string {
  const amount = amountInCents / 100;

  const currencyMap: Record<string, { locale: string; code: string }> = {
    BRL: { locale: 'pt-BR', code: 'BRL' },
    USD: { locale: 'en-US', code: 'USD' },
    EUR: { locale: 'de-DE', code: 'EUR' },
  };

  const config = currencyMap[currency] || { locale: 'en-US', code: currency };

  return new Intl.NumberFormat(config.locale, {
    style: 'currency',
    currency: config.code,
  }).format(amount);
}
