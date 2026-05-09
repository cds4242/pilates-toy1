"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/store/auth-store";

export default function InstructorLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const { accessToken } = useAuthStore();

  useEffect(() => {
    if (!accessToken) router.replace("/instructor-login");
  }, [accessToken, router]);

  return (
    <div className="min-h-screen bg-instructor-light">
      {children}
    </div>
  );
}
