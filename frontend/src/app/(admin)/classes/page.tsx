"use client";

import { useEffect, useState } from "react";
import { format, addDays, addWeeks, startOfWeek } from "date-fns";
import { ChevronLeft, ChevronRight, X } from "lucide-react";
import { api } from "@/lib/api/client";
import { adminApi } from "@/lib/api/admin";
import type { ClassSchedule } from "@/lib/types/domain";
import { formatTime } from "@/lib/utils/format";
import { toast } from "sonner";

const HOURS = [9, 10, 11, 14, 15, 16, 17, 18, 19];
const DAYS = ["월", "화", "수", "목", "금", "토", "일"];

function getLessonTypeColor(lessonTypeName?: string): string {
  if (!lessonTypeName) return "bg-pilates-light text-pilates-dark";
  if (lessonTypeName.includes("개인")) return "bg-blue-100 text-blue-800";
  if (lessonTypeName.includes("듀엣")) return "bg-purple-100 text-purple-800";
  if (lessonTypeName.includes("그룹")) return "bg-green-100 text-green-800";
  if (lessonTypeName.includes("체험")) return "bg-amber-100 text-amber-800";
  return "bg-pilates-light text-pilates-dark";
}

export default function AdminClassesPage() {
  const [schedules, setSchedules] = useState<ClassSchedule[]>([]);
  const [loading, setLoading] = useState(true);
  const [weekOffset, setWeekOffset] = useState(0);
  const [filterInstructor, setFilterInstructor] = useState("");

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

  // 수업 상세 모달
  const [selectedClass, setSelectedClass] = useState<ClassSchedule | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const [reservations, setReservations] = useState<{ id: number; memberName: string; memberPhone: string; status: string }[]>([]);
  const [resLoading, setResLoading] = useState(false);

  useEffect(() => {
    if (!selectedClass) { setReservations([]); return; }
    setResLoading(true);
    api<{ id: number; memberName: string; memberPhone: string; status: string }[]>(
      "get", `/api/admin/class-schedules/${selectedClass.id}/reservations`
    ).then(setReservations).catch(() => setReservations([])).finally(() => setResLoading(false));
  }, [selectedClass]);

  const weekStart = startOfWeek(addWeeks(new Date(), weekOffset), { weekStartsOn: 1 });
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
  }, [weekOffset]);

  useEffect(() => {
    // 강사/수업유형 로드
    api<{ id: number; name: string }[]>("get", "/api/admin/instructors").then(setInstructors).catch(() => {});
    api<{ id: number; name: string; maxCapacity: number }[]>("get", "/api/lesson-types").then(setLessonTypes).catch(() => {});
  }, []);

  const todayStr = format(new Date(), "yyyy-MM-dd");

  const filteredSchedules = filterInstructor
    ? schedules.filter(s => s.instructorName === filterInstructor)
    : schedules;

  const getClass = (date: string, hour: number) =>
    filteredSchedules.find((s) => s.classDate === date && parseInt(s.startTime) === hour);

  const handleCellClick = (date: string, hour: number) => {
    setFormDate(date);
    setFormTime(`${String(hour).padStart(2, "0")}:00`);
  };

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
    if (!confirm("고정 스케줄 기반으로 다음 4주치 수업을 자동 생성합니다. 기존 수업은 유지됩니다. 진행하시겠습니까?")) return;
    try {
      const res = await api<{ createdCount: number }>("post", "/api/admin/class-schedules/generate", { weeks: 4 });
      toast.success(`${res.createdCount}건 수업이 생성되었습니다`);
      loadSchedules();
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "자동 생성 실패");
    }
  };

  const handleClassClick = (cls: ClassSchedule) => {
    setSelectedClass(cls);
  };

  const handleCancelClass = async () => {
    if (!selectedClass) return;
    if (!confirm("이 수업을 휴강 처리하시겠습니까? 예약된 회원에게 알림이 전송됩니다.")) return;
    setCancelling(true);
    try {
      await api("post", `/api/admin/class-schedules/${selectedClass.id}/cancel`);
      toast.success("휴강 처리되었습니다");
      setSelectedClass(null);
      loadSchedules();
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "휴강 처리 실패");
    } finally {
      setCancelling(false);
    }
  };

  const inputCls = "border border-[#DDDDDD] rounded-[8px] px-3 py-2.5 text-[15px] outline-none focus:border-pilates bg-white w-full";

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
        <h1 className="text-[26px] font-bold text-text-title">시간표 관리</h1>
        <button onClick={handleAutoGenerate} title="고정 스케줄 기반으로 다음 4주치 수업을 자동으로 생성합니다" className="rounded-[8px] bg-pilates hover:bg-pilates-dark px-4 py-2.5 text-[13px] font-semibold text-text-title transition-colors">
          다음 4주치 자동 생성
        </button>
      </div>

      <div className="flex items-center justify-center gap-4 mb-4">
        <button onClick={() => setWeekOffset((p) => p - 1)} className="p-1.5 rounded-[8px] hover:bg-bg-section transition-colors">
          <ChevronLeft className="w-5 h-5 text-text-title" />
        </button>
        <span className="text-[15px] font-semibold text-text-title min-w-[120px] text-center">
          {format(weekDates[0], "M/d")} ~ {format(weekDates[6], "M/d")}
        </span>
        <button onClick={() => setWeekOffset((p) => p + 1)} className="p-1.5 rounded-[8px] hover:bg-bg-section transition-colors">
          <ChevronRight className="w-5 h-5 text-text-title" />
        </button>
      </div>

      {/* 강사 필터 칩 */}
      <div className="flex flex-wrap gap-2 mb-4">
        <button
          onClick={() => setFilterInstructor("")}
          className={`px-3 py-1.5 rounded-[8px] text-[13px] transition-colors ${filterInstructor === "" ? "bg-pilates text-text-title font-semibold" : "bg-bg-section text-text-body"}`}
        >
          전체
        </button>
        {instructors.map(inst => (
          <button
            key={inst.id}
            onClick={() => setFilterInstructor(inst.name)}
            className={`px-3 py-1.5 rounded-[8px] text-[13px] transition-colors ${filterInstructor === inst.name ? "bg-pilates text-text-title font-semibold" : "bg-bg-section text-text-body"}`}
          >
            {inst.name}
          </button>
        ))}
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
              <select value={formLessonTypeId} onChange={(e) => {
                  setFormLessonTypeId(e.target.value);
                  const lt = lessonTypes.find(l => String(l.id) === e.target.value);
                  if (lt) setFormCapacity(String(lt.maxCapacity));
                }} className={inputCls}>
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
              <p className="text-[11px] text-text-sub mt-1">종료: {String(Number(formTime.split(":")[0])).padStart(2, "0")}:50</p>
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-[13px] font-semibold text-text-title">정원</label>
              <input type="number" value={formCapacity} onChange={(e) => setFormCapacity(e.target.value)} min="1" max="50" className={inputCls} />
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
            {weekDates.map((d, i) => {
              const isToday = format(d, "yyyy-MM-dd") === todayStr;
              return (
                <div key={i} className={`border-b border-r border-border p-2 text-center ${isToday ? "bg-pilates-light" : "bg-bg-section"}`}>
                  <p className="text-[13px] font-semibold text-text-title">{DAYS[i]}</p>
                  <p className="text-[11px] text-text-sub">{format(d, "M/d")}</p>
                </div>
              );
            })}
            {HOURS.map((hour) => (
              <>
                <div key={`h-${hour}`} className="border-b border-r border-border bg-bg-section p-2 text-[13px] text-text-sub text-center">
                  {String(hour).padStart(2, "0")}:00
                </div>
                {weekDates.map((d, i) => {
                  const dateStr = format(d, "yyyy-MM-dd");
                  const isToday = dateStr === todayStr;
                  const cls = getClass(dateStr, hour);
                  const isFull = cls ? cls.currentCount >= cls.maxCapacity : false;
                  return (
                    <div
                      key={`${hour}-${i}`}
                      className={`border-b border-r border-border min-h-[44px] p-0.5 ${isToday ? "bg-pilates-light/30" : ""} ${!cls ? "cursor-pointer hover:bg-pilates-light/20" : ""}`}
                      onClick={() => !cls && handleCellClick(dateStr, hour)}
                    >
                      {cls && (
                        <div onClick={() => handleClassClick(cls)} className={`${getLessonTypeColor(cls.lessonTypeName)} rounded-[4px] p-1 text-[11px] leading-tight cursor-pointer hover:opacity-80 transition-colors ${isFull ? "ring-2 ring-red-400" : ""}`}>
                          {cls.lessonTypeName}<br />{cls.instructorName}
                          <p className="text-[10px] opacity-70 mt-0.5">{cls.currentCount}/{cls.maxCapacity}</p>
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
            const dayClasses = filteredSchedules.filter((s) => s.classDate === dateStr).sort((a, b) => a.startTime.localeCompare(b.startTime));
            if (dayClasses.length === 0) return null;
            return (
              <div key={i}>
                <h3 className="text-[15px] font-bold text-text-title mb-2">{format(d, "M/d")} ({DAYS[i]})</h3>
                {dayClasses.map((c) => (
                  <div key={c.id} onClick={() => handleClassClick(c)} className="flex justify-between items-center py-2 border-b border-border text-[13px] cursor-pointer hover:bg-bg-section transition-colors">
                    <span className="text-text-title">{formatTime(c.startTime)} {c.lessonTypeName} · {c.instructorName}</span>
                    <span className="text-text-sub">{c.currentCount}/{c.maxCapacity}명</span>
                  </div>
                ))}
              </div>
            );
          })}
          {filteredSchedules.length === 0 && !loading && (
            <p className="text-center py-8 text-text-sub">이번 주 수업이 없습니다</p>
          )}
        </div>
      </div>

      {/* 수업 상세 모달 */}
      {selectedClass && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={() => setSelectedClass(null)}>
          <div className="bg-white rounded-[18px] max-w-[480px] w-[calc(100%-32px)] p-6 relative" onClick={(e) => e.stopPropagation()}>
            <button onClick={() => setSelectedClass(null)} className="absolute top-4 right-4 p-1 rounded-[8px] hover:bg-bg-section transition-colors">
              <X className="w-5 h-5 text-text-sub" />
            </button>

            <h2 className="text-[18px] font-bold text-text-title mb-4">수업 상세</h2>

            {/* 수업 정보 */}
            <div className="space-y-2 mb-5">
              <div className="flex justify-between text-[14px]">
                <span className="text-text-sub">날짜</span>
                <span className="text-text-title font-medium">{selectedClass.classDate}</span>
              </div>
              <div className="flex justify-between text-[14px]">
                <span className="text-text-sub">시간</span>
                <span className="text-text-title font-medium">{formatTime(selectedClass.startTime)} ~ {formatTime(selectedClass.endTime)}</span>
              </div>
              <div className="flex justify-between text-[14px]">
                <span className="text-text-sub">수업 유형</span>
                <span className={`${getLessonTypeColor(selectedClass.lessonTypeName)} px-2 py-0.5 rounded-[4px] text-[13px] font-medium`}>
                  {selectedClass.lessonTypeName}
                </span>
              </div>
              <div className="flex justify-between text-[14px]">
                <span className="text-text-sub">강사</span>
                <span className="text-text-title font-medium">{selectedClass.instructorName}</span>
              </div>
              <div className="flex justify-between text-[14px]">
                <span className="text-text-sub">인원</span>
                <span className="text-text-title font-medium">{selectedClass.currentCount} / {selectedClass.maxCapacity}명</span>
              </div>
              <div className="flex justify-between text-[14px]">
                <span className="text-text-sub">상태</span>
                <span className={`text-[13px] font-medium ${selectedClass.status === "CANCELLED" ? "text-red-600" : selectedClass.status === "COMPLETED" ? "text-text-sub" : "text-green-600"}`}>
                  {selectedClass.status === "SCHEDULED" ? "예정" : selectedClass.status === "CANCELLED" ? "휴강" : selectedClass.status === "COMPLETED" ? "완료" : selectedClass.status}
                </span>
              </div>
            </div>

            {/* 예약자 명단 */}
            <div className="mb-5">
              <h3 className="text-[15px] font-bold text-text-title mb-2">예약자 명단 ({reservations.length}명)</h3>
              {resLoading ? (
                <div className="rounded-[8px] border border-border p-3 text-[13px] text-text-sub text-center">로딩 중...</div>
              ) : reservations.length === 0 ? (
                <div className="rounded-[8px] border border-border p-3 text-[13px] text-text-sub text-center">예약자가 없습니다</div>
              ) : (
                <div className="flex flex-col gap-1 max-h-[160px] overflow-y-auto">
                  {reservations.map((r, i) => (
                    <div key={r.id} className="flex items-center justify-between rounded-[6px] bg-bg-section px-3 py-2 text-[13px]">
                      <span className="text-text-title font-medium">{i + 1}. {r.memberName}</span>
                      <span className="text-text-sub">{r.status === "CONFIRMED" ? "확정" : r.status}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* 액션 버튼 */}
            {selectedClass.status === "SCHEDULED" && (
              <button
                onClick={handleCancelClass}
                disabled={cancelling}
                className="w-full bg-red-500 hover:bg-red-600 text-white rounded-[8px] py-3 text-[15px] font-semibold transition-colors disabled:opacity-60"
              >
                {cancelling ? "처리 중..." : "휴강 처리"}
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
