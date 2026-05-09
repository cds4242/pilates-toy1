"use client";

import { Clock, User, MapPin } from "lucide-react";

interface ClassCardProps {
  time: string;
  endTime?: string;
  name: string;
  instructor: string;
  currentCount: number;
  maxCapacity: number;
  actionLabel?: string;
  onAction?: () => void;
  disabled?: boolean;
  loading?: boolean;
  accentColor?: "primary" | "instructor";
}

export function ClassCard({
  time,
  endTime,
  name,
  instructor,
  currentCount,
  maxCapacity,
  actionLabel = "예약",
  onAction,
  disabled,
  loading,
  accentColor = "primary",
}: ClassCardProps) {
  const isFull = currentCount >= maxCapacity;
  const btnBg =
    accentColor === "instructor"
      ? "bg-[var(--color-instructor)] hover:bg-[#6A7DC2]"
      : "bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)]";

  return (
    <div className="rounded-[18px] border border-[var(--color-border)] bg-white p-5 flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <span className="text-[16px] font-bold text-[var(--color-text-title)]">
          {time}{endTime ? `~${endTime}` : ""}
        </span>
        <span className="rounded-[20px] bg-[var(--color-pilates-light)] px-2.5 py-1 text-[13px] font-semibold text-[var(--color-pilates-dark)]">
          {name}
        </span>
      </div>
      <p className="text-[15px] font-semibold text-[var(--color-text-title)]">{instructor}</p>
      <div className="flex gap-4 text-[13px] text-[var(--color-text-body)]">
        <span className="flex items-center gap-1">
          <User className="h-3.5 w-3.5" />
          {currentCount}/{maxCapacity}명
        </span>
        <span className="flex items-center gap-1">
          <Clock className="h-3.5 w-3.5" />
          50분
        </span>
      </div>
      {onAction && (
        <button
          onClick={onAction}
          disabled={disabled || isFull}
          className={`w-full rounded-[8px] py-3 text-[15px] font-semibold text-white transition-all
            ${disabled || isFull ? "bg-[var(--color-text-sub)] cursor-not-allowed" : btnBg}`}
        >
          {loading ? "처리 중..." : isFull ? "마감" : actionLabel}
        </button>
      )}
    </div>
  );
}
