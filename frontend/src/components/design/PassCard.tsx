"use client";

interface PassCardProps {
  name: string;
  remaining: number;
  total: number;
  unlimited?: boolean;
  endDate: string;
  compact?: boolean;
}

export function PassCard({
  name,
  remaining,
  total,
  unlimited,
  endDate,
  compact,
}: PassCardProps) {
  const percent = unlimited ? 100 : total > 0 ? (remaining / total) * 100 : 0;

  return (
    <div className="rounded-[18px] bg-[var(--color-pilates-light)] p-5">
      <div className="flex items-center justify-between mb-3">
        <span className="text-[15px] font-semibold text-[var(--color-text-title)]">
          {name}
        </span>
        <span className="text-[13px] text-[var(--color-text-body)]">
          ~{endDate}
        </span>
      </div>
      <div className="flex items-end gap-1 mb-3">
        <span className={`font-bold text-[var(--color-text-title)] ${compact ? "text-[28px]" : "text-[32px]"}`}>
          {unlimited ? "무제한" : remaining}
        </span>
        {!unlimited && (
          <span className="text-[15px] text-[var(--color-text-body)] mb-1">
            / {total}회 남음
          </span>
        )}
      </div>
      <div className="h-2 rounded-full bg-white/60">
        <div
          className="h-2 rounded-full bg-[var(--color-pilates-dark)] transition-all"
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  );
}
