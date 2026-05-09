"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/hooks/use-auth";
import {
  LayoutDashboard, Users, UserCheck, Ticket, Calendar, BarChart3, Settings,
} from "lucide-react";

const items = [
  { href: "/dashboard", icon: LayoutDashboard, label: "대시보드" },
  { href: "/members", icon: Users, label: "회원 관리" },
  { href: "/classes", icon: Calendar, label: "시간표" },
  { href: "/statistics", icon: BarChart3, label: "통계" },
];

export function AdminSidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  return (
    <aside className="hidden md:flex md:w-[240px] flex-col border-r border-[var(--color-border)] bg-white min-h-screen fixed left-0 top-0">
      {/* 로고 */}
      <div className="px-6 py-5 border-b border-[var(--color-border)]">
        <Link href="/dashboard" className="text-[20px] font-bold text-[var(--color-text-title)]">
          필라테스 OO점
        </Link>
      </div>

      {/* 네비게이션 */}
      <nav className="flex-1 py-3 px-3">
        {items.map(({ href, icon: Icon, label }) => {
          const active = pathname === href;
          return (
            <Link
              key={href}
              href={href}
              className={`flex items-center gap-3 rounded-[8px] px-4 py-3 mb-1 text-[15px] transition-all ${
                active
                  ? "bg-[var(--color-pilates-light)] text-[var(--color-pilates-dark)] font-semibold"
                  : "text-[var(--color-text-body)] hover:bg-[var(--color-bg-section)]"
              }`}
            >
              <Icon className="h-5 w-5" />
              {label}
            </Link>
          );
        })}
      </nav>

      {/* 사용자 정보 */}
      <div className="px-4 py-4 border-t border-[var(--color-border)] flex items-center gap-3">
        <div className="w-8 h-8 rounded-full bg-[var(--color-pilates-light)] flex items-center justify-center text-[13px] font-bold text-[var(--color-pilates-dark)]">
          {user?.name?.charAt(0) || "관"}
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-[13px] font-semibold text-[var(--color-text-title)] truncate">
            {user?.name || "관리자"}
          </p>
          <p className="text-[11px] text-[var(--color-text-sub)]">관리자</p>
        </div>
      </div>
    </aside>
  );
}
