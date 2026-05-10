"use client";

import { useRouter } from "next/navigation";
import { useAuthStore } from "@/lib/store/auth-store";

export function useAuth() {
  const router = useRouter();
  const store = useAuthStore();

  const redirectByRole = (role: string) => {
    switch (role) {
      case "MEMBER":
        router.push("/home");
        break;
      case "INSTRUCTOR":
        router.push("/instructor/schedule");
        break;
      case "ADMIN":
      case "SUPER_ADMIN":
        router.push("/dashboard");
        break;
      default:
        router.push("/login");
    }
  };

  const logout = () => {
    const role = store.user?.role;
    store.logout();
    if (role === "INSTRUCTOR") {
      router.push("/instructor-login");
    } else if (role === "ADMIN" || role === "SUPER_ADMIN") {
      router.push("/admin-login");
    } else {
      router.push("/login");
    }
  };

  return { ...store, redirectByRole, logout };
}
