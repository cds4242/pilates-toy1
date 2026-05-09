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

export default function MemberHomePage() {
  const router = useRouter();
  const { user, accessToken } = useAuthStore();

  useEffect(() => {
    if (!accessToken) router.replace("/login");
  }, [accessToken, router]);
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
  const upcomingReservations = reservations
    .filter((r) => r.status === "CONFIRMED")
    .slice(0, 3);

  const today = new Date();
  const dateStr = `${today.getMonth() + 1}월 ${today.getDate()}일 ${["일", "월", "화", "수", "목", "금", "토"][today.getDay()]}요일`;

  return (
    <div className="max-w-[560px] mx-auto min-h-screen bg-white pb-20">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-white px-6 py-4 flex items-center justify-between border-b border-[var(--color-border)]">
        <span className="text-[20px] font-bold text-[var(--color-text-title)]">
          필라테스 OO점
        </span>
        <button onClick={() => router.push("/reservations")} className="relative text-[var(--color-text-title)]">
          <Bell className="h-6 w-6" />
          <span className="absolute -top-1 -right-1 bg-[var(--color-error)] text-white text-[10px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
            2
          </span>
        </button>
      </header>

      <main className="p-6 flex flex-col gap-6">
        {/* 인사 */}
        <div>
          <h1 className="text-[22px] font-semibold text-[var(--color-text-title)]">
            안녕하세요, {member?.name || "회원"}님
          </h1>
          <p className="text-[15px] text-[var(--color-text-body)] mt-1">{dateStr}</p>
        </div>

        {/* 정기권 카드 */}
        {loading ? (
          <div className="rounded-[18px] bg-[var(--color-pilates-light)] p-5 animate-pulse h-32" />
        ) : activeMembership ? (
          <PassCard
            name={activeMembership.passName}
            remaining={activeMembership.remainingCount}
            total={activeMembership.totalCount}
            unlimited={activeMembership.unlimited}
            endDate={activeMembership.endDate}
          />
        ) : (
          <div className="rounded-[18px] bg-[var(--color-bg-section)] p-5 text-center text-[15px] text-[var(--color-text-sub)]">
            활성 정기권이 없습니다
          </div>
        )}

        {/* 다음 예약 */}
        <div>
          <h2 className="text-[18px] font-bold text-[var(--color-text-title)] mb-3">
            다음 예약
          </h2>
          {upcomingReservations.length > 0 ? (
            <div className="flex flex-col gap-3">
              {upcomingReservations.map((r) => (
                <div
                  key={r.id}
                  className="rounded-[18px] border border-[var(--color-border)] p-4 flex items-center gap-4"
                >
                  <div className="w-1 h-12 rounded-full bg-[var(--color-pilates)]" />
                  <div className="flex-1">
                    <p className="text-[15px] font-semibold text-[var(--color-text-title)]">
                      {r.classDate} {formatTime(r.startTime)}
                    </p>
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
      </main>

      <MobileTabBar />
    </div>
  );
}
