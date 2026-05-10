"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { classroomApi } from "@/lib/api/classroom";
import type { ClassSchedule } from "@/lib/types/domain";

const WEEKDAY_KO = ["일", "월", "화", "수", "목", "금", "토"];

interface SearchSheetProps {
  open: boolean;
  onClose: () => void;
}

export function SearchSheet({ open, onClose }: SearchSheetProps) {
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [lessonFilter, setLessonFilter] = useState<string | null>(null);
  const [schedules, setSchedules] = useState<ClassSchedule[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        const today = new Date();
        const end = new Date();
        end.setDate(today.getDate() + 13);
        const from = today.toISOString().split("T")[0];
        const to = end.toISOString().split("T")[0];
        const data = await classroomApi.getSchedules(from, to);
        if (!cancelled) setSchedules(data);
      } catch {
        // empty
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [open]);

  useEffect(() => {
    if (!open) {
      setQuery("");
      setLessonFilter(null);
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const orig = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = orig;
    };
  }, [open]);

  const lessonTypes = useMemo(() => {
    return Array.from(new Set(schedules.map((s) => s.lessonTypeName))).sort();
  }, [schedules]);

  const todayStr = new Date().toISOString().split("T")[0];
  const nowMs = Date.now();

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    return schedules
      .filter((s) => {
        if (s.classDate < todayStr) return false;
        const startMs = new Date(`${s.classDate}T${s.startTime}`).getTime();
        if (startMs < nowMs) return false;
        if (lessonFilter && s.lessonTypeName !== lessonFilter) return false;
        if (!q) return true;
        return (
          s.instructorName.toLowerCase().includes(q) ||
          s.lessonTypeName.toLowerCase().includes(q) ||
          s.classDate.includes(q)
        );
      })
      .sort(
        (a, b) =>
          a.classDate.localeCompare(b.classDate) ||
          a.startTime.localeCompare(b.startTime),
      )
      .slice(0, 30);
  }, [schedules, query, lessonFilter, todayStr, nowMs]);

  if (!open) return null;

  function go(s: ClassSchedule) {
    onClose();
    router.push(`/schedule?date=${s.classDate}`);
  }

  return (
    <div
      className="fixed inset-0 z-[200] flex flex-col bg-black/40"
      onClick={onClose}
    >
      <div
        className="mx-auto flex h-full w-full max-w-[480px] flex-col bg-white"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center gap-2 border-b border-[#F0EBE8] px-4 py-3">
          <button
            onClick={onClose}
            aria-label="닫기"
            className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full text-[#2A2A2C] hover:bg-[#FAF7F5]"
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
                d="M6 18 18 6M6 6l12 12"
              />
            </svg>
          </button>
          <div className="flex flex-1 items-center gap-2 rounded-full bg-[#FAF7F5] px-3.5 py-2">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={1.8}
              stroke="currentColor"
              className="h-4 w-4 flex-shrink-0 text-[#A0A0A0]"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z"
              />
            </svg>
            <input
              autoFocus
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="강사명 · 수업 · 날짜로 찾기"
              className="flex-1 bg-transparent text-[14px] text-[#2A2A2C] placeholder:text-[#A0A0A0] focus:outline-none"
            />
            {query && (
              <button
                onClick={() => setQuery("")}
                aria-label="입력 지우기"
                className="flex h-5 w-5 flex-shrink-0 items-center justify-center rounded-full bg-[#E0DCD8] text-white"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={2.5}
                  stroke="currentColor"
                  className="h-3 w-3"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M6 18 18 6M6 6l12 12"
                  />
                </svg>
              </button>
            )}
          </div>
        </div>

        {/* Filter chips */}
        {lessonTypes.length > 0 && (
          <div className="flex gap-1.5 overflow-x-auto px-4 py-3">
            <button
              onClick={() => setLessonFilter(null)}
              className={`flex-shrink-0 rounded-full border px-3 py-1.5 text-[12px] font-medium transition-colors ${
                lessonFilter === null
                  ? "border-[#F0A0B5] bg-[#F0A0B5] text-white"
                  : "border-[#F0EBE8] bg-white text-[#6B6B6B]"
              }`}
            >
              전체
            </button>
            {lessonTypes.map((type) => (
              <button
                key={type}
                onClick={() => setLessonFilter(type)}
                className={`flex-shrink-0 rounded-full border px-3 py-1.5 text-[12px] font-medium transition-colors ${
                  lessonFilter === type
                    ? "border-[#F0A0B5] bg-[#F0A0B5] text-white"
                    : "border-[#F0EBE8] bg-white text-[#6B6B6B]"
                }`}
              >
                {type}
              </button>
            ))}
          </div>
        )}

        {/* Results */}
        <div className="flex-1 overflow-y-auto px-4 pb-6">
          {loading ? (
            <div className="py-16 text-center text-[13px] text-[#A0A0A0]">
              불러오는 중...
            </div>
          ) : results.length === 0 ? (
            <div className="py-16 text-center">
              <p className="mb-1 text-[14px] font-semibold text-[#2A2A2C]">
                {query || lessonFilter
                  ? "검색 결과가 없어요"
                  : "예정된 수업이 없어요"}
              </p>
              <p className="text-[12px] text-[#A0A0A0]">
                다른 키워드 또는 필터로 검색해보세요
              </p>
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {results.map((s) => {
                const d = new Date(s.classDate);
                const remaining = s.maxCapacity - s.currentCount;
                const isFull = remaining <= 0;
                return (
                  <button
                    key={s.id}
                    onClick={() => go(s)}
                    className="flex items-center gap-3 rounded-[14px] border border-[#F0EBE8] bg-white p-3 text-left transition-colors hover:border-[#FAD4DE]"
                  >
                    <div className="flex h-14 w-12 flex-shrink-0 flex-col items-center justify-center rounded-[12px] bg-[#FDEDF2]">
                      <span className="text-[10px] font-semibold text-[#D88A9E]">
                        {d.getMonth() + 1}월
                      </span>
                      <span className="text-[18px] font-extrabold leading-none tracking-[-0.03em] text-[#2A2A2C]">
                        {String(d.getDate()).padStart(2, "0")}
                      </span>
                      <span className="mt-0.5 text-[10px] text-[#A0A0A0]">
                        {WEEKDAY_KO[d.getDay()]}
                      </span>
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="text-[12px] font-semibold text-[#D88A9E]">
                        {s.startTime.slice(0, 5)} - {s.endTime.slice(0, 5)}
                      </div>
                      <div className="text-[14px] font-semibold tracking-[-0.01em] text-[#2A2A2C]">
                        {s.lessonTypeName}
                      </div>
                      <div className="mt-0.5 text-[12px] text-[#6B6B6B]">
                        {s.instructorName} 강사
                      </div>
                    </div>
                    <span
                      className={`flex-shrink-0 rounded-full px-2 py-1 text-[10px] font-semibold ${
                        isFull
                          ? "bg-[#FAF7F5] text-[#A0A0A0]"
                          : remaining <= 2
                            ? "bg-[#FEEAEA] text-[#E76F51]"
                            : "border border-[#FAD4DE] bg-white text-[#D88A9E]"
                      }`}
                    >
                      {isFull ? "마감" : `잔여 ${remaining}`}
                    </span>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
