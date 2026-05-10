"use client";

import { useState } from "react";
import { HelpCircle, X } from "lucide-react";
import Link from "next/link";

/**
 * 도움말 툴팁 — 물음표 아이콘 클릭 시 설명 팝업
 */
export function HelpTip({ text, link, linkText }: { text: string; link?: string; linkText?: string }) {
  const [open, setOpen] = useState(false);

  return (
    <span className="relative inline-flex">
      <button
        onClick={(e) => { e.stopPropagation(); setOpen(!open); }}
        className="text-[var(--color-text-sub)] hover:text-[var(--color-pilates-dark)] transition-colors p-0.5"
        aria-label="도움말"
      >
        <HelpCircle className="h-4 w-4" />
      </button>
      {open && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} />
          <div className="absolute left-1/2 -translate-x-1/2 top-7 z-50 w-[240px] bg-white rounded-[12px] p-3 card-elevated-lg border border-[var(--color-border)]">
            <div className="flex justify-between items-start gap-2 mb-1">
              <p className="text-[12px] text-[var(--color-text-body)] leading-relaxed flex-1">{text}</p>
              <button onClick={() => setOpen(false)} className="shrink-0 text-[var(--color-text-sub)]">
                <X className="h-3 w-3" />
              </button>
            </div>
            {link && (
              <Link
                href={link}
                onClick={() => setOpen(false)}
                className="text-[11px] text-[var(--color-pilates-dark)] font-semibold hover:underline mt-1 inline-block"
              >
                {linkText || "바로가기"} →
              </Link>
            )}
          </div>
        </>
      )}
    </span>
  );
}

/**
 * 페이지 상단 안내 배너 — 닫을 수 있음
 */
export function PageGuide({ text, link, linkText }: { text: string; link?: string; linkText?: string }) {
  const [dismissed, setDismissed] = useState(false);
  if (dismissed) return null;

  return (
    <div className="rounded-[12px] bg-[#FFF8FA] border border-[#F5E0E6] px-4 py-3 mb-4 flex items-start gap-3 animate-fade-in">
      <HelpCircle className="h-4 w-4 text-[var(--color-pilates-dark)] shrink-0 mt-0.5" />
      <div className="flex-1">
        <p className="text-[13px] text-[var(--color-text-body)] leading-relaxed">{text}</p>
        {link && (
          <Link href={link} className="text-[12px] text-[var(--color-pilates-dark)] font-semibold hover:underline mt-1 inline-block">
            {linkText || "바로가기"} →
          </Link>
        )}
      </div>
      <button onClick={() => setDismissed(true)} className="text-[var(--color-text-sub)] hover:text-[var(--color-text-body)] shrink-0">
        <X className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}
