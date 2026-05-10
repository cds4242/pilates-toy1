"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ChevronLeft } from "lucide-react";
import { toast } from "sonner";
import { api } from "@/lib/api/client";

interface AttendanceItem {
  id: number;
  reservationId: number;
  memberName: string;
  status: string;
}

type MarkStatus = "ATTENDED" | "LATE" | "ABSENT";

export default function InstructorAttendancePage() {
  return <Suspense fallback={<div className="flex items-center justify-center min-h-screen">로딩...</div>}><AttendanceContent /></Suspense>;
}

function AttendanceContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const classId = searchParams.get("classId");

  const [attendances, setAttendances] = useState<AttendanceItem[]>([]);
  const [localStatus, setLocalStatus] = useState<Record<number, MarkStatus>>({});
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!classId) { setLoading(false); return; }
    async function load() {
      try {
        const data = await api<AttendanceItem[]>("get", `/api/instructor/class-schedules/${classId}/attendances`);
        setAttendances(data);
        const initial: Record<number, MarkStatus> = {};
        data.forEach(a => { if (a.status !== "PENDING") initial[a.reservationId] = a.status as MarkStatus; });
        setLocalStatus(initial);
      } catch { toast.error("출석 데이터 로드 실패"); }
      finally { setLoading(false); }
    }
    load();
  }, [classId]);

  const statusButtons: { value: MarkStatus; label: string; colors: string; selected: string }[] = [
    { value: "ATTENDED", label: "출석", colors: "bg-bg-section text-text-sub", selected: "bg-[#E8F5E9] text-[#4CAF50]" },
    { value: "LATE", label: "지각", colors: "bg-bg-section text-text-sub", selected: "bg-[#FFF3E0] text-[#F4A261]" },
    { value: "ABSENT", label: "결석", colors: "bg-bg-section text-text-sub", selected: "bg-[#FDECEA] text-[#E76F51]" },
  ];

  const handleComplete = async () => {
    const pending = attendances.filter(a => !localStatus[a.reservationId]);
    if (pending.length > 0) { toast.error(`${pending.length}명의 출석 상태를 선택해주세요.`); return; }
    setSubmitting(true);
    try {
      for (const a of attendances) {
        const st = localStatus[a.reservationId];
        if (st) await api("post", `/api/instructor/attendances/${a.reservationId}`, { status: st });
      }
      toast.success("출석 체크 완료");
      router.back();
    } catch (err: unknown) { toast.error(err instanceof Error ? err.message : "출석 체크 실패"); }
    finally { setSubmitting(false); }
  };

  if (!classId) {
    return <div className="max-w-[560px] mx-auto min-h-screen bg-white flex items-center justify-center"><p className="text-text-sub">수업을 선택해주세요</p></div>;
  }

  return (
    <div className="max-w-[560px] mx-auto min-h-screen bg-white pb-24">
      <header className="sticky top-0 z-50 bg-white px-6 py-4 flex items-center gap-4 border-b border-border">
        <button onClick={() => router.back()} className="text-text-title"><ChevronLeft className="h-6 w-6" /></button>
        <h1 className="text-[20px] font-bold text-text-title">출석 체크</h1>
      </header>
      <div className="bg-bg-section px-6 py-4 border-b border-border">
        <p className="text-[13px] text-text-body">수업 #{classId}</p>
        <div className="flex gap-4 mt-2 text-[11px]">
          <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-[#4CAF50]" />출석 = 정상 참석</span>
          <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-[#F4A261]" />지각 = 출석 인정 (지각 기록)</span>
          <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-[#E76F51]" />결석 = 노쇼 처리</span>
        </div>
      </div>
      <div className="p-6 flex flex-col gap-3">
        {loading ? <div className="text-center py-16 text-text-sub">로딩 중...</div>
        : attendances.length === 0 ? <div className="text-center py-16 text-text-sub">예약자가 없습니다</div>
        : attendances.map((a) => (
          <div key={a.id} className="rounded-[18px] border border-border p-4 flex items-center gap-3">
            <div className="w-9 h-9 rounded-full bg-pilates-light flex items-center justify-center text-[14px] font-bold text-pilates-dark shrink-0">{a.memberName?.charAt(0)||"?"}</div>
            <div className="flex-1 min-w-0"><span className="text-[15px] font-semibold text-text-title">{a.memberName}</span></div>
            <div className="flex gap-1 shrink-0">
              {statusButtons.map((btn) => (
                <button key={btn.value} onClick={() => setLocalStatus(prev => ({ ...prev, [a.reservationId]: btn.value }))}
                  className={`rounded-[6px] px-3 py-2 text-[13px] font-semibold transition-colors ${localStatus[a.reservationId] === btn.value ? btn.selected : btn.colors}`}>{btn.label}</button>
              ))}
            </div>
          </div>
        ))}
      </div>
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-border p-4">
        <div className="max-w-[560px] mx-auto">
          <button onClick={handleComplete} disabled={submitting}
            className="w-full bg-instructor hover:bg-[#6A7DC2] text-white rounded-[8px] py-4 text-[16px] font-semibold transition-colors disabled:opacity-60">
            {submitting ? "저장 중..." : "출석 체크 완료"}
          </button>
        </div>
      </div>
    </div>
  );
}
