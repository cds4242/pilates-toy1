"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Bell } from "lucide-react";
import { useAuthStore } from "@/lib/store/auth-store";
import { memberApi } from "@/lib/api/member";
import type { Member, Membership, Reservation } from "@/lib/types/domain";
import { PassCard } from "@/components/design/PassCard";
import { MobileTabBar } from "@/components/design/MobileTabBar";
import { formatTime } from "@/lib/utils/format";
import { toast } from "sonner";
import { usePageTitle } from "@/lib/hooks/use-page-title";

export default function MemberHomePage() {
  usePageTitle("홈");
  const router = useRouter();
  const { user, accessToken, _hydrated } = useAuthStore();

  useEffect(() => {
    if (!_hydrated) return;
    if (!accessToken) router.replace("/login");
  }, [accessToken, router, _hydrated]);
  const [member, setMember] = useState<Member | null>(null);
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const [me, ms, rs] = await Promise.all([
          memberApi.getMe(),
          memberApi.getMemberships(),
          memberApi.getReservations(),
        ]);
        setMember(me);
        setMemberships(ms);
        setReservations(rs);
      } catch {
        // empty
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const activeMembership = memberships.find((m) => m.status === "ACTIVE");
  const todayStr = new Date().toISOString().split("T")[0];
  const upcomingReservations = reservations
    .filter((r) => r.status === "CONFIRMED" && r.classDate >= todayStr)
    .sort((a, b) => a.classDate.localeCompare(b.classDate) || a.startTime.localeCompare(b.startTime))
    .slice(0, 3);

  const today = new Date();
  const dateStr = `${today.getMonth() + 1}월 ${today.getDate()}일 ${["일", "월", "화", "수", "목", "금", "토"][today.getDay()]}요일`;

  const hour = new Date().getHours();
  const greeting = hour < 12 ? "좋은 아침이에요" : hour < 17 ? "오늘도 화이팅" : "수고하셨어요";

  const daysLeft = activeMembership ? Math.ceil((new Date(activeMembership.endDate).getTime() - Date.now()) / 86400000) : 0;

  function getTimeUntil(dateStr: string, timeStr: string) {
    const target = new Date(`${dateStr}T${timeStr}`);
    const diff = target.getTime() - Date.now();
    if (diff < 0) return "진행 중";
    const hours = Math.floor(diff / 3600000);
    const mins = Math.floor((diff % 3600000) / 60000);
    if (hours > 24) return `${Math.floor(hours/24)}일 후`;
    if (hours > 0) return `${hours}시간 ${mins}분 후`;
    return `${mins}분 후`;
  }

  const isNewMember = !loading && !activeMembership && upcomingReservations.length === 0;

  return (
    <div className="max-w-[560px] mx-auto min-h-screen bg-white pb-20">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white px-6 py-4 flex items-center justify-between border-b border-[var(--color-border)]">
        <span className="text-[20px] font-bold text-[var(--color-text-title)]">
          필라테스 OO점
        </span>
        <button onClick={() => router.push("/reservations")} className="relative text-[var(--color-text-title)]">
          <Bell className="h-6 w-6" />
        </button>
      </header>

      <main className="p-6 flex flex-col gap-6">
        {/* 인사 */}
        <div>
          <h1 className="text-[22px] font-semibold text-[var(--color-text-title)]">
            {greeting}, {member?.name || "회원"}님
          </h1>
          <p className="text-[15px] text-[var(--color-text-body)] mt-1">{dateStr}</p>
        </div>

        {isNewMember ? (
          /* 신규 회원 웰컴 카드 */
          <div className="rounded-[18px] bg-gradient-to-br from-[var(--color-pilates-light)] to-white p-6 text-center">
            <div className="text-[40px] mb-3">🧘‍♀️</div>
            <h3 className="text-[18px] font-bold text-[var(--color-text-title)] mb-2">첫 수업을 시작해보세요!</h3>
            <p className="text-[14px] text-[var(--color-text-body)] mb-4">수강권을 구매하고 원하는 시간에<br/>수업을 예약할 수 있어요</p>
            <Link href="/membership" className="inline-block bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] text-[var(--color-text-title)] rounded-[8px] px-6 py-3 text-[15px] font-semibold transition-colors">
              수강권 둘러보기
            </Link>
          </div>
        ) : (
          <>
            {/* 정기권 카드 */}
            {loading ? (
              <div className="rounded-[18px] bg-[var(--color-pilates-light)] p-5 animate-pulse h-32" />
            ) : activeMembership ? (
              <div>
                <PassCard
                  name={activeMembership.passName}
                  remaining={activeMembership.remainingCount}
                  total={activeMembership.totalCount}
                  unlimited={activeMembership.unlimited}
                  endDate={activeMembership.endDate}
                />
                {!activeMembership.unlimited && (
                  <div className="mt-2 px-2">
                    <div className="flex justify-between text-[12px] text-[var(--color-text-sub)] mb-1">
                      <span>잔여 {activeMembership.remainingCount}회</span>
                      <span>총 {activeMembership.totalCount}회</span>
                    </div>
                    <div className="w-full h-[4px] bg-gray-200 rounded-full">
                      <div className="h-full rounded-full bg-[var(--color-pilates)]" style={{ width: `${(activeMembership.remainingCount / activeMembership.totalCount) * 100}%` }} />
                    </div>
                  </div>
                )}
                <p className="text-[12px] text-[var(--color-text-sub)] text-right mt-1">D-{daysLeft}일 남음</p>
              </div>
            ) : (
              <div className="rounded-[18px] bg-[var(--color-bg-section)] p-5 text-center text-[15px] text-[var(--color-text-sub)]">
                활성 수강권이 없습니다
              </div>
            )}

            {/* 다음 예약 */}
            <div>
              <h2 className="text-[18px] font-bold text-[var(--color-text-title)] mb-3">
                다음 예약
              </h2>
              {upcomingReservations.length > 0 ? (
                <div className="flex flex-col gap-3">
                  {upcomingReservations.map((r, idx) => (
                    <div
                      key={r.id}
                      className="rounded-[18px] border border-[var(--color-border)] p-4 flex items-center gap-4"
                    >
                      <div className="w-1 h-12 rounded-full bg-[var(--color-pilates)]" />
                      <div className="flex-1">
                        <p className="text-[15px] font-semibold text-[var(--color-text-title)]">
                          {r.classDate} {formatTime(r.startTime)}
                        </p>
                        {idx === 0 && (
                          <span className="text-[12px] text-[var(--color-pilates-dark)] font-semibold">{getTimeUntil(r.classDate, r.startTime)}</span>
                        )}
                        <p className="text-[13px] text-[var(--color-text-body)]">
                          {r.lessonTypeName} · {r.instructorName}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-[15px] text-[var(--color-text-sub)]">
                  예정된 예약이 없습니다
                </p>
              )}
            </div>

            {/* CTA */}
            <Link href="/schedule">
              <button className="w-full bg-[var(--color-pilates)] hover:bg-[var(--color-pilates-dark)] text-[var(--color-text-title)] rounded-[8px] py-4 text-[16px] font-semibold transition-all">
                수업 예약하기
              </button>
            </Link>
          </>
        )}
      </main>

      <MobileTabBar />
    </div>
  );
}
