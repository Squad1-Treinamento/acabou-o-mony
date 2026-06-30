# Task 01: Create Dashboard Component

**Status**: Ready to implement  
**Estimated Time**: 2-3 hours  
**Dependencies**: None (uses existing API client)

---

## Goal

Build the main Dashboard component that serves as the merchant's home page, displaying key performance indicators (KPIs), recent transactions, and quick actions.

---

## Context

The Dashboard is the **first thing merchants see** after logging in. It should provide:
- At-a-glance business metrics (revenue, transaction count, success rate)
- Recent activity (last 5 transactions)
- Quick navigation to common tasks

This transforms the system from a "testing tool" to a "merchant platform."

---

## What You're Building

```
┌─────────────────────────────────────────────────────────┐
│  Dashboard                                              │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ 💰       │ │ 📊       │ │ ✅       │ │ 💳       │  │
│  │ Revenue  │ │ Trans.   │ │ Success  │ │ Avg      │  │
│  │ R$ 1.2K  │ │ 45       │ │ 94.2%    │ │ R$ 27.43 │  │
│  │ +12% ↑   │ │ +8% ↑    │ │ -1.2% ↓  │ │ +3.5% ↑  │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
│                                                         │
│  Recent Transactions                    [View All →]   │
│  ┌─────────────────────────────────────────────────┐   │
│  │ tx_123... │ R$ 100.00 │ COMPLETED │ 2 min ago  │   │
│  │ tx_456... │ R$ 50.00  │ COMPLETED │ 5 min ago  │   │
│  │ tx_789... │ R$ 75.00  │ DECLINED  │ 10 min ago │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  Quick Actions                                          │
│  [View All Transactions]  [Developer Tools]            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## Files to Create

### 1. `frontend/src/components/Dashboard.tsx`

Main dashboard component that orchestrates all sections.

```typescript
import { useState, useEffect } from 'react';
import { apiClient } from '../services/api';
import type { TransactionDetails } from '../types/payment';
import { KPICard } from './KPICard';
import {
  filterByDate,
  calculateRevenue,
  calculateSuccessRate,
  calculateAverage,
  calculateTrend,
  formatCurrency,
} from '../utils/metrics';

interface DashboardMetrics {
  todayRevenue: number;
  todayCount: number;
  successRate: number;
  avgTransaction: number;
  revenueTrend: { value: string; trend: 'up' | 'down' | 'neutral' };
  countTrend: { value: string; trend: 'up' | 'down' | 'neutral' };
  successRateTrend: { value: string; trend: 'up' | 'down' | 'neutral' };
  avgTrend: { value: string; trend: 'up' | 'down' | 'neutral' };
}

interface DashboardProps {
  onNavigateToTransactions: () => void;
  onNavigateToDevTools: () => void;
  onSelectTransaction: (transactionId: string) => void;
}

export function Dashboard({
  onNavigateToTransactions,
  onNavigateToDevTools,
  onSelectTransaction,
}: DashboardProps) {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [recentTransactions, setRecentTransactions] = useState<TransactionDetails[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);

    try {
      const transactions = await apiClient.getTransactions();

      // Filter by date ranges
      const today = filterByDate(transactions, 'today');
      const yesterday = filterByDate(transactions, 'yesterday');

      // Calculates
      const todayRe today's metricvenue = calculateRevenue(today);
      const todayCount = today.length;
      const successRate = calculateSuccessRate(to
      // CalgTransaction = calculateAverday);
      const avage(today);
culate yesterday's metrics for trends
      const yesterdayRevenue = calculateRevenue(yesterday);
      const yesterdayCount = yesterday.length;
      const yesterdaySuccessRate = calculateSuccessRate(yesterday);
      const yesterdayAvg = calculateAverage(yesterday);

      // Calculate trends
      const revenueTrend = calculateTrend(todayRevenue, yesterdayRevenue);
      const countTrend = calculateTrend(todayCount, yesterdayCount);
      const successRateTrend = calculateTrend(successRate, yesterdaySuccessRate);
      const avgTrend = calculateTrend(avgTransaction, yesterdayAvg);

      setMetrics({
        todayRevenue,
        todayCount,
        successRate,
        avgTransaction,
        revenueTrend,
        countTrend,
        successRateTrend,
        avgTrend,
      });

      // Get recent transactions (last 5)
      const sorted = [...transactions].sort(
        (a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime()
      );
      setRecentTransactions(sorted.slice(0, 5));
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="text-nu-text-secondary">Loading dashboard...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-nu-error/20 rounded-xl p-6">
        <h3 className="text-lg font-bold text-nu-error mb-2">Error</h3>
        <p className="text-sm text-nu-error/80">{error}</p>
        <button
          onClick={fetchDashboardData}
          className="mt-4 px-4 py-2 rounded-full bg-nu-error text-white text-sm hover:brightness-110 transition-all"
        >
          Retry
        </button>
      </div>
    );
  }

  if (!metrics) {
    return null;
  }

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min ago`;
    if (diffMins < 1440) return `${Math.floor(diffMins / 60)} hours ago`;
    return date.toLocaleDateString();
  };

  const getStatusBadge = (status: string) => {
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
  };

  return (
    <div className="max-w-6xl mx-auto">
      <h2 className="text-2xl font-bold text-nu-text-primary mb-6">Dashboard</h2>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <KPICard
          title="Today's Revenue"
          value={formatCurrency(metrics.todayRevenue, 'BRL')}
          change={metrics.revenueTrend.value}
          trend={metrics.revenueTrend.trend}
          icon="💰"
        />
        <KPICard
          title="Transactions"
          value={metrics.todayCount.toString()}
          change={metrics.countTrend.value}
          trend={metrics.countTrend.trend}
          icon="📊"
        />
        <KPICard
          title="Success Rate"
          value={`${metrics.successRate.toFixed(1)}%`}
          change={metrics.successRateTrend.value}
          trend={metrics.successRateTrend.trend}
          icon="✅"
        />
        <KPICard
          title="Avg. Transaction"
          value={formatCurrency(metrics.avgTransaction, 'BRL')}
          change={metrics.avgTrend.value}
          trend={metrics.avgTrend.trend}
          icon="💳"
        />
      </div>

      {/* Recent Transactions */}
      <div className="nu-card mb-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-bold text-nu-text-primary">Recent Transactions</h3>
          <button
            onClick={onNavigateToTransactions}
            className="text-sm text-nu-purple hover:text-nu-purple-dark font-medium transition-colors"
          >
            View All →
          </button>
        </div>

        {recentTransactions.length === 0 ? (
          <div className="py-8 text-center">
            <p className="text-nu-text-muted">No transactions yet</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead>
                <tr className="border-b border-nu-border">
                  <th className="px-4 py-2 text-left text-xs font-semibold text-nu-text-muted uppercase">
                    Transaction ID
                  </th>
                  <th className="px-4 py-2 text-left text-xs font-semibold text-nu-text-muted uppercase">
                    Amount
                  </th>
                  <th className="px-4 py-2 text-left text-xs font-semibold text-nu-text-muted uppercase">
                    Status
                  </th>
                  <th className="px-4 py-2 text-left text-xs font-semibold text-nu-text-muted uppercase">
                    Time
                  </th>
                </tr>
              </thead>
              <tbody>
                {recentTransactions.map((tx) => (
                  <tr
                    key={tx.transaction_id}
                    onClick={() => onSelectTransaction(tx.transaction_id)}
                    className="border-b border-nu-border hover:bg-nu-purple-light/30 cursor-pointer transition-colors"
                  >
                    <td className="px-4 py-3 text-sm font-mono text-nu-text-primary">
                      {tx.transaction_id.substring(0, 8)}...
                    </td>
                    <td className="px-4 py-3 text-sm font-medium text-nu-text-primary">
                      {formatCurrency(tx.amount, tx.currency)}
                    </td>
                    <td className="px-4 py-3">
                      <span className={getStatusBadge(tx.status)}>{tx.status}</span>
                    </td>
                    <td className="px-4 py-3 text-sm text-nu-text-secondary">
                      {formatDate(tx.created_at)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <button
          onClick={onNavigateToTransactions}
          className="nu-card hover:shadow-nu-md transition-all cursor-pointer text-left"
        >
          <div className="flex items-center gap-3">
            <span className="text-3xl">💳</span>
            <div>
              <h4 className="font-semibold text-nu-text-primary">View All Transactions</h4>
              <p className="text-sm text-nu-text-muted">Browse and filter all payments</p>
            </div>
          </div>
        </button>

        <button
          onClick={onNavigateToDevTools}
          className="nu-card hover:shadow-nu-md transition-all cursor-pointer text-left"
        >
          <div className="flex items-center gap-3">
            <span className="text-3xl">🧪</span>
            <div>
              <h4 className="font-semibold text-nu-text-primary">Developer Tools</h4>
              <p className="text-sm text-nu-text-muted">Test payments and idempotency</p>
            </div>
          </div>
        </button>
      </div>
    </div>
  );
}
```

---

### 2. `frontend/src/components/KPICard.tsx`

Reusable KPI card component.

```typescript
interface KPICardProps {
  title: string;
  value: string;
  change: string;
  trend: 'up' | 'down' | 'neutral';
  icon: string;
}

export function KPICard({ title, value, change, trend, icon }: KPICardProps) {
  const trendColor = {
    up: 'text-nu-success',
    down: 'text-nu-error',
    neutral: 'text-nu-text-muted',
  }[trend];

  const trendIcon = {
    up: '↑',
    down: '↓',
    neutral: '→',
  }[trend];

  return (
    <div className="nu-card hover:shadow-nu-md transition-all">
      <div className="flex items-start justify-between mb-3">
        <span className="text-3xl">{icon}</span>
      </div>
      <h3 className="text-sm font-medium text-nu-text-secondary mb-1">{title}</h3>
      <p className="text-2xl font-bold text-nu-text-primary mb-2">{value}</p>
      <div className="flex items-center gap-1">
        <span className={`text-sm font-medium ${trendColor}`}>
          {change} {trendIcon}
        </span>
        <span className="text-xs text-nu-text-muted">vs yesterday</span>
      </div>
    </div>
  );
}
```

---

## Acceptance Criteria

- [ ] Dashboard component renders without errors
- [ ] Shows 4 KPI cards with correct data
- [ ] KPIs calculate from real transaction data via API
- [ ] Trend indicators show correct direction (up/down/neutral)
- [ ] Trend percentages calculate correctly (today vs yesterday)
- [ ] Recent transactions table shows last 5 transactions
- [ ] Clicking transaction row navigates to details
- [ ] "View All" button navigates to Transactions page
- [ ] Quick action cards navigate correctly
- [ ] Loading state displays while fetching data
- [ ] Error state displays if API fails
- [ ] Empty state displays if no transactions exist
- [ ] All amounts format correctly (cents to currency)
- [ ] Relative time displays correctly ("2 min ago", "5 hours ago")

---

## Testing Checklist

### Manual Testing

1. **With No Transactions**
   - [ ] All KPIs show 0 or 0%
   - [ ] Recent transactions shows "No transactions yet"
   - [ ] No errors in console

2. **With Mock Data**
   - [ ] KPIs calculate correctly
   - [ ] Trends show correct direction
   - [ ] Recent transactions display correctly
   - [ ] Click transaction navigates to details

3. **API Failure**
   - [ ] Error message displays
   - [ ] Retry button works
   - [ ] No console errors

4. **Loading State**
   - [ ] Loading message displays
   - [ ] Transitions to content when loaded

---

## Validation Commands

```bash
# Type checking
npm run type-check

# Linting
npm run lint

# Development server
npm run dev
```

---

## Notes

- This component depends on `metrics.ts` utility functions (Task 5)
- You may want to implement Task 5 first, or create placeholder functions
- The Dashboard uses the existing `apiClient.getTransactions()` method
- All styling uses existing Nubank-inspired design system from `index.css`

---

**Next Task**: `02-navigation-structure.md`
