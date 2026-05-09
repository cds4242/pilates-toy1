import { api } from "./client";
import apiClient from "./client";
import type { DashboardData } from "@/lib/types/domain";
import type { PageResponse } from "@/lib/types/api";

export const adminApi = {
  getDashboard: () => api<DashboardData>("get", "/api/admin/dashboard"),

  getMembers: (params: { search?: string; page?: number; size?: number }) =>
    api<PageResponse<Record<string, unknown>>>("get", "/api/admin/members", params),

  createClassSchedule: (data: {
    instructorId: number;
    lessonTypeId: number;
    classDate: string;
    startTime: string;
    endTime: string;
    maxCapacity: number;
  }) => api<Record<string, unknown>>("post", "/api/admin/class-schedules", data),

  getClassSchedules: (from: string, to: string) =>
    api<Record<string, unknown>[]>("get", "/api/admin/class-schedules", { from, to }),

  bulkImportMembers: async (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    const res = await apiClient.post("/api/admin/members/bulk", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return res.data.data;
  },
};
