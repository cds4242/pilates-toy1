"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { AdminSidebar } from "@/components/layout/AdminSidebar";
import { useAuthStore } from "@/lib/store/auth-store";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const { accessToken, user, _hydrated } = useAuthStore();

  useEffect(() => {
    if (!_hydrated) return;
    if (!accessToken) {
      router.replace("/admin-login");
      return;
    }
    if (user?.role && user.role !== "ADMIN" && user.role !== "SUPER_ADMIN") {
      router.replace("/admin-login");
    }
  }, [accessToken, user, router, _hydrated]);

  return (
    <div className="min-h-screen" style={{ background: "#FAF7F5" }}>
      <AdminSidebar />
      <main className="min-h-screen p-4 md:ml-[248px] md:p-8">{children}</main>
    </div>
  );
}
