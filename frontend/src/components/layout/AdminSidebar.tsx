"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/hooks/use-auth";
import {
  LayoutDashboard, Users, UserCheck, Calendar, Settings, Menu, X,
} from "lucide-react";

const items = [
  { href: "/dashboard", icon: LayoutDashboard, label: "대시보드" },
  { href: "/members", icon: Users, label: "회원 관리" },
  { href: "/instructors", icon: UserCheck, label: "강사 관리" },
  { href: "/classes", icon: Calendar, label: "시간표" },
  { href: "/settings", icon: Settings, label: "설정" },
];

function SidebarContent({ pathname, user, logout, onNavigate }: {
  pathname: string;
  user: { name?: string } | null;
  logout: () => void;
  onNavigate?: () => void;
}) {
  return (
    <>
      <div className="px-6 py-5 border-b border-[var(--color-border)]">
        <Link href="/dashboard" className="text-[20px] font-bold text-[var(--color-text-title)]" onClick={onNavigate}>
          필라테스 OO점
        </Link>
      </div>
      <nav className="flex-1 py-3 px-3">
        {items.map(({ href, icon: Icon, label }) => {
          const active = pathname === href;
          return (
            <Link
              key={href}
              href={href}
              onClick={onNavigate}
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
      <div className="px-4 py-4 border-t border-[var(--color-border)]">
        <div className="flex items-center gap-3 mb-3">
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
        <button onClick={logout} className="w-full text-left px-3 py-2 rounded-[8px] text-[13px] text-[var(--color-text-sub)] hover:bg-[var(--color-bg-section)] hover:text-[var(--color-text-body)] transition-colors">
          로그아웃
        </button>
      </div>
    </>
  );
}

export function AdminSidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <>
      {/* PC 사이드바 */}
      <aside className="hidden md:flex md:w-[240px] flex-col border-r border-[var(--color-border)] bg-white min-h-screen fixed left-0 top-0">
        <SidebarContent pathname={pathname} user={user} logout={logout} />
      </aside>

      {/* 모바일 헤더 */}
      <header className="md:hidden sticky top-0 z-40 bg-white border-b border-[var(--color-border)] px-4 py-3 flex items-center justify-between">
        <button onClick={() => setMobileOpen(true)} className="p-1">
          <Menu className="h-6 w-6 text-[var(--color-text-title)]" />
        </button>
        <span className="text-[17px] font-bold text-[var(--color-text-title)]">{(() => { const pageNames: Record<string, string> = { "/dashboard": "대시보드", "/members": "회원 관리", "/instructors": "강사 관리", "/classes": "시간표", "/settings": "설정" }; return pageNames[pathname] || "필라테스 OO점"; })()}</span>
        <div className="w-8" />
      </header>

      {/* 모바일 슬라이드 사이드바 */}
      {mobileOpen && (
        <div className="md:hidden fixed inset-0 z-50">
          <div className="absolute inset-0 bg-black/40" onClick={() => setMobileOpen(false)} />
          <aside className="absolute left-0 top-0 bottom-0 w-[280px] bg-white flex flex-col shadow-xl animate-in slide-in-from-left">
            <div className="flex justify-end p-3">
              <button onClick={() => setMobileOpen(false)}>
                <X className="h-5 w-5 text-[var(--color-text-sub)]" />
              </button>
            </div>
            <SidebarContent
              pathname={pathname}
              user={user}
              logout={logout}
              onNavigate={() => setMobileOpen(false)}
            />
          </aside>
        </div>
      )}
    </>
  );
}
