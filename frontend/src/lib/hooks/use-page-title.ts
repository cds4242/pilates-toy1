"use client";

import { useEffect } from "react";

export function usePageTitle(title: string) {
  useEffect(() => {
    document.title = `${title} | 필라테스 OO점`;
    return () => { document.title = "필라테스 OO점"; };
  }, [title]);
}
