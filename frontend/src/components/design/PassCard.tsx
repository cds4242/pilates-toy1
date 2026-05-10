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
  const daysLeft = Math.ceil((new Date(endDate).getTime() - Date.now()) / 86400000);

  // 색상 코드: 초록(>30일), 노랑(7~30일), 빨강(<7일)
  const urgency = daysLeft <= 0 ? "expired" : daysLeft <= 7 ? "danger" : daysLeft <= 30 ? "warning" : "safe";
  const bgColor = {
    expired: "bg-gray-200",
    danger: "bg-red-50",
    warning: "bg-amber-50",
    safe: "bg-[var(--color-pilates-light)]",
  }[urgency];
  const barColor = {
    expired: "bg-gray-400",
    danger: "bg-red-400",
    warning: "bg-amber-400",
    safe: "bg-[var(--color-pilates-dark)]",
  }[urgency];
  const badgeColor = {
    expired: "text-gray-600",
    danger: "text-red-600 font-bold",
    warning: "text-amber-600 font-semibold",
    safe: "text-[var(--color-text-sub)]",
  }[urgency];

  return (
    <div className={`rounded-[18px] ${bgColor} p-5`}>
      <div className="flex items-center justify-between mb-3">
        <span className="text-[15px] font-semibold text-[var(--color-text-title)]">
          {name || "수강권"}
        </span>
        <span className={`text-[13px] ${badgeColor}`}>
          {daysLeft <= 0 ? "만료됨" : `D-${daysLeft}`}
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
          className={`h-2 rounded-full ${barColor} transition-all`}
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  );
}
