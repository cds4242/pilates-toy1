import Link from "next/link";

export default function NotFound() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gradient-to-b from-white to-[#FFF5F7] p-6">
      <div className="text-center">
        <p className="text-[80px] font-bold text-[var(--color-pilates)]">404</p>
        <h1 className="text-[24px] font-bold text-[var(--color-text-title)] mt-2 mb-3">페이지를 찾을 수 없어요</h1>
        <p className="text-[15px] text-[var(--color-text-body)] mb-8">요청하신 페이지가 존재하지 않거나 이동되었을 수 있습니다.</p>
        <div className="flex gap-3 justify-center">
          <Link href="/" className="btn-primary rounded-[8px] px-6 py-3 text-[15px]">홈으로 돌아가기</Link>
        </div>
      </div>
    </div>
  );
}
