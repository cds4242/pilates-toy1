"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { Calendar } from "lucide-react";
import { memberApi } from "@/lib/api/member";
import { reservationApi } from "@/lib/api/reservation";
import type { Reservation } from "@/lib/types/domain";
import { StatusBadge } from "@/components/design/StatusBadge";
import { MobileTabBar } from "@/components/design/MobileTabBar";
import { formatTime } from "@/lib/utils/format";

export default function ReservationsPage() {
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    try { setReservations(await memberApi.getReservations()); } catch { /* empty */ }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const todayStr = new Date().toISOString().split("T")[0];
  const upcoming = reservations.filter((r) => r.status === "CONFIRMED" && r.classDate >= todayStr);
  const past = reservations.filter((r) => r.status !== "CONFIRMED" || r.classDate < todayStr);

  const [cancelTarget, setCancelTarget] = useState<number | null>(null);
  const [cancelling, setCancelling] = useState(false);

  const handleCancel = async (id: number) => {
    setCancelling(true);
    try {
      await reservationApi.cancel(id, "회원 요청 취소");
      toast.success("예약이 취소되었습니다.");
      setCancelTarget(null);
      load();
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "취소 실패");
    } finally {
      setCancelling(false);
    }
  };

  const statusMap: Record<string, { status: "present" | "absent" | "late" | "noshow" | "active"; label: string }> = {
    CONFIRMED: { status: "active", label: "확정" },
    CANCELLED: { status: "absent", label: "취소" },
    NO_SHOW: { status: "noshow", label: "노쇼" },
    WAITING: { status: "late", label: "대기" },
  };

  return (
    <div className="max-w-[560px] mx-auto min-h-screen bg-white pb-20">
      <header className="sticky top-0 z-50 bg-white px-6 py-4 border-b border-[var(--color-border)]">
        <h1 className="text-[20px] font-bold text-[var(--color-text-title)]">예약 내역</h1>
      </header>
      <main className="p-6 flex flex-col gap-6">
        {/* 다가오는 예약 */}
        <div>
          <h2 className="text-[20px] font-bold text-[var(--color-text-title)] mb-3">다가오는 예약</h2>
          {loading ? (
            <div className="animate-pulse h-20 bg-[var(--color-bg-section)] rounded-[18px]" />
          ) : upcoming.length === 0 ? (
            <>
              <div className="flex flex-col items-center gap-3 py-12">
                <Calendar className="h-10 w-10 text-[var(--color-text-sub)]" />
                <p className="text-[15px] text-[var(--color-text-sub)]">예정된 예약이 없습니다</p>
                <Link href="/schedule"
                  className="mt-1 inline-block rounded-[8px] bg-[var(--color-pilates)] px-5 py-2.5 text-[14px] font-semibold text-[var(--color-text-title)] hover:bg-[var(--color-pilates-dark)] transition-colors">
                  수업 예약하러 가기
                </Link>
              </div>
              <div className="section-card p-4 mt-4 animate-fade-in">
                <p className="text-[13px] text-[var(--color-text-sub)] text-center">예약한 수업 내역이 여기에 표시됩니다</p>
              </div>
            </>
          ) : (
            <div className="flex flex-col gap-2">
              {upcoming.map((r) => (
                <div key={r.id} className="rounded-[18px] border border-[var(--color-border)] p-4 flex items-center gap-4">
                  <div className="w-1 h-12 rounded-full bg-[var(--color-pilates)]" />
                  <div className="flex-1">
                    <p className="text-[15px] font-semibold text-[var(--color-text-title)]">
                      {r.classDate} {formatTime(r.startTime)}~{formatTime(r.endTime)}
                    </p>
                    <p className="text-[13px] text-[var(--color-text-body)]">{r.lessonTypeName} · {r.instructorName}</p>
                  </div>
                  <button onClick={() => setCancelTarget(r.id)} className="rounded-[8px] border border-[var(--color-border)] px-3 py-2 text-[13px] text-[var(--color-text-body)] hover:border-[var(--color-error)] hover:text-[var(--color-error)] transition-colors">
                    취소
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 지난 예약 */}
        {past.length > 0 && (
          <div>
            <h2 className="text-[20px] font-bold text-[var(--color-text-title)] mb-3">지난 예약</h2>
            <div className="flex flex-col gap-2">
              {past.slice(0, 20).map((r) => {
                const isPastConfirmed = r.status === "CONFIRMED" && r.classDate < todayStr;
                const s = isPastConfirmed
                  ? { status: "present" as const, label: "완료" }
                  : statusMap[r.status] || { status: "active" as const, label: r.status };
                return (
                  <div key={r.id} className="flex items-center justify-between rounded-[18px] border border-[var(--color-border)] p-4">
                    <div>
                      <p className="text-[15px] font-semibold text-[var(--color-text-title)]">{r.classDate} {formatTime(r.startTime)}</p>
                      <p className="text-[13px] text-[var(--color-text-sub)]">{r.lessonTypeName} · {r.instructorName}</p>
                    </div>
                    <StatusBadge status={s.status} label={s.label} />
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </main>
      <MobileTabBar />

      {/* 취소 확인 모달 */}
      {cancelTarget !== null && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4" onClick={() => setCancelTarget(null)}>
          <div className="bg-white rounded-[18px] w-full max-w-[360px] p-6" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-[18px] font-bold text-[var(--color-text-title)] mb-2">예약을 취소하시겠습니까?</h3>
            <p className="text-[14px] text-[var(--color-text-sub)] mb-6">취소된 예약은 복구할 수 없으며, 수강권 횟수가 복원됩니다.</p>
            <div className="flex gap-3">
              <button onClick={() => setCancelTarget(null)} className="flex-1 rounded-[8px] border border-[var(--color-border)] py-3 text-[15px] font-semibold text-[var(--color-text-body)] transition-colors hover:bg-[var(--color-bg-section)]">
                돌아가기
              </button>
              <button onClick={() => handleCancel(cancelTarget)} disabled={cancelling} className="flex-1 rounded-[8px] bg-[var(--color-error)] py-3 text-[15px] font-semibold text-white transition-colors hover:opacity-90 disabled:opacity-60">
                {cancelling ? "취소 중..." : "예약 취소"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
