import { api } from "./client";
import type { ClassSchedule } from "@/lib/types/domain";

export const classroomApi = {
  getSchedules: (from: string, to: string) =>
    api<ClassSchedule[]>("get", "/api/class-schedules", { from, to }),
};
