"use client";

import { useEffect, useState } from "react";
import { format, addDays, startOfWeek } from "date-fns";
import { ko } from "date-fns/locale";
import { api } from "@/lib/api/client";
import type { ClassSchedule } from "@/lib/types/domain";
import { formatTime } from "@/lib/utils/format";
import { toast } from "sonner";

const HOURS = [9, 10, 11, 14, 15, 16, 17, 18, 19];
const DAYS = ["월", "화", "수", "목", "금", "토", "일"];

export default function AdminClassesPage() {
  const [schedules, setSchedules] = useState<ClassSchedule[]>([]);
  const [loading, setLoading] = useState(true);

  const weekStart = startOfWeek(new Date(), { weekStartsOn: 1 });
  const weekDates = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));

  useEffect(() => {
    async function load() {
      try {
        const from = format(weekDates[0], "yyyy-MM-dd");
        const to = format(weekDates[6], "yyyy-MM-dd");
        const data = await api<ClassSchedule[]>("get", "/api/admin/class-schedules", { from, to });
        setSchedules(data);
      } catch { /* empty */ }
      finally { setLoading(false); }
    }
    load();
  }, []);

  const getClass = (date: string, hour: number) =>
    schedules.find((s) => s.classDate === date && parseInt(s.startTime) === hour);

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
        <h1 className="text-[26px] font-bold text-[var(--color-text-title)]">시간표 관리</h1>
        <button onClick={() => toast.info("자동 생성 기능은 준비 중입니다")} className="rounded-[8px] bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] px-4 py-2.5 text-[13px] font-semibold text-[var(--color-text-title)] transition-colors">
          다음 4주치 자동 생성
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[260px_1fr] gap-6">
        {/* 수업 추가 폼 */}
        <div className="rounded-[18px] border border-[var(--color-border)] bg-white p-5">
          <h2 className="text-[16px] font-bold text-[var(--color-text-title)] mb-4">수업 추가</h2>
          <div className="flex flex-col gap-3">
            {[
              { label: "강사", placeholder: "강사 선택" },
              { label: "수업 유형", placeholder: "유형 선택" },
              { label: "요일", placeholder: "요일 선택" },
              { label: "시작시간", placeholder: "시간 선택" },
              { label: "정원", placeholder: "정원 입력" },
            ].map((f) => (
              <div key={f.label} className="flex flex-col gap-1">
                <label className="text-[13px] font-semibold text-[var(--color-text-title)]">{f.label}</label>
                <select className="border border-[#DDDDDD] rounded-[8px] px-3 py-2.5 text-[15px] outline-none focus:border-[var(--color-pilates)] bg-white">
                  <option>{f.placeholder}</option>
                </select>
              </div>
            ))}
            <button onClick={() => toast.info("수업 추가 기능은 준비 중입니다")} className="w-full bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] text-[var(--color-text-title)] rounded-[8px] py-3 text-[15px] font-semibold mt-2 transition-colors">
              수업 추가
            </button>
          </div>
        </div>

        {/* 주간 캘린더 (PC) */}
        <div className="hidden lg:block rounded-[18px] border border-[var(--color-border)] bg-white overflow-hidden">
          <div className="grid grid-cols-[60px_repeat(7,1fr)]">
            {/* 헤더 */}
            <div className="bg-[var(--color-bg-section)] border-b border-r border-[var(--color-border)] p-2" />
            {weekDates.map((d, i) => (
              <div key={i} className="bg-[var(--color-bg-section)] border-b border-r border-[var(--color-border)] p-2 text-center">
                <p className="text-[13px] font-semibold text-[var(--color-text-title)]">{DAYS[i]}</p>
                <p className="text-[11px] text-[var(--color-text-sub)]">{format(d, "M/d")}</p>
              </div>
            ))}
            {/* 시간 행 */}
            {HOURS.map((hour) => (
              <>
                <div key={`h-${hour}`} className="border-b border-r border-[var(--color-border)] bg-[var(--color-bg-section)] p-2 text-[13px] text-[var(--color-text-sub)] text-center">
                  {String(hour).padStart(2, "0")}:00
                </div>
                {weekDates.map((d, i) => {
                  const dateStr = format(d, "yyyy-MM-dd");
                  const cls = getClass(dateStr, hour);
                  return (
                    <div key={`${hour}-${i}`} className="border-b border-r border-[var(--color-border)] min-h-[44px] p-0.5">
                      {cls && (
                        <div className="bg-[var(--color-pilates-light)] text-[var(--color-pilates-dark)] rounded-[4px] p-1 text-[11px] leading-tight cursor-pointer hover:bg-[var(--color-pilates)] hover:text-white transition-colors">
                          {cls.lessonTypeName}<br />{cls.instructorName}
                        </div>
                      )}
                    </div>
                  );
                })}
              </>
            ))}
          </div>
        </div>

        {/* 모바일: 리스트 */}
        <div className="lg:hidden flex flex-col gap-4">
          {weekDates.map((d, i) => {
            const dateStr = format(d, "yyyy-MM-dd");
            const dayClasses = schedules.filter((s) => s.classDate === dateStr);
            if (dayClasses.length === 0) return null;
            return (
              <div key={i}>
                <h3 className="text-[15px] font-bold text-[var(--color-text-title)] mb-2">
                  {format(d, "M/d")} ({DAYS[i]})
                </h3>
                {dayClasses.map((c) => (
                  <div key={c.id} className="flex justify-between items-center py-2 border-b border-[var(--color-border)] text-[13px]">
                    <span className="text-[var(--color-text-title)]">
                      {formatTime(c.startTime)} {c.lessonTypeName} · {c.instructorName}
                    </span>
                    <span className="text-[var(--color-text-sub)]">{c.currentCount}/{c.maxCapacity}명</span>
                  </div>
                ))}
              </div>
            );
          })}
          {schedules.length === 0 && !loading && (
            <p className="text-center py-8 text-[var(--color-text-sub)]">이번 주 수업이 없습니다</p>
          )}
        </div>
      </div>
    </div>
  );
}
