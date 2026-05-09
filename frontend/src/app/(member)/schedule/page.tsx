"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { format, addDays } from "date-fns";
import { ko } from "date-fns/locale";
import { ChevronLeft, Clock, User } from "lucide-react";
import { classroomApi } from "@/lib/api/classroom";
import { reservationApi } from "@/lib/api/reservation";
import type { ClassSchedule } from "@/lib/types/domain";
import { MobileTabBar } from "@/components/design/MobileTabBar";
import { formatTime } from "@/lib/utils/format";

export default function SchedulePage() {
  const router = useRouter();
  const [schedules, setSchedules] = useState<ClassSchedule[]>([]);
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [loading, setLoading] = useState(true);
  const [reserving, setReserving] = useState<number | null>(null);

  const dates = Array.from({ length: 7 }, (_, i) => addDays(new Date(), i));
  const from = format(dates[0], "yyyy-MM-dd");
  const to = format(dates[6], "yyyy-MM-dd");

  useEffect(() => {
    async function load() {
      try {
        const data = await classroomApi.getSchedules(from, to);
        setSchedules(data);
      } catch {
        // empty
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [from, to]);

  const selectedDateStr = format(selectedDate, "yyyy-MM-dd");
  const daySchedules = schedules
    .filter((s) => s.classDate === selectedDateStr)
    .sort((a, b) => a.startTime.localeCompare(b.startTime));

  const handleReserve = async (classScheduleId: number) => {
    setReserving(classScheduleId);
    try {
      await reservationApi.create(classScheduleId);
      toast.success("예약이 완료되었습니다!");
      const data = await classroomApi.getSchedules(from, to);
      setSchedules(data);
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "예약 실패");
    } finally {
      setReserving(null);
    }
  };

  return (
    <div className="max-w-[480px] mx-auto min-h-screen bg-white pb-20">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white px-6 py-4 flex items-center gap-4 border-b border-[var(--color-border)]">
        <button onClick={() => router.back()} className="text-[var(--color-text-title)]">
          <ChevronLeft className="h-6 w-6" />
        </button>
        <h1 className="text-[20px] font-bold text-[var(--color-text-title)]">수업 예약</h1>
      </header>

      {/* 날짜 선택 */}
      <div className="flex gap-2 overflow-x-auto px-6 py-4 scrollbar-hide">
        {dates.map((d) => {
          const dateStr = format(d, "yyyy-MM-dd");
          const isSelected = dateStr === selectedDateStr;
          return (
            <button
              key={dateStr}
              onClick={() => setSelectedDate(d)}
              className={`flex flex-col items-center min-w-[56px] rounded-[18px] py-2.5 px-3 text-center border transition-all ${
                isSelected
                  ? "bg-[var(--color-pilates)] border-[var(--color-pilates)] text-[var(--color-text-title)]"
                  : "bg-white border-[var(--color-border)] text-[var(--color-text-body)]"
              }`}
            >
              <span className="text-[13px]">{format(d, "M/d")}</span>
              <span className="text-[15px] font-semibold">{format(d, "EEE", { locale: ko })}</span>
            </button>
          );
        })}
      </div>

      {/* 수업 목록 */}
      <div className="px-6 py-2 flex flex-col gap-3">
        {loading ? (
          <div className="text-center py-8 text-[var(--color-text-sub)]">로딩 중...</div>
        ) : daySchedules.length === 0 ? (
          <div className="text-center py-16 text-[var(--color-text-sub)]">
            이 날에는 수업이 없습니다
          </div>
        ) : (
          daySchedules.map((cs) => {
            const isFull = cs.currentCount >= cs.maxCapacity;
            return (
              <div key={cs.id} className="rounded-[18px] border border-[var(--color-border)] p-4 flex items-center justify-between">
                <div className="flex-1">
                  <p className="text-[15px] font-semibold text-[var(--color-text-title)]">
                    {formatTime(cs.startTime)}~{formatTime(cs.endTime)} {cs.lessonTypeName}
                  </p>
                  <div className="flex gap-3 mt-1.5 text-[13px] text-[var(--color-text-body)]">
                    <span className="flex items-center gap-1">
                      <User className="h-3.5 w-3.5" />{cs.instructorName}
                    </span>
                    <span className="flex items-center gap-1">
                      <Clock className="h-3.5 w-3.5" />{cs.currentCount}/{cs.maxCapacity}명
                    </span>
                  </div>
                </div>
                <button
                  onClick={() => handleReserve(cs.id)}
                  disabled={isFull || reserving === cs.id}
                  className={`rounded-[8px] px-4 py-2.5 text-[13px] font-semibold transition-all ${
                    isFull
                      ? "bg-[var(--color-bg-section)] text-[var(--color-text-sub)] cursor-not-allowed"
                      : "bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] text-[var(--color-text-title)]"
                  }`}
                >
                  {reserving === cs.id ? "..." : isFull ? "마감" : "예약"}
                </button>
              </div>
            );
          })
        )}
      </div>

      <MobileTabBar />
    </div>
  );
}
