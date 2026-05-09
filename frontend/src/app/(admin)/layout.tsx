"use client";

import { AdminSidebar } from "@/components/layout/AdminSidebar";

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-[var(--color-bg-section)]">
      <AdminSidebar />
      <main className="md:ml-[240px] p-6">{children}</main>
    </div>
  );
}
