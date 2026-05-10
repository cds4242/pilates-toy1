import { api } from "./client";

export const reservationApi = {
  create: (classScheduleId: number) =>
    api<{ id: number }>("post", "/api/reservations", { classScheduleId }),

  cancel: (id: number, reason: string) =>
    api<void>("delete", `/api/reservations/${id}`, undefined, { params: { reason } }),
};
