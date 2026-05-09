import { api } from "./client";

export const reservationApi = {
  create: (classScheduleId: number) =>
    api<{ id: number }>("post", "/api/reservations", { classScheduleId }),

  cancel: (id: number, reason: string) =>
    api<void>("post", `/api/reservations/${id}/cancel`, { reason }),
};
