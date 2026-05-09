"use client";

import Link from "next/link";
import { useAuth } from "@/lib/hooks/use-auth";
import { Button } from "@/components/ui/button";
import { LogOut, User } from "lucide-react";

export function Header() {
  const { user, isAuthenticated, logout } = useAuth();

  return (
    <header className="sticky top-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="flex h-14 items-center px-4 max-w-screen-xl mx-auto">
        <Link href="/" className="font-semibold text-lg tracking-tight">
          Pilates Studio
        </Link>
        <div className="ml-auto flex items-center gap-2">
          {isAuthenticated() ? (
            <>
              <span className="text-sm text-muted-foreground">
                {user?.name || user?.role}
              </span>
              <Button variant="ghost" size="icon" onClick={logout}>
                <LogOut className="h-4 w-4" />
              </Button>
            </>
          ) : (
            <Link href="/login">
              <Button variant="ghost" size="sm">
                <User className="h-4 w-4 mr-1" />
                로그인
              </Button>
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
