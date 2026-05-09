"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { format, addDays } from "date-fns";
import { ko } from "date-fns/locale";
import { classroomApi } from "@/lib/api/classroom";
import { reservationApi } from "@/lib/api/reservation";
import type { ClassSchedule } from "@/lib/types/domain";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { formatTime } from "@/lib/utils/format";

export default function SchedulePage() {
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
      // 스케줄 갱신
      const data = await classroomApi.getSchedules(from, to);
      setSchedules(data);
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "예약 실패");
    } finally {
      setReserving(null);
    }
  };

  return (
    <div className="max-w-lg mx-auto p-4 space-y-4">
      <h1 className="text-xl font-bold">수업 시간표</h1>

      {/* 날짜 선택 (가로 스크롤) */}
      <div className="flex gap-2 overflow-x-auto pb-2 -mx-4 px-4">
        {dates.map((d) => {
          const dateStr = format(d, "yyyy-MM-dd");
          const isSelected = dateStr === selectedDateStr;
          return (
            <button
              key={dateStr}
              onClick={() => setSelectedDate(d)}
              className={`flex flex-col items-center min-w-[3.5rem] rounded-lg py-2 px-3 text-sm transition-colors ${
                isSelected
                  ? "bg-primary text-primary-foreground"
                  : "bg-muted hover:bg-accent"
              }`}
            >
              <span className="text-xs">
                {format(d, "EEE", { locale: ko })}
              </span>
              <span className="font-semibold">{format(d, "d")}</span>
            </button>
          );
        })}
      </div>

      {/* 수업 목록 */}
      {loading ? (
        <div className="text-center text-muted-foreground py-8">
          로딩 중...
        </div>
      ) : daySchedules.length === 0 ? (
        <div className="text-center text-muted-foreground py-8">
          이 날에는 수업이 없습니다.
        </div>
      ) : (
        <div className="space-y-3">
          {daySchedules.map((cs) => {
            const isFull = cs.currentCount >= cs.maxCapacity;
            return (
              <Card key={cs.id}>
                <CardContent className="flex items-center justify-between py-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-sm">
                        {formatTime(cs.startTime)}~{formatTime(cs.endTime)}
                      </span>
                      <Badge variant="secondary">{cs.lessonTypeName}</Badge>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {cs.instructorName} ·{" "}
                      {cs.currentCount}/{cs.maxCapacity}명
                    </p>
                  </div>
                  <Button
                    size="sm"
                    disabled={isFull || reserving === cs.id}
                    onClick={() => handleReserve(cs.id)}
                  >
                    {reserving === cs.id
                      ? "예약 중..."
                      : isFull
                        ? "마감"
                        : "예약"}
                  </Button>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
