"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/hooks/use-auth";
import {
  LayoutDashboard,
  Users,
  Calendar,
  BarChart3,
  LogOut,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";

const items = [
  { href: "/dashboard", icon: LayoutDashboard, label: "대시보드" },
  { href: "/members", icon: Users, label: "회원 관리" },
  { href: "/classes", icon: Calendar, label: "수업 관리" },
  { href: "/statistics", icon: BarChart3, label: "통계" },
];

export function AdminSidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  return (
    <aside className="hidden md:flex md:w-60 flex-col border-r bg-muted/30 min-h-screen">
      <div className="p-4 border-b">
        <Link href="/dashboard" className="font-semibold text-lg">
          Pilates Studio
        </Link>
        <p className="text-xs text-muted-foreground mt-1">
          {user?.name || "관리자"}
        </p>
      </div>
      <nav className="flex-1 p-2 space-y-1">
        {items.map(({ href, icon: Icon, label }) => {
          const active = pathname === href;
          return (
            <Link
              key={href}
              href={href}
              className={cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors",
                active
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
              )}
            >
              <Icon className="h-4 w-4" />
              {label}
            </Link>
          );
        })}
      </nav>
      <div className="p-2 border-t">
        <Button
          variant="ghost"
          className="w-full justify-start"
          onClick={logout}
        >
          <LogOut className="h-4 w-4 mr-2" />
          로그아웃
        </Button>
      </div>
    </aside>
  );
}
