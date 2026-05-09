import { api } from "./client";
import type { DashboardData } from "@/lib/types/domain";

export const adminApi = {
  getDashboard: () => api<DashboardData>("get", "/api/admin/dashboard"),
};
