"use client";

import axios, { AxiosError } from "axios";
import type { ApiResponse } from "@/lib/types/api";

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
  timeout: 15000,
});

// 요청 인터셉터: Authorization 헤더
apiClient.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
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
    if (
      error.response?.status === 401 &&
      original &&
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
      localStorage.removeItem("auth-storage");
      window.location.href = "/login";
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
}

export default apiClient;
