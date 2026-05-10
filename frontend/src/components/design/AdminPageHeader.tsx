"use client";

import { ReactNode } from "react";

interface AdminPageHeaderProps {
  eyebrow?: string;
  title: string;
  sub?: string;
  actions?: ReactNode;
}

export function AdminPageHeader({
  eyebrow,
  title,
  sub,
  actions,
}: AdminPageHeaderProps) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        {eyebrow && (
          <div className="mb-1.5 text-[11px] font-semibold uppercase tracking-[0.1em] text-[#D88A9E]">
            {eyebrow}
          </div>
        )}
        <h1 className="text-[28px] font-bold leading-[1.2] tracking-[-0.03em] text-[#2A2A2C]">
          {title}
        </h1>
        {sub && <p className="mt-1.5 text-[14px] text-[#6B6B6B]">{sub}</p>}
      </div>
      {actions && (
        <div className="flex w-full items-center gap-2.5 md:w-auto">{actions}</div>
      )}
    </div>
  );
}

interface AdminSearchBoxProps {
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  className?: string;
}

export function AdminSearchBox({
  value,
  onChange,
  placeholder = "검색...",
  className = "",
}: AdminSearchBoxProps) {
  return (
    <div
      className={`flex flex-1 items-center gap-2 rounded-full border border-[#F0EBE8] bg-white px-4 py-2.5 transition-colors focus-within:border-[#FAD4DE] md:w-60 md:flex-none ${className}`}
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        strokeWidth={2}
        stroke="currentColor"
        className="h-4 w-4 flex-shrink-0 text-[#A0A0A0]"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="m21 21-5.197-5.197m0 0A7.5 7.5 0 1 0 5.196 5.196a7.5 7.5 0 0 0 10.607 10.607Z"
        />
      </svg>
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="flex-1 bg-transparent text-[13px] text-[#2A2A2C] outline-none placeholder:text-[#A0A0A0]"
      />
    </div>
  );
}

interface AdminPrimaryButtonProps {
  onClick?: () => void;
  type?: "button" | "submit";
  disabled?: boolean;
  children: ReactNode;
  icon?: ReactNode;
}

export function AdminPrimaryButton({
  onClick,
  type = "button",
  disabled,
  children,
  icon,
}: AdminPrimaryButtonProps) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className="inline-flex items-center gap-1.5 rounded-full bg-[#F0A0B5] px-4 py-2.5 text-[13px] font-semibold tracking-[-0.01em] text-white transition-colors hover:bg-[#D88A9E] disabled:opacity-60"
    >
      {icon}
      {children}
    </button>
  );
}

export function AdminGhostButton({
  onClick,
  children,
}: {
  onClick?: () => void;
  children: ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className="inline-flex items-center gap-1.5 rounded-full border border-[#F0EBE8] bg-white px-4 py-2.5 text-[13px] font-semibold tracking-[-0.01em] text-[#6B6B6B] transition-colors hover:border-[#FAD4DE] hover:text-[#D88A9E]"
    >
      {children}
    </button>
  );
}

export function AdminPlusIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={2.4}
      stroke="currentColor"
      className="h-3.5 w-3.5"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M12 4.5v15m7.5-7.5h-15"
      />
    </svg>
  );
}
