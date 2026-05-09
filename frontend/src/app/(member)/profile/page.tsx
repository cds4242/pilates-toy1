"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ChevronRight, Ticket, CreditCard, Bell, LogOut, UserX } from "lucide-react";
import { useAuthStore } from "@/lib/store/auth-store";
import { memberApi } from "@/lib/api/member";
import type { Member, Membership } from "@/lib/types/domain";
import { PassCard } from "@/components/design/PassCard";
import { MobileTabBar } from "@/components/design/MobileTabBar";

export default function MyPage() {
  const router = useRouter();
  const { logout } = useAuthStore();
  const [member, setMember] = useState<Member | null>(null);
  const [memberships, setMemberships] = useState<Membership[]>([]);

  useEffect(() => {
    async function load() {
      try {
        const [me, ms] = await Promise.all([memberApi.getMe(), memberApi.getMemberships()]);
        setMember(me);
        setMemberships(ms);
      } catch { /* empty */ }
    }
    load();
  }, []);

  const activeMembership = memberships.find((m) => m.status === "ACTIVE");

  const menuItems = [
    { icon: Ticket, label: "정기권 구매", href: "/membership" },
    { icon: CreditCard, label: "결제 내역", href: "/reservations" },
    { icon: Bell, label: "알림 설정", href: "#" },
  ];

  return (
    <div className="max-w-[480px] mx-auto min-h-screen bg-white pb-20">
      <header className="sticky top-0 z-50 bg-white px-6 py-4 border-b border-[var(--color-border)]">
        <h1 className="text-[20px] font-bold text-[var(--color-text-title)]">마이페이지</h1>
      </header>
      <main className="p-6 flex flex-col gap-6">
        <div className="text-center py-5">
          <div className="inline-block mb-3">
            <div className="w-[72px] h-[72px] rounded-full bg-[var(--color-pilates)] flex items-center justify-center text-white text-[24px] font-bold">
              {member?.name?.charAt(0) || "회"}
            </div>
          </div>
          <h2 className="text-[20px] font-bold text-[var(--color-text-title)]">{member?.name || "회원"}</h2>
          <p className="text-[13px] text-[var(--color-text-sub)] mt-1">가입일 {member?.createdAt?.slice(0, 10) || ""}</p>
        </div>
        {activeMembership ? (
          <PassCard name={activeMembership.passName} remaining={activeMembership.remainingCount} total={activeMembership.totalCount} unlimited={activeMembership.unlimited} endDate={activeMembership.endDate} compact />
        ) : (
          <div className="rounded-[18px] bg-[var(--color-bg-section)] p-5 text-center text-[15px] text-[var(--color-text-sub)]">활성 정기권이 없습니다</div>
        )}
        <div className="flex flex-col">
          {menuItems.map(({ icon: Icon, label, href }) => (
            <button key={label} onClick={() => router.push(href)} className="flex items-center gap-4 py-4 border-b border-[var(--color-border)] hover:text-[var(--color-pilates-dark)] transition-colors">
              <Icon className="h-[18px] w-[18px] shrink-0 text-[var(--color-text-body)]" />
              <span className="flex-1 text-left text-[15px] text-[var(--color-text-title)]">{label}</span>
              <ChevronRight className="h-4 w-4 text-[var(--color-text-sub)]" />
            </button>
          ))}
          <button onClick={() => { logout(); router.push("/login"); }} className="flex items-center gap-4 py-4 border-b border-[var(--color-border)]">
            <LogOut className="h-[18px] w-[18px] shrink-0 text-[var(--color-text-body)]" />
            <span className="flex-1 text-left text-[15px] text-[var(--color-text-title)]">로그아웃</span>
            <ChevronRight className="h-4 w-4 text-[var(--color-text-sub)]" />
          </button>
          <button className="flex items-center gap-4 py-4">
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
