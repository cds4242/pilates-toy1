"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Home, Calendar, Ticket, User } from "lucide-react";

const items = [
  { href: "/home", icon: Home, label: "홈" },
  { href: "/schedule", icon: Calendar, label: "예약" },
  { href: "/membership", icon: Ticket, label: "수강권" },
  { href: "/profile", icon: User, label: "마이" },
];

export function MobileTabBar() {
  const pathname = usePathname();

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 border-t border-[var(--color-border)] bg-white md:hidden">
      <div className="mx-auto flex h-16 max-w-[480px] items-center justify-around">
        {items.map(({ href, icon: Icon, label }) => {
          const active = pathname === href;
          return (
            <Link
              key={href}
              href={href}
              className={`flex flex-col items-center gap-0.5 text-[11px] ${
                active
                  ? "text-[var(--color-pilates-dark)] font-semibold"
                  : "text-[var(--color-text-sub)]"
              }`}
            >
              <Icon className="h-5 w-5" />
              {label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
