"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Home, Calendar, Ticket, User } from "lucide-react";
import { cn } from "@/lib/utils";

const items = [
  { href: "/home", icon: Home, label: "홈" },
  { href: "/schedule", icon: Calendar, label: "시간표" },
  { href: "/membership", icon: Ticket, label: "정기권" },
  { href: "/profile", icon: User, label: "내 정보" },
];

export function BottomNav() {
  const pathname = usePathname();

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 border-t bg-background md:hidden">
      <div className="flex h-16 items-center justify-around">
        {items.map(({ href, icon: Icon, label }) => {
          const active = pathname === href;
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                "flex flex-col items-center gap-0.5 text-xs",
                active
                  ? "text-foreground font-medium"
                  : "text-muted-foreground"
              )}
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
