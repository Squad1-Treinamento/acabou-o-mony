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

      // Calculate today's metrics
      const todayRevenue = calculateRevenue(today);
      const todayCount = today.length;
      const successRate = calculateSuccessRate(today);
      const avgTransaction = calculateAverage(today);

      // Calculate yesterday's metrics for trends
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
