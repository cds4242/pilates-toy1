"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { api } from "@/lib/api/client";

interface AttendanceItem {
  id: number;
  reservationId: number;
  memberName: string;
  status: string;
}

type MarkStatus = "ATTENDED" | "LATE" | "ABSENT";

const TONE = {
  primary: "#7C8FD4",
  primaryDark: "#5F75C4",
  primaryDeep: "#4A5DA8",
  primaryLight: "#C7D0F2",
  primarySoft: "#EEF0FF",
  primaryFaint: "#F6F7FF",
  border: "#E4E7F2",
};

export default function InstructorAttendancePage() {
  return (
    <Suspense
      fallback={
        <div
          className="flex min-h-screen items-center justify-center text-[13px] text-[#A0A0A0]"
          style={{ background: "#F6F7FF" }}
        >
          로딩...
        </div>
      }
    >
      <AttendanceContent />
    </Suspense>
  );
}

function AttendanceContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const classId = searchParams.get("classId");

  const [attendances, setAttendances] = useState<AttendanceItem[]>([]);
  const [localStatus, setLocalStatus] = useState<Record<number, MarkStatus>>(
    {},
  );
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!classId) {
      setLoading(false);
      return;
    }
    async function load() {
      try {
        const data = await api<AttendanceItem[]>(
          "get",
          `/api/instructor/class-schedules/${classId}/attendances`,
        );
        setAttendances(data);
        const initial: Record<number, MarkStatus> = {};
        data.forEach((a) => {
          if (a.status !== "PENDING")
            initial[a.reservationId] = a.status as MarkStatus;
        });
        setLocalStatus(initial);
      } catch {
        toast.error("출석 데이터 로드 실패");
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [classId]);

  const handleMarkAll = (status: MarkStatus) => {
    const next: Record<number, MarkStatus> = {};
    attendances.forEach((a) => {
      next[a.reservationId] = status;
    });
    setLocalStatus(next);
  };

  const handleComplete = async () => {
    const pending = attendances.filter((a) => !localStatus[a.reservationId]);
    if (pending.length > 0) {
      toast.error(`${pending.length}명의 출석 상태를 선택해주세요.`);
      return;
    }
    setSubmitting(true);
    try {
      for (const a of attendances) {
        const st = localStatus[a.reservationId];
        if (st)
          await api("post", `/api/instructor/attendances/${a.reservationId}`, {
            status: st,
          });
      }
      toast.success("출석 체크 완료");
      router.back();
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : "출석 체크 실패");
    } finally {
      setSubmitting(false);
    }
  };

  const completed = attendances.filter(
    (a) => localStatus[a.reservationId],
  ).length;
  const totalCount = attendances.length;

  if (!classId) {
    return (
      <div
        className="mx-auto flex min-h-screen max-w-[480px] items-center justify-center text-[13px] text-[#A0A0A0]"
        style={{ background: "#F6F7FF" }}
      >
        수업을 선택해주세요
      </div>
    );
  }

  const statusOptions: {
    value: MarkStatus;
    label: string;
    accent: string;
    soft: string;
  }[] = [
    { value: "ATTENDED", label: "출석", accent: "#22A259", soft: "#E6F4EA" },
    { value: "LATE", label: "지각", accent: "#F4A261", soft: "#FFF3E0" },
    { value: "ABSENT", label: "결석", accent: "#E76F51", soft: "#FDECEA" },
  ];

  return (
    <div
      className="relative mx-auto min-h-screen max-w-[480px] overflow-hidden bg-white pb-32"
      style={{ background: "#F6F7FF" }}
    >
      {/* Hero header */}
      <section
        className="relative overflow-hidden px-6 pb-6 pt-[18px]"
        style={{
          background:
            "radial-gradient(120% 80% at 90% -10%, #C7D0F2 0%, rgba(199,208,242,0) 55%), radial-gradient(80% 60% at -10% 30%, #E1E6FA 0%, rgba(225,230,250,0) 60%), linear-gradient(180deg, #F6F7FF 0%, #FFFFFF 100%)",
        }}
      >
        <span
          className="pointer-events-none absolute -right-10 -top-10 h-[180px] w-[180px] rounded-full"
          style={{
            background:
              "radial-gradient(circle, rgba(124,143,212,0.18) 0%, rgba(124,143,212,0) 70%)",
          }}
        />
        <div className="relative z-[2] flex items-center gap-2">
          <button
            onClick={() => router.back()}
            aria-label="뒤로가기"
            className="flex h-10 w-10 items-center justify-center rounded-full border bg-white/70 text-[#2A2A2C] backdrop-blur-md transition-colors hover:bg-white"
            style={{ borderColor: "rgba(228,231,242,0.8)" }}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={1.8}
              stroke="currentColor"
              className="h-5 w-5"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M15.75 19.5 8.25 12l7.5-7.5"
              />
            </svg>
          </button>
          <span className="text-[16px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
            출석 체크
          </span>
        </div>

        <div className="relative z-[2] mt-5">
          <span
            className="mb-1 block text-[11px] font-semibold uppercase tracking-[0.06em]"
            style={{ color: TONE.primaryDark }}
          >
            CLASS #{classId}
          </span>
          <h1 className="text-[24px] font-bold leading-[1.3] tracking-[-0.03em] text-[#2A2A2C]">
            출석을 기록해주세요
          </h1>
          <p className="mt-1.5 text-[14px] text-[#6B6B6B]">
            {loading
              ? "출석 데이터를 불러오는 중이에요."
              : totalCount === 0
                ? "예약자가 없어요."
                : `${completed} / ${totalCount}명 체크됨`}
          </p>
        </div>
      </section>

      <main className="relative z-[3] -mt-3 flex flex-col gap-5 px-5">
        {/* Legend */}
        <div
          className="rounded-[14px] border bg-white p-3.5"
          style={{ borderColor: TONE.border }}
        >
          <div className="mb-2 text-[11px] font-semibold uppercase tracking-[0.06em] text-[#A0A0A0]">
            상태 안내
          </div>
          <div className="flex flex-col gap-1.5 text-[12px] text-[#6B6B6B]">
            <div className="flex items-center gap-2">
              <span
                className="block h-2 w-2 rounded-full"
                style={{ background: "#22A259" }}
              />
              출석 = 정상 참석
            </div>
            <div className="flex items-center gap-2">
              <span
                className="block h-2 w-2 rounded-full"
                style={{ background: "#F4A261" }}
              />
              지각 = 출석 인정 (지각 기록)
            </div>
            <div className="flex items-center gap-2">
              <span
                className="block h-2 w-2 rounded-full"
                style={{ background: "#E76F51" }}
              />
              결석 = 노쇼 처리
            </div>
          </div>
        </div>

        {/* Quick actions */}
        {!loading && attendances.length > 0 && (
          <div className="flex gap-2">
            <button
              onClick={() => handleMarkAll("ATTENDED")}
              className="flex-1 cursor-pointer rounded-full border bg-white px-3 py-2 text-[12px] font-medium text-[#6B6B6B] transition-colors hover:border-[#7C8FD4] hover:text-[#5F75C4]"
              style={{ borderColor: TONE.border }}
            >
              모두 출석으로
            </button>
            <button
              onClick={() => setLocalStatus({})}
              className="cursor-pointer rounded-full border bg-white px-3 py-2 text-[12px] font-medium text-[#A0A0A0] transition-colors hover:text-[#6B6B6B]"
              style={{ borderColor: TONE.border }}
            >
              초기화
            </button>
          </div>
        )}

        {/* List */}
        <section>
          <div className="mb-3">
            <span
              className="mb-1 block text-[11px] font-semibold uppercase tracking-[0.06em]"
              style={{ color: TONE.primaryDark }}
            >
              ATTENDEES
            </span>
            <h2 className="text-[18px] font-bold tracking-[-0.02em] text-[#2A2A2C]">
              수강생 명단
            </h2>
          </div>

          {loading ? (
            <div
              className="rounded-[18px] border bg-white py-12 text-center text-[13px] text-[#A0A0A0]"
              style={{ borderColor: TONE.border }}
            >
              불러오는 중...
            </div>
          ) : attendances.length === 0 ? (
            <div
              className="rounded-[18px] border bg-white p-8 text-center"
              style={{ borderColor: TONE.border }}
            >
              <div className="mb-3 text-[40px]">👥</div>
              <p className="mb-1 text-[15px] font-semibold text-[#2A2A2C]">
                예약자가 없어요
              </p>
              <p className="text-[13px] text-[#6B6B6B]">
                이 수업에는 예약된 회원이 없습니다.
              </p>
            </div>
          ) : (
            <div className="flex flex-col gap-2.5">
              {attendances.map((a) => {
                const current = localStatus[a.reservationId];
                return (
                  <div
                    key={a.id}
                    className="rounded-[18px] border bg-white p-3.5"
                    style={{
                      borderColor: current
                        ? TONE.primaryLight
                        : TONE.border,
                    }}
                  >
                    <div className="mb-3 flex items-center gap-3">
                      <div
                        className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full text-[14px] font-bold"
                        style={{
                          background: TONE.primarySoft,
                          color: TONE.primaryDeep,
                        }}
                      >
                        {a.memberName?.charAt(0) || "?"}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="text-[15px] font-semibold tracking-[-0.01em] text-[#2A2A2C]">
                          {a.memberName}
                        </p>
                        <p className="mt-0.5 text-[12px] text-[#A0A0A0]">
                          예약 #{a.reservationId}
                        </p>
                      </div>
                    </div>
                    <div className="grid grid-cols-3 gap-1.5">
                      {statusOptions.map((opt) => {
                        const selected = current === opt.value;
                        return (
                          <button
                            key={opt.value}
                            onClick={() =>
                              setLocalStatus((prev) => ({
                                ...prev,
                                [a.reservationId]: opt.value,
                              }))
                            }
                            className="cursor-pointer rounded-[10px] border px-3 py-2 text-[13px] font-semibold transition-all"
                            style={
                              selected
                                ? {
                                    background: opt.accent,
                                    color: "#fff",
                                    borderColor: opt.accent,
                                  }
                                : {
                                    background: opt.soft,
                                    color: opt.accent,
                                    borderColor: "transparent",
                                  }
                            }
                          >
                            {opt.label}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      </main>

      {/* Sticky submit */}
      {attendances.length > 0 && (
        <div
          className="fixed bottom-0 left-1/2 z-[100] w-full max-w-[480px] -translate-x-1/2 border-t bg-white/92 p-4 backdrop-blur-md"
          style={{
            borderColor: TONE.border,
            background: "rgba(255,255,255,0.92)",
            paddingBottom: "calc(env(safe-area-inset-bottom, 0) + 16px)",
          }}
        >
          <div className="mb-2 flex items-center justify-between text-[12px]">
            <span className="text-[#6B6B6B]">
              체크 완료{" "}
              <span
                className="font-bold"
                style={{ color: TONE.primaryDark }}
              >
                {completed}
              </span>
              <span className="text-[#A0A0A0]"> / {totalCount}명</span>
            </span>
            <span
              className="rounded-full px-2 py-0.5 text-[11px] font-semibold"
              style={{
                background:
                  completed === totalCount
                    ? "rgba(34,162,89,0.12)"
                    : TONE.primaryFaint,
                color:
                  completed === totalCount ? "#22A259" : TONE.primaryDark,
              }}
            >
              {completed === totalCount ? "준비 완료" : "진행 중"}
            </span>
          </div>
          <button
            onClick={handleComplete}
            disabled={submitting || completed !== totalCount}
            className="group/btn relative w-full cursor-pointer overflow-hidden rounded-[14px] border-none px-4 py-4 text-[15px] font-bold text-white transition-[transform,box-shadow] disabled:cursor-not-allowed disabled:opacity-60"
            style={{
              background:
                "linear-gradient(135deg, #8FA1DC 0%, #7C8FD4 60%, #5F75C4 100%)",
              boxShadow:
                "0 8px 18px -6px rgba(95, 117, 196, 0.5), inset 0 0 0 1px rgba(255,255,255,0.2)",
            }}
          >
            {submitting ? (
              "저장 중..."
            ) : (
              <>
                출석 체크 완료{" "}
                <span className="ml-1 inline-block transition-transform group-hover/btn:translate-x-1">
                  →
                </span>
              </>
            )}
          </button>
        </div>
      )}
    </div>
  );
}
