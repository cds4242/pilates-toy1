"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

export default function RootPage() {
  const router = useRouter();
  const [checked, setChecked] = useState(false);

  useEffect(() => {
    // Wait a tick for Zustand persist to hydrate from localStorage
    const timer = setTimeout(() => {
      try {
        const raw = localStorage.getItem("auth-storage");
        if (!raw) {
          router.replace("/login");
          setChecked(true);
          return;
        }
        const { state } = JSON.parse(raw);
        if (!state?.accessToken) {
          router.replace("/login");
          setChecked(true);
          return;
        }
        const role = state.user?.role;
        if (role === "MEMBER") router.replace("/home");
        else if (role === "INSTRUCTOR") router.replace("/instructor/schedule");
        else if (role === "ADMIN" || role === "SUPER_ADMIN") router.replace("/dashboard");
        else router.replace("/login");
        setChecked(true);
      } catch {
        router.replace("/login");
        setChecked(true);
      }
    }, 50);
    return () => clearTimeout(timer);
  }, [router]);

  if (checked) return null;

  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="animate-pulse text-muted-foreground">로딩 중...</div>
    </div>
  );
}
