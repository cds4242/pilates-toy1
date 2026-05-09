import { api } from "./client";
import type {
  LoginResponse,
  AdminLoginResponse,
  SignupResponse,
} from "@/lib/types/domain";

export const authApi = {
  /** SMS 인증 요청 */
  requestSms: (phoneNumber: string) =>
    api<void>("post", "/api/auth/sms/request", { phoneNumber }),

  /** SMS 인증 확인 */
  verifySms: (phoneNumber: string, code: string) =>
    api<{ verifiedToken: string }>("post", "/api/auth/sms/verify", {
      phoneNumber,
      code,
    }),

  /** 회원가입 */
  signup: (data: {
    verifiedToken: string;
    name: string;
    password: string;
    gender: string;
  }) => api<SignupResponse>("post", "/api/auth/signup", data),

  /** 회원 로그인 */
  login: (phoneNumber: string, password: string) =>
    api<LoginResponse>("post", "/api/auth/login", { phoneNumber, password }),

  /** 관리자 로그인 */
  adminLogin: (loginId: string, password: string) =>
    api<AdminLoginResponse>("post", "/api/admin/auth/login", {
      loginId,
      password,
    }),
};
