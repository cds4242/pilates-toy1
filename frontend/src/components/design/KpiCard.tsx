interface KpiCardProps {
  label: string;
  value: string | number;
  change?: string;
  trend?: "up" | "down" | "neutral";
}

export function KpiCard({ label, value, change, trend }: KpiCardProps) {
  const trendColor =
    trend === "up"
      ? "text-[#4CAF50]"
      : trend === "down"
        ? "text-[#E76F51]"
        : "text-[var(--color-text-sub)]";

  return (
    <div className="rounded-[18px] border border-[var(--color-border)] bg-white p-5">
      <p className="text-[13px] font-semibold text-[var(--color-text-body)] mb-2">
        {label}
      </p>
      <p className="text-[28px] font-bold text-[var(--color-text-title)]">
        {value}
      </p>
      {change && (
        <p className={`text-[13px] mt-1 ${trendColor}`}>{change}</p>
      )}
    </div>
  );
}
