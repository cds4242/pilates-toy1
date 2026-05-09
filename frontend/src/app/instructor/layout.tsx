"use client";

import { Header } from "@/components/layout/Header";

export default function InstructorLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col min-h-screen">
      <Header />
      <main className="flex-1 p-4 md:p-6">{children}</main>
    </div>
  );
}
