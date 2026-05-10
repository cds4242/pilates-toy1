"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ChevronRight, Ticket, CreditCard, Bell, Lock, LogOut, UserX, Calendar } from "lucide-react";
import { useAuthStore } from "@/lib/store/auth-store";
import { memberApi } from "@/lib/api/member";
import { api } from "@/lib/api/client";
import type { Member, Membership, Reservation } from "@/lib/types/domain";
import { PassCard } from "@/components/design/PassCard";
import { MobileTabBar } from "@/components/design/MobileTabBar";
import { toast } from "sonner";

interface AttendanceRecord {
  id: number;
  classDate: string;
  startTime: string;
  lessonTypeName: string;
  instructorName: string;
  status: string;
}

function MonthlyCalendar({ records }: { records: { classDate: string; status: string }[] }) {
  const today = new Date();
  const year = today.getFullYear();
  const month = today.getMonth();
  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  const recordMap = new Map<number, string>();
  records.forEach(r => {
    const d = new Date(r.classDate);
    if (d.getFullYear() === year && d.getMonth() === month) {
      recordMap.set(d.getDate(), r.status);
    }
  });

  return (
    <div>
      <p className="text-[14px] font-semibold text-[var(--color-text-title)] mb-2">{month + 1}월 출석 현황</p>
      <div className="grid grid-cols-7 gap-1 text-center text-[11px] text-[var(--color-text-sub)] mb-1">
        {["일","월","화","수","목","금","토"].map(d => <div key={d}>{d}</div>)}
      </div>
      <div className="grid grid-cols-7 gap-1">
        {Array.from({ length: firstDay }).map((_, i) => <div key={`e-${i}`} />)}
        {Array.from({ length: daysInMonth }).map((_, i) => {
          const day = i + 1;
          const status = recordMap.get(day);
          const isToday = day === today.getDate();
          let dotCls = "bg-gray-200";
          if (status === "PRESENT" || status === "LATE") dotCls = "bg-[var(--color-pilates)]";
          else if (status === "NO_SHOW") dotCls = "bg-[var(--color-error)]";
          return (
            <div key={day} className="flex flex-col items-center py-0.5">
              <span className={`text-[11px] ${isToday ? "font-bold text-[var(--color-pilates-dark)]" : "text-[var(--color-text-sub)]"}`}>{day}</span>
              <div className={`w-2 h-2 rounded-full mt-0.5 ${dotCls}`} />
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default function MyPage() {
  const router = useRouter();
  const { logout } = useAuthStore();
  const [member, setMember] = useState<Member | null>(null);
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [attendances, setAttendances] = useState<AttendanceRecord[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);

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
      } catch { /* empty */ }
      // 출석 이력 시도
      try {
        const att = await api<AttendanceRecord[]>("get", "/api/members/me/attendances");
        setAttendances(att);
      } catch { /* API 없을 수 있음 */ }
    }
    load();
  }, []);

  const activeMembership = memberships.find((m) => m.status === "ACTIVE");

  // 최근 예약에서 출석 이력 대체 (출석 API 없는 경우)
  const recentHistory = attendances.length > 0
    ? attendances.slice(0, 5)
    : reservations
        .filter((r) => r.status !== "CONFIRMED")
        .slice(0, 5)
        .map((r) => ({
          id: r.id,
          classDate: r.classDate,
          startTime: r.startTime,
          lessonTypeName: r.lessonTypeName,
          instructorName: r.instructorName,
          status: r.status === "NO_SHOW" ? "NO_SHOW" : r.status === "CANCELLED" ? "CANCELLED" : "PRESENT",
        }));

  const menuItems = [
    { icon: Ticket, label: "수강권 구매", href: "/membership" },
    { icon: CreditCard, label: "결제 내역", href: "/reservations" },
    { icon: Lock, label: "비밀번호 변경", href: "/reset-password" },
    { icon: Bell, label: "알림 설정", href: "/reservations" },
  ];

  return (
    <div className="max-w-[560px] mx-auto min-h-screen bg-white pb-20">
      <header className="sticky top-0 z-50 bg-white px-6 py-4 border-b border-[var(--color-border)]">
        <h1 className="text-[20px] font-bold text-[var(--color-text-title)]">마이페이지</h1>
      </header>
      <main className="p-6 flex flex-col gap-6">
        {/* 프로필 */}
        <div className="text-center py-5">
          <div className="inline-block mb-3">
            <div className="w-[72px] h-[72px] rounded-full bg-[var(--color-pilates)] flex items-center justify-center text-white text-[24px] font-bold">
              {member?.name?.charAt(0) || "회"}
            </div>
          </div>
          <h2 className="text-[20px] font-bold text-[var(--color-text-title)]">{member?.name || "회원"}</h2>
          <p className="text-[13px] text-[var(--color-text-sub)] mt-1">가입일 {(() => { const d = member?.createdAt ? new Date(member.createdAt) : null; return d ? `${d.getFullYear()}년 ${d.getMonth()+1}월 ${d.getDate()}일` : ""; })()}</p>
        </div>

        {/* 정기권 카드 */}
        {activeMembership ? (
          <PassCard name={activeMembership.passName} remaining={activeMembership.remainingCount} total={activeMembership.totalCount} unlimited={activeMembership.unlimited} endDate={activeMembership.endDate} compact />
        ) : (
          <div className="rounded-[18px] bg-[var(--color-bg-section)] p-5 text-center text-[15px] text-[var(--color-text-sub)]">활성 수강권이 없습니다</div>
        )}

        {/* 출석 이력 — 달력 히트맵 */}
        <div>
          <h2 className="text-[18px] font-bold text-[var(--color-text-title)] mb-3">출석 이력</h2>
          {recentHistory.length === 0 ? (
            <div className="rounded-[18px] bg-[var(--color-bg-section)] p-5 text-center">
              <Calendar className="h-8 w-8 text-[var(--color-text-sub)] mx-auto mb-2" />
              <p className="text-[14px] text-[var(--color-text-sub)]">아직 출석 기록이 없습니다</p>
              <p className="text-[12px] text-[var(--color-text-sub)] mt-1">수업 참석 시 강사가 출석을 체크합니다</p>
            </div>
          ) : (
            <div className="rounded-[18px] border border-[var(--color-border)] p-4">
              <MonthlyCalendar records={recentHistory} />
            </div>
          )}
        </div>

        {/* 메뉴 */}
        <div className="flex flex-col">
          {menuItems.map(({ icon: Icon, label, href }) => (
            <button key={label} onClick={() => router.push(href)} className="flex items-center gap-4 py-4 border-b border-[var(--color-border)] hover:text-[var(--color-pilates-dark)] transition-colors">
              <Icon className="h-[18px] w-[18px] shrink-0 text-[var(--color-text-body)]" />
              <span className="flex-1 text-left text-[15px] text-[var(--color-text-title)]">{label}</span>
              <ChevronRight className="h-4 w-4 text-[var(--color-text-sub)]" />
            </button>
          ))}
          <button onClick={() => { logout(); router.push("/login"); }} className="flex items-center gap-4 py-4">
            <LogOut className="h-[18px] w-[18px] shrink-0 text-[var(--color-text-body)]" />
            <span className="flex-1 text-left text-[15px] text-[var(--color-text-title)]">로그아웃</span>
            <ChevronRight className="h-4 w-4 text-[var(--color-text-sub)]" />
          </button>
        </div>

        {/* 계정 — 탈퇴 별도 섹션 */}
        <div className="mt-8">
          <div className="border-t border-[var(--color-border)] mb-4" />
          <h3 className="text-[14px] font-semibold text-[var(--color-text-sub)] mb-3">계정</h3>
          <button onClick={async () => {
            if (!confirm("정말 탈퇴하시겠습니까?\n\n• 보유 중인 수강권이 모두 소멸됩니다\n• 예약된 수업이 모두 취소됩니다\n• 탈퇴 후 재가입이 가능하지만 이전 데이터는 복구되지 않습니다\n\n되돌릴 수 없습니다.")) return;
            try {
              await api("delete", "/api/members/me", { reason: "회원 요청" });
              toast.success("탈퇴 완료"); logout(); router.push("/login");
            } catch (err: unknown) { toast.error(err instanceof Error ? err.message : "탈퇴 실패"); }
          }} className="flex items-center gap-4 py-3 rounded-[12px] px-3 hover:bg-red-50 transition-colors">
            <UserX className="h-[18px] w-[18px] shrink-0 text-[var(--color-error)]" />
            <span className="flex-1 text-left text-[15px] text-[var(--color-error)]">회원 탈퇴</span>
            <ChevronRight className="h-4 w-4 text-[var(--color-text-sub)]" />
          </button>
        </div>
      </main>
      <MobileTabBar />
    </div>
  );
}
