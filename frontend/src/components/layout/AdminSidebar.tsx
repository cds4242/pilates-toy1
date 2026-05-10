"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/hooks/use-auth";

interface NavItem {
  href: string;
  label: string;
  iconPath: string;
  iconPath2?: string;
}

const SECTIONS: { label: string; items: NavItem[] }[] = [
  {
    label: "메인",
    items: [
      {
        href: "/dashboard",
        label: "대시보드",
        iconPath:
          "M3.75 6A2.25 2.25 0 0 1 6 3.75h2.25A2.25 2.25 0 0 1 10.5 6v2.25a2.25 2.25 0 0 1-2.25 2.25H6a2.25 2.25 0 0 1-2.25-2.25V6ZM3.75 15.75A2.25 2.25 0 0 1 6 13.5h2.25a2.25 2.25 0 0 1 2.25 2.25V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-2.25ZM13.5 6a2.25 2.25 0 0 1 2.25-2.25H18A2.25 2.25 0 0 1 20.25 6v2.25A2.25 2.25 0 0 1 18 10.5h-2.25a2.25 2.25 0 0 1-2.25-2.25V6ZM13.5 15.75a2.25 2.25 0 0 1 2.25-2.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25a2.25 2.25 0 0 1-2.25-2.25v-2.25Z",
      },
      {
        href: "/classes",
        label: "시간표",
        iconPath:
          "M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5",
      },
    ],
  },
  {
    label: "관리",
    items: [
      {
        href: "/members",
        label: "회원 관리",
        iconPath:
          "M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z",
      },
      {
        href: "/instructors",
        label: "강사 관리",
        iconPath:
          "M4.26 10.147a60.438 60.438 0 0 0-.491 6.347A48.62 48.62 0 0 1 12 20.904a48.62 48.62 0 0 1 8.232-4.41 60.46 60.46 0 0 0-.491-6.347m-15.482 0a50.636 50.636 0 0 0-2.658-.813A59.906 59.906 0 0 1 12 3.493a59.903 59.903 0 0 1 10.399 5.84c-.896.248-1.783.52-2.658.814m-15.482 0A50.717 50.717 0 0 1 12 13.489a50.702 50.702 0 0 1 7.74-3.342M6.75 15a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5Zm0 0v-3.675A55.378 55.378 0 0 1 12 8.443m-7.007 11.55A5.981 5.981 0 0 0 6.75 15.75v-1.5",
      },
      {
        href: "/membership-passes",
        label: "정기권",
        iconPath:
          "M16.5 6v.75m0 3v.75m0 3v.75m0 3V18m-9-5.25h5.25M7.5 15h3M3.375 5.25c-.621 0-1.125.504-1.125 1.125v3.026a2.999 2.999 0 0 1 0 5.198v3.026c0 .621.504 1.125 1.125 1.125h17.25c.621 0 1.125-.504 1.125-1.125v-3.026a2.999 2.999 0 0 1 0-5.198V6.375c0-.621-.504-1.125-1.125-1.125H3.375Z",
      },
    ],
  },
  {
    label: "시스템",
    items: [
      {
        href: "/settings",
        label: "설정",
        iconPath:
          "M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.325.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 0 1 1.37.49l1.296 2.247a1.125 1.125 0 0 1-.26 1.431l-1.003.827c-.293.241-.438.613-.43.992a7.723 7.723 0 0 1 0 .255c-.008.378.137.75.43.991l1.004.827c.424.35.534.955.26 1.43l-1.298 2.247a1.125 1.125 0 0 1-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.47 6.47 0 0 1-.22.128c-.331.183-.581.495-.644.869l-.213 1.281c-.09.543-.56.94-1.11.94h-2.594c-.55 0-1.019-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 0 1-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 0 1-1.369-.49l-1.297-2.247a1.125 1.125 0 0 1 .26-1.431l1.004-.827c.292-.24.437-.613.43-.991a6.932 6.932 0 0 1 0-.255c.007-.38-.138-.751-.43-.992l-1.004-.827a1.125 1.125 0 0 1-.26-1.43l1.297-2.247a1.125 1.125 0 0 1 1.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.086.22-.128.332-.183.582-.495.644-.869l.214-1.28Z",
        iconPath2: "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z",
      },
    ],
  },
];

function NavIcon({
  iconPath,
  iconPath2,
}: {
  iconPath: string;
  iconPath2?: string;
}) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.7}
      stroke="currentColor"
      className="h-[19px] w-[19px] flex-shrink-0"
    >
      <path strokeLinecap="round" strokeLinejoin="round" d={iconPath} />
      {iconPath2 && (
        <path strokeLinecap="round" strokeLinejoin="round" d={iconPath2} />
      )}
    </svg>
  );
}

function SidebarContent({
  pathname,
  user,
  logout,
  onNavigate,
}: {
  pathname: string;
  user: { name?: string } | null;
  logout: () => void;
  onNavigate?: () => void;
}) {
  const initial = user?.name?.charAt(0) || "관";
  return (
    <>
      {/* Logo */}
      <Link
        href="/dashboard"
        onClick={onNavigate}
        className="flex items-center gap-2.5 border-b border-[#F0EBE8] px-6 py-[22px] text-[16px] font-bold tracking-[-0.02em] text-[#2A2A2C] no-underline"
      >
        <span
          className="flex h-[30px] w-[30px] items-center justify-center rounded-[9px] text-[15px] font-extrabold italic text-white"
          style={{ background: "#F0A0B5", fontFamily: "Georgia, serif" }}
        >
          P
        </span>
        필라테스 OO점
      </Link>

      {/* Nav */}
      <nav className="flex flex-1 flex-col gap-0.5 overflow-y-auto px-3 py-3">
        {SECTIONS.map((section) => (
          <div key={section.label}>
            <div className="px-3 pt-3.5 pb-1.5 text-[10px] font-bold uppercase tracking-[0.1em] text-[#A0A0A0]">
              {section.label}
            </div>
            {section.items.map((item) => {
              const active =
                pathname === item.href ||
                pathname?.startsWith(item.href + "/");
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={onNavigate}
                  className={`flex cursor-pointer items-center gap-3 rounded-[12px] px-3 py-[11px] text-[14px] no-underline transition-all ${
                    active
                      ? "bg-[#FDEDF2] font-semibold text-[#D88A9E]"
                      : "font-medium text-[#6B6B6B] hover:bg-[#FFF7F4] hover:text-[#2A2A2C]"
                  }`}
                >
                  <NavIcon iconPath={item.iconPath} iconPath2={item.iconPath2} />
                  {item.label}
                  {active && (
                    <span className="ml-auto h-1.5 w-1.5 rounded-full bg-[#F0A0B5]" />
                  )}
                </Link>
              );
            })}
          </div>
        ))}
      </nav>

      {/* Footer */}
      <div className="flex items-center gap-3 border-t border-[#F0EBE8] p-4">
        <div className="flex h-[38px] w-[38px] flex-shrink-0 items-center justify-center rounded-full bg-[#FAD4DE] text-[14px] font-bold text-[#D88A9E]">
          {initial}
        </div>
        <div className="min-w-0 flex-1">
          <div className="truncate text-[13px] font-semibold tracking-[-0.01em] text-[#2A2A2C]">
            {user?.name || "관리자"}
          </div>
          <div className="mt-px text-[11px] text-[#A0A0A0]">관리자</div>
        </div>
        <button
          onClick={logout}
          aria-label="로그아웃"
          className="flex h-[30px] w-[30px] items-center justify-center rounded-[8px] border border-[#F0EBE8] bg-transparent text-[#A0A0A0] transition-all hover:border-[#FAD4DE] hover:text-[#D88A9E]"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={1.8}
            stroke="currentColor"
            className="h-[14px] w-[14px]"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M15.75 9V5.25A2.25 2.25 0 0 0 13.5 3h-6a2.25 2.25 0 0 0-2.25 2.25v13.5A2.25 2.25 0 0 0 7.5 21h6a2.25 2.25 0 0 0 2.25-2.25V15M12 9l-3 3m0 0 3 3m-3-3h12.75"
            />
          </svg>
        </button>
      </div>
    </>
  );
}

export function AdminSidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);

  const pageNames: Record<string, string> = {
    "/dashboard": "대시보드",
    "/members": "회원 관리",
    "/instructors": "강사 관리",
    "/classes": "시간표",
    "/membership-passes": "정기권",
    "/settings": "설정",
  };
  const currentPage =
    Object.entries(pageNames).find(([h]) => pathname?.startsWith(h))?.[1] ||
    "필라테스 OO점";

  return (
    <>
      {/* Desktop sidebar */}
      <aside className="fixed left-0 top-0 z-[200] hidden h-screen w-[248px] flex-col border-r border-[#F0EBE8] bg-white md:flex">
        <SidebarContent pathname={pathname} user={user} logout={logout} />
      </aside>

      {/* Mobile header */}
      <header className="sticky top-0 z-[100] flex items-center justify-between border-b border-[#F0EBE8] bg-white px-6 py-4 md:hidden">
        <button
          onClick={() => setMobileOpen(true)}
          aria-label="메뉴 열기"
          className="cursor-pointer p-1"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={1.5}
            stroke="currentColor"
            className="h-6 w-6 text-[#2A2A2C]"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5"
            />
          </svg>
        </button>
        <span className="text-[16px] font-bold text-[#2A2A2C]">
          {currentPage}
        </span>
        <span className="w-6" />
      </header>

      {/* Mobile drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-[250] md:hidden">
          <div
            className="absolute inset-0 bg-[rgba(45,30,30,0.3)]"
            onClick={() => setMobileOpen(false)}
          />
          <aside className="absolute bottom-0 left-0 top-0 flex w-[280px] flex-col border-r border-[#F0EBE8] bg-white shadow-xl">
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
