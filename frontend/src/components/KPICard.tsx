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
