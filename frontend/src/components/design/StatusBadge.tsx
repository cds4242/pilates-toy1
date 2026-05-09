interface StatusBadgeProps {
  status: "active" | "expired" | "expiring" | "present" | "late" | "absent" | "noshow";
  label: string;
}

const styles: Record<string, string> = {
  active: "bg-[#E8F5E9] text-[#4CAF50]",
  present: "bg-[#E8F5E9] text-[#4CAF50]",
  expired: "bg-[#FDECEA] text-[#E76F51]",
  absent: "bg-[#FDECEA] text-[#E76F51]",
  noshow: "bg-[#FDECEA] text-[#E76F51]",
  expiring: "bg-[#FFF3E0] text-[#F4A261]",
  late: "bg-[#FFF3E0] text-[#F4A261]",
};

export function StatusBadge({ status, label }: StatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-[12px] px-2 py-0.5 text-[13px] font-semibold ${styles[status] || styles.active}`}
    >
      {label}
    </span>
  );
}
