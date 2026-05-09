"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/store/auth-store";

export default function RootPage() {
  const router = useRouter();
  const { user, accessToken } = useAuthStore();

  useEffect(() => {
    if (!accessToken) {
      router.replace("/login");
      return;
    }
    switch (user?.role) {
      case "MEMBER":
        router.replace("/home");
        break;
      case "INSTRUCTOR":
        router.replace("/instructor/schedule");
        break;
      case "ADMIN":
      case "SUPER_ADMIN":
        router.replace("/dashboard");
        break;
      default:
        router.replace("/login");
    }
  }, [accessToken, user, router]);

  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="animate-pulse text-muted-foreground">로딩 중...</div>
    </div>
  );
}
