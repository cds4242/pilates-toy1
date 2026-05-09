import { api } from "./client";
import type { Member, Membership, Reservation } from "@/lib/types/domain";

export const memberApi = {
  getMe: () => api<Member>("get", "/api/members/me"),

  getMemberships: () =>
    api<Membership[]>("get", "/api/members/me/memberships"),

  getReservations: () =>
    api<Reservation[]>("get", "/api/members/me/reservations"),
};
