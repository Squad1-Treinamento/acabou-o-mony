# Task 05: Add Helper Functions for Metrics

**Status**: Ready to implement  
**Estimated Time**: 30 minutes  
**Dependencies**: None (standalone utility functions)

---

## Goal

Create utility functions for calculating dashboard metrics (revenue, success rate, trends, etc.) from transaction data. These functions will be used by the Dashboard component.

---

## Context

The Dashboard needs to calculate various metrics from raw transaction data:
- Filter transactions by date (today, yesterday, week, month)
- Calculate total revenue (sum of COMPLETED transactions)
- Calculate success rate (percentage of COMPLETED transactions)
- Calculate average transaction value
- Calculate trends (compare today vs yesterday)
- Format currency values

These calculations should be in a separate utility file for:
- **Reusability**: Can be used by other components
- **Testability**: Easy to unit test
- **Maintainability**: Business logic separated from UI

---

## Files to Create

### 1. `frontend/src/utils/metrics.ts`

Utility functions for dashboard metrics calculation.

```typescript
import type { TransactionDetails } from '../types/payment';

/**
 * Filter transactions by date period
 */
export function filterByDate(
  transactions: TransactionDetails[],
  period: 'today' | 'yesterday' | 'week' | 'month'
): TransactionDetails[] {
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const startOfYesterday = new Date(startOfToday);
  startOfYesterday.setDate(startOfYesterday.getDate() - 1);

  return transactions.filter((tx) => {
    const txDate = new Date(tx.created_at);

    switch (period) {
      case 'today':
        return txDate >= startOfToday;
      
      case 'yesterday':
        return txDate >= startOfYesterday && txDate < startOfToday;
      
      case 'week':
        const weekAgo = new Date(now);
        weekAgo.setDate(weekAgo.getDate() - 7);
        return txDate >= weekAgo;
      
      case 'month':
        const monthAgo = new Date(now);
        monthAgo.setMonth(monthAgo.getMonth() - 1);
        return txDate >= monthAgo;
      
      default:
        return false;
    }
  });
}

/**
 * Calculate total revenue from COMPLETED transactions
 * @returns Revenue in cents
 */
export function calculateRevenue(transactions: TransactionDetails[]): number {
  return transactions
    .filter((tx) => tx.status === 'COMPLETED')
    .reduce((sum, tx) => sum + tx.amount, 0);
}

/**
 * Calculate success rate (percentage of COMPLETED transactions)
 * @returns Success rate as percentage (0-100)
 */
export function calculateSuccessRate(transactions: TransactionDetails[]): number {
  if (transactions.length === 0) return 0;

  const completed = transactions.filter((tx) => tx.status === 'COMPLETED').length;
  return (completed / transactions.length) * 100;
}

/**
 * Calculate average transaction value from COMPLETED transactions
 * @returns Average value in cents
 */
export function calculateAverage(transactions: TransactionDetails[]): number {
  const completed = transactions.filter((tx) => tx.status === 'COMPLETED');
  if (completed.length === 0) return 0;

  const total = calculateRevenue(completed);
  return total / completed.length;
}

/**
 * Calculate trend between current and previous value
 * @returns Trend object with formatted value and direction
 */
export function calculateTrend(
  current: number,
  previous: number
): {
  value: string;
  trend: 'up' | 'down' | 'neutral';
} {
  // Handle edge case: no previous data
  if (previous === 0) {
    if (current === 0) {
      return { value: '0%', trend: 'neutral' };
    }
    return { value: '+100%', trend: 'up' };
  }

  // Calculate percentage change
  const change = ((current - previous) / previous) * 100;

  // Determine trend direction
  let trend: 'up' | 'down' | 'neutral';
  if (change > 0) {
    trend = 'up';
  } else if (change < 0) {
    trend = 'down';
  } else {
    trend = 'neutral';
  }

  // Format value
  const formattedChange = change >= 0 ? `+${change.toFixed(1)}%` : `${change.toFixed(1)}%`;

  return {
    value: formattedChange,
    trend,
  };
}

/**
 * Format amount in cents to currency string
 */
export function formatCurrency(amount: number, currency: string): string {
  const value = (amount / 100).toFixed(2);

  switch (currency) {
    case 'BRL':
      return `R$ ${value}`;
    case 'USD':
      return `$ ${value}`;
    case 'EUR':
      return `€ ${value}`;
    default:
      return `${value} ${currency}`;
  }
}

/**
 * Format date to relative time (e.g., "2 min ago", "5 hours ago")
 */
export function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);

  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins} min ago`;
  if (diffMins < 1440) return `${Math.floor(diffMins / 60)} hours ago`;
  
  // More than 24 hours: show date
  return date.toLocaleDateString();
}

/**
 * Get status badge class name
 */
export function getStatusBadgeClass(status: string): string {
  switch (status) {
    case 'COMPLETED':
      return 'nu-badge-success';
    case 'DECLINED':
    case 'FAILED':
      return 'nu-badge-error';
    case 'PROCESSING':
    case 'CHALLENGE_PENDING':
      return 'nu-badge-warning';
    default:
      return 'nu-badge-neutral';
  }
}
```

---

## Usage Examples

### In Dashboard Component

```typescript
import {
  filterByDate,
  calculateRevenue,
  calculateSuccessRate,
  calculateAverage,
  calculateTrend,
  formatCurrency,
} from '../utils/metrics';

// Fetch transactions
const transactions = await apiClient.getTransactions();

// Filter by date
const today = filterByDate(transactions, 'today');
const yesterday = filterByDate(transactions, 'yesterday');

// Calculate metrics
const todayRevenue = calculateRevenue(today);
const todayCount = today.length;
const successRate = calculateSuccessRate(today);
const avgTransaction = calculateAverage(today);

// Calculate trends
const yesterdayRevenue = calculateRevenue(yesterday);
const revenueTrend = calculateTrend(todayRevenue, yesterdayRevenue);

// Format for display
const formattedRevenue = formatCurrency(todayRevenue, 'BRL');
// => "R$ 1,234.56"
```

---

## Test Cases (for future unit tests)

### filterByDate

```typescript
// Test: Filter today's transactions
const today = filterByDate(transactions, 'today');
// Should only include transactions from today

// Test: Filter yesterday's transactions
const yesterday = filterByDate(transactions, 'yesterday');
// Should only include transactions from yesterday

// Test: Empty array
const empty = filterByDate([], 'today');
// Should return []
```

### calculateRevenue

```typescript
// Test: Calculate revenue from COMPLETED transactions
const revenue = calculateRevenue([
  { status: 'COMPLETED', amount: 10000 },
  { status: 'COMPLETED', amount: 5000 },
  { status: 'DECLINED', amount: 3000 }, // Should be excluded
]);
// Should return 15000

// Test: Empty array
const zero = calculateRevenue([]);
// Should return 0
```

### calculateSuccessRate

```typescript
// Test: Calculate success rate
const rate = calculateSuccessRate([
  { status: 'COMPLETED' },
  { status: 'COMPLETED' },
  { status: 'DECLINED' },
  { status: 'FAILED' },
]);
// Shoout of 4)

// Test: All completed
const perfect = calculateSuccessRate([
  { status: 'COMPLETED' },
  { status: 'COMPLETED' },
uld return 50 (2 ]);
// Should return 100

// Test: Empty array
const noData = calculateSuccessRate([]);
// Should return 0
```

### calculateTrend

```typescript
// Test: Positive trend
const upTrend = calculateTrend(120, 100);
// Should return { value: '+20.0%', trend: 'up' }

// Test: Negative trend
const downTrend = calculateTrend(80, 100);
// Should return { value: '-20.0%', trend: 'down' }

// Test: No change
const neutral = calculateTrend(100, 100);
// Should return { value: '0.0%', trend: 'neutral' }

// Test: No previous data
const noPrevious = calculateTrend(100, 0);
// Should return { value: '+100%', trend: 'up' }
```

### formatCurrency

```typescript
// Test: BRL formatting
const brl = formatCurrency(10000, 'BRL');
// Should return "R$ 100.00"

// Test: USD formatting
const usd = formatCurrency(10000, 'USD');
// Should return "$ 100.00"

// Test: Zero amount
const zero = formatCurrency(0, 'BRL');
// Should return "R$ 0.00"
```

---

## Acceptance Criteria

- [ ] `metrics.ts` file created in `utils/` directory
- [ ] All functions are exported
- [ ] All functions have TypeScript types
- [ ] Functions handle edge cases (empty arrays, zero values)
- [ ] Functions are pure (no side effects)
- [ ] Functions are well-documented (JSDoc comments)
- [ ] No console errors when importing
- [ ] Dashboard component can import and use functions

---

## Testing Checklist

### Manual Testing

1. **Import in Dashboard**
   - [ ] Import functions in Dashboard.tsx
   - [ ] No TypeScript errors
   - [ ] No console errors

2. **Calculate Metrics**
   - [ ] Revenue calculates correctly
   - [ ] Success rate calculates correctly
   - [ ] Average calculates correctly
   - [ ] Trends calculate correctly

3. **Edge Cases**
   - [ ] Empty transaction array
   - [ ] All transactions COMPLETED
   - [ ] All transactions FAILED
   - [ ] Zero previous value (trend)
   - [ ] Negative amounts (should not happen, but handle gracefully)

4. **Format Functions**
   - [ ] Currency formats correctly
   - [ ] Relative time formats correctly
   - [ ] Status badge classes are correct

---

## Validation Commands

```bash
# Type checking
npm run type-check

# Linting
npm run lint

# Development server (to test in Dashboard)
npm run dev
```

---

## Notes

- These functions are pure (no side effects)
- They can be easily unit tested (future enhancement)
- They follow functional programming principles
- They handle edge cases gracefully
- They are reusable across components

---

## Optional Enhancements (Out of Scope for MVP)

If you want to enhance this later:

1. **Unit Tests**: Add Jest/Vitest tests for each function
2. **Memoization**: Cache expensive calculations
3. **Date Range Picker**: Add custom date range filtering
4. **Moedian, mode, percentiles
5. **Currency Localizationre Metrics**: Add m**: Use Intl.NumberFormat for proper formatting
6. **Time Zones**: Handle different time zones correctly

---

**Next Task**: `06-default-landing-page.md`
