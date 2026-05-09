interface SectionTitleProps {
  children: React.ReactNode;
}

export function SectionTitle({ children }: SectionTitleProps) {
  return (
    <h2 className="text-[20px] font-bold text-[var(--color-text-title)] pb-2 border-b-2 border-[var(--color-pilates)] mb-4">
      {children}
    </h2>
  );
}
