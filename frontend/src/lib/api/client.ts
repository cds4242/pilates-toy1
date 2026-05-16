"use client";

import axios, { AxiosError } from "axios";
import type { ApiResponse } from "@/lib/types/api";
import { demoMockAdapter } from "./demo-mock";

// NAS 박제 한정 플래그. 운영 빌드(Railway 등)에서는 NEXT_PUBLIC_DEMO_MODE 미설정 → IS_DEMO=false → mock adapter 비활성
const IS_DEMO = process.env.NEXT_PUBLIC_DEMO_MODE === "true";

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
  timeout: 15000,
  ...(IS_DEMO ? { adapter: demoMockAdapter } : {}),
});

// 요청 인터셉터: Authorization 헤더
apiClient.interceptors.request.use((config) => {
  // 로그인/인증 API는 토큰 불필요
  const isAuthUrl = config.url?.includes("/auth/login") || config.url?.includes("/auth/signup") || config.url?.includes("/auth/sms");
  if (typeof window !== "undefined" && !isAuthUrl) {
    const stored = localStorage.getItem("auth-storage");
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        const token = parsed?.state?.accessToken;
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
      } catch {
        // ignore
      }
    }
  }
  return config;
});

// 응답 인터셉터: 401 → 토큰 갱신 시도
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config;
    const isAuthUrl = original?.url?.includes("/auth/login") || original?.url?.includes("/auth/signup") || original?.url?.includes("/auth/sms");
    if (
      error.response?.status === 401 &&
      original &&
      !isAuthUrl &&
      !(original as unknown as Record<string, unknown>)._retry
    ) {
      (original as unknown as Record<string, unknown>)._retry = true;
      try {
        const stored = localStorage.getItem("auth-storage");
        if (stored) {
          const parsed = JSON.parse(stored);
          const refreshToken = parsed?.state?.refreshToken;
          if (refreshToken) {
            // 회원 또는 관리자 refresh 시도
            const role = parsed?.state?.user?.role;
            const endpoint =
              role === "MEMBER"
                ? "/api/auth/refresh"
                : "/api/admin/auth/refresh";
            const res = await axios.post(
              `${apiClient.defaults.baseURL}${endpoint}`,
              { refreshToken }
            );
            const newToken = res.data?.data?.accessToken;
            if (newToken) {
              parsed.state.accessToken = newToken;
              localStorage.setItem("auth-storage", JSON.stringify(parsed));
              original.headers.Authorization = `Bearer ${newToken}`;
              return apiClient(original);
            }
          }
        }
      } catch {
        // refresh 실패 → 로그아웃
      }
      // role에 따라 적절한 로그인 페이지로 리디렉트
      try {
        const stored2 = localStorage.getItem("auth-storage");
        const role2 = stored2 ? JSON.parse(stored2)?.state?.user?.role : null;
        localStorage.removeItem("auth-storage");
        if (role2 === "INSTRUCTOR") window.location.href = "/instructor-login";
        else if (role2 === "ADMIN" || role2 === "SUPER_ADMIN") window.location.href = "/admin-login";
        else window.location.href = "/login";
      } catch {
        localStorage.removeItem("auth-storage");
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

/** ApiResponse<T> 언래핑: data만 반환 */
export async function api<T>(
  method: "get" | "post" | "patch" | "delete",
  url: string,
  data?: unknown,
  config?: Record<string, unknown>
): Promise<T> {
  try {
    const res = await apiClient.request<ApiResponse<T>>({
      method,
      url,
      data: method !== "get" ? data : undefined,
      params: method === "get" ? data : undefined,
      ...config,
    });
    if (!res.data.success) {
      throw new Error(res.data.error?.message || "API 오류");
    }
    return res.data.data;
  } catch (err: unknown) {
    // axios 에러에서 한글 메시지 추출
    if (err && typeof err === "object" && "response" in err) {
      const axiosErr = err as { response?: { data?: { error?: { message?: string } } } };
      const msg = axiosErr.response?.data?.error?.message;
      if (msg) throw new Error(msg);
    }
    throw err;
  }
}

export default apiClient;
