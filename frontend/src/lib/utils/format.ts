import { format, parseISO } from "date-fns";
import { ko } from "date-fns/locale";

export function formatDate(dateStr: string, fmt = "yyyy.MM.dd") {
  try {
    return format(parseISO(dateStr), fmt, { locale: ko });
  } catch {
    return dateStr;
  }
}

export function formatPhone(phone: string) {
  const cleaned = phone.replace(/\D/g, "");
  if (cleaned.length === 11) {
    return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 7)}-${cleaned.slice(7)}`;
  }
  return phone;
}

export function formatTime(time: string) {
  // "10:00:00" → "10:00"
  return time?.slice(0, 5) || time;
}
