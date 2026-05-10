"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const items = [
  {
    href: "/home",
    label: "홈",
    path: "m2.25 12 8.954-8.955a1.126 1.126 0 0 1 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25",
  },
  {
    href: "/schedule",
    label: "예약",
    path: "M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5",
  },
  {
    href: "/membership",
    label: "정기권",
    path: "M16.5 6v.75m0 3v.75m0 3v.75m0 3V18m-9-5.25h5.25M7.5 15h3M3.375 5.25c-.621 0-1.125.504-1.125 1.125v3.026a3 3 0 0 1 0 5.198v3.026c0 .621.504 1.125 1.125 1.125h17.25c.621 0 1.125-.504 1.125-1.125v-3.026a3 3 0 0 1 0-5.198V6.375c0-.621-.504-1.125-1.125-1.125H3.375Z",
  },
  {
    href: "/profile",
    label: "마이",
    path: "M15.75 6a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0ZM4.501 20.118a7.5 7.5 0 0 1 14.998 0A17.933 17.933 0 0 1 12 21.75c-2.676 0-5.216-.584-7.499-1.632Z",
  },
];

export function MobileTabBar() {
  const pathname = usePathname();

  return (
    <nav
      className="fixed bottom-0 left-1/2 z-[100] flex w-full max-w-[480px] -translate-x-1/2 border-t border-[#F0EBE8] bg-white/92 backdrop-blur-md"
      style={{
        background: "rgba(255,255,255,0.92)",
        paddingBottom: "env(safe-area-inset-bottom, 0)",
      }}
    >
      {items.map(({ href, label, path }) => {
        const active = pathname === href || pathname?.startsWith(href + "/");
        return (
          <Link
            key={href}
            href={href}
            className={`relative flex flex-1 flex-col items-center gap-[3px] px-0 pb-3 pt-2.5 text-[10px] no-underline transition-colors duration-200 ${
              active
                ? "font-semibold text-[#D88A9E]"
                : "font-medium text-[#A0A0A0]"
            }`}
          >
            {active && (
              <span className="absolute left-1/2 top-0 h-[3px] w-6 -translate-x-1/2 rounded-b-[4px] bg-[#F0A0B5]" />
            )}
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={1.8}
              stroke="currentColor"
              className="h-[22px] w-[22px]"
            >
              <path strokeLinecap="round" strokeLinejoin="round" d={path} />
            </svg>
            {label}
          </Link>
        );
      })}
    </nav>
  );
}
