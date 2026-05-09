"use client";

import { useEffect, useState } from "react";
import { format, addDays, startOfWeek } from "date-fns";
import { api } from "@/lib/api/client";
import { adminApi } from "@/lib/api/admin";
import type { ClassSchedule } from "@/lib/types/domain";
import { formatTime } from "@/lib/utils/format";
import { toast } from "sonner";

const HOURS = [9, 10, 11, 14, 15, 16, 17, 18, 19];
const DAYS = ["월", "화", "수", "목", "금", "토", "일"];

export default function AdminClassesPage() {
  const [schedules, setSchedules] = useState<ClassSchedule[]>([]);
  const [loading, setLoading] = useState(true);

  // 수업 추가 폼 state
  const [formInstructorId, setFormInstructorId] = useState("");
  const [formLessonTypeId, setFormLessonTypeId] = useState("");
  const [formDate, setFormDate] = useState(format(new Date(), "yyyy-MM-dd"));
  const [formTime, setFormTime] = useState("10:00");
  const [formCapacity, setFormCapacity] = useState("8");
  const [adding, setAdding] = useState(false);

  // 강사/수업유형 목록
  const [instructors, setInstructors] = useState<{ id: number; name: string }[]>([]);
  const [lessonTypes, setLessonTypes] = useState<{ id: number; name: string; maxCapacity: number }[]>([]);

  const weekStart = startOfWeek(new Date(), { weekStartsOn: 1 });
  const weekDates = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i));

  const loadSchedules = async () => {
    try {
      const from = format(weekDates[0], "yyyy-MM-dd");
      const to = format(weekDates[6], "yyyy-MM-dd");
      const data = await api<ClassSchedule[]>("get", "/api/admin/class-schedules", { from, to });
      setSchedules(data);
    } catch { /* empty */ }
    finally { setLoading(false); }
  };

  useEffect(() => {
    loadSchedules();
    // 강사/수업유형 로드
    api<{ id: number; name: string }[]>("get", "/api/admin/instructors").then(setInstructors).catch(() => {});
    api<{ id: number; name: string; maxCapacity: number }[]>("get", "/api/lesson-types").then(setLessonTypes).catch(() => {});
  }, []);

  const getClass = (date: string, hour: number) =>
    schedules.find((s) => s.classDate === date && parseInt(s.startTime) === hour);

  const handleAddClass = async () => {
    if (!formInstructorId || !formLessonTypeId) {
      toast.error("강사와 수업 유형을 선택해주세요");
      return;
    }
    setAdding(true);
    try {
      await adminApi.createClassSchedule({
        instructorId: Number(formInstructorId),
        lessonTypeId: Number(formLessonTypeId),
        classDate: formDate,
        startTime: formTime,
        endTime: `${String(Number(formTime.split(":")[0])).padStart(2, "0")}:50`,
        maxCapacity: Number(formCapacity),
      });
      toast.success("수업이 추가되었습니다");
      loadSchedules();
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "수업 추가 실패");
    } finally {
      setAdding(false);
    }
  };

  const handleAutoGenerate = async () => {
    try {
      const res = await api<{ createdCount: number }>("post", "/api/admin/class-schedules/generate", { weeks: 4 });
      toast.success(`${res.createdCount}건 수업이 생성되었습니다`);
      loadSchedules();
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "자동 생성 실패");
    }
  };

  const inputCls = "border border-[#DDDDDD] rounded-[8px] px-3 py-2.5 text-[15px] outline-none focus:border-pilates bg-white w-full";

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
        <h1 className="text-[26px] font-bold text-text-title">시간표 관리</h1>
        <button onClick={handleAutoGenerate} className="rounded-[8px] bg-pilates hover:bg-pilates-dark px-4 py-2.5 text-[13px] font-semibold text-text-title transition-colors">
          다음 4주치 자동 생성
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[260px_1fr] gap-6">
        {/* 수업 추가 폼 */}
        <div className="rounded-[18px] border border-border bg-white p-5">
          <h2 className="text-[16px] font-bold text-text-title mb-4">수업 추가</h2>
          <div className="flex flex-col gap-3">
            <div className="flex flex-col gap-1">
              <label className="text-[13px] font-semibold text-text-title">강사</label>
              <select value={formInstructorId} onChange={(e) => setFormInstructorId(e.target.value)} className={inputCls}>
                <option value="">강사 선택</option>
                {instructors.map(i => <option key={i.id} value={i.id}>{i.name}</option>)}
              </select>
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-[13px] font-semibold text-text-title">수업 유형</label>
              <select value={formLessonTypeId} onChange={(e) => setFormLessonTypeId(e.target.value)} className={inputCls}>
                <option value="">유형 선택</option>
                {lessonTypes.map(l => <option key={l.id} value={l.id}>{l.name} (정원 {l.maxCapacity})</option>)}
              </select>
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-[13px] font-semibold text-text-title">날짜</label>
              <input type="date" value={formDate} onChange={(e) => setFormDate(e.target.value)} className={inputCls} />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-[13px] font-semibold text-text-title">시작시간</label>
              <select value={formTime} onChange={(e) => setFormTime(e.target.value)} className={inputCls}>
                {HOURS.map(h => <option key={h} value={`${String(h).padStart(2,"0")}:00`}>{String(h).padStart(2,"0")}:00</option>)}
              </select>
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-[13px] font-semibold text-text-title">정원</label>
              <input type="number" value={formCapacity} onChange={(e) => setFormCapacity(e.target.value)} min="1" className={inputCls} />
            </div>
            <button onClick={handleAddClass} disabled={adding} className="w-full bg-pilates hover:bg-pilates-dark text-text-title rounded-[8px] py-3 text-[15px] font-semibold mt-2 transition-colors disabled:opacity-60">
              {adding ? "추가 중..." : "수업 추가"}
            </button>
          </div>
        </div>

        {/* 주간 캘린더 (PC) */}
        <div className="hidden lg:block rounded-[18px] border border-border bg-white overflow-hidden">
          <div className="grid grid-cols-[60px_repeat(7,1fr)]">
            <div className="bg-bg-section border-b border-r border-border p-2" />
            {weekDates.map((d, i) => (
              <div key={i} className="bg-bg-section border-b border-r border-border p-2 text-center">
                <p className="text-[13px] font-semibold text-text-title">{DAYS[i]}</p>
                <p className="text-[11px] text-text-sub">{format(d, "M/d")}</p>
              </div>
            ))}
            {HOURS.map((hour) => (
              <>
                <div key={`h-${hour}`} className="border-b border-r border-border bg-bg-section p-2 text-[13px] text-text-sub text-center">
                  {String(hour).padStart(2, "0")}:00
                </div>
                {weekDates.map((d, i) => {
                  const dateStr = format(d, "yyyy-MM-dd");
                  const cls = getClass(dateStr, hour);
                  return (
                    <div key={`${hour}-${i}`} className="border-b border-r border-border min-h-[44px] p-0.5">
                      {cls && (
                        <div className="bg-pilates-light text-pilates-dark rounded-[4px] p-1 text-[11px] leading-tight cursor-pointer hover:bg-pilates hover:text-white transition-colors">
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

        {/* 모바일 리스트 */}
        <div className="lg:hidden flex flex-col gap-4">
          {weekDates.map((d, i) => {
            const dateStr = format(d, "yyyy-MM-dd");
            const dayClasses = schedules.filter((s) => s.classDate === dateStr);
            if (dayClasses.length === 0) return null;
            return (
              <div key={i}>
                <h3 className="text-[15px] font-bold text-text-title mb-2">{format(d, "M/d")} ({DAYS[i]})</h3>
                {dayClasses.map((c) => (
                  <div key={c.id} className="flex justify-between items-center py-2 border-b border-border text-[13px]">
                    <span className="text-text-title">{formatTime(c.startTime)} {c.lessonTypeName} · {c.instructorName}</span>
                    <span className="text-text-sub">{c.currentCount}/{c.maxCapacity}명</span>
                  </div>
                ))}
              </div>
            );
          })}
          {schedules.length === 0 && !loading && (
            <p className="text-center py-8 text-text-sub">이번 주 수업이 없습니다</p>
          )}
        </div>
      </div>
    </div>
  );
}
