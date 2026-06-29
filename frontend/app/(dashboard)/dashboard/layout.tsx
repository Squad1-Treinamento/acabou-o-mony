"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { DashboardSidebar } from "@/components/dashboard/DashboardSidebar";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();

  useEffect(() => {
    if (!sessionStorage.getItem("mony_api_key")) {
      router.replace("/login");
    }
  }, [router]);

  return (
    <div className="flex min-h-screen bg-[#F8F9FB]">
      {/* Desktop sidebar */}
      <div className="hidden md:block">
        <DashboardSidebar />
      </div>

      {/* Mobile top bar */}
      <header className="md:hidden fixed top-0 inset-x-0 z-30 bg-white border-b border-slate-100 h-14 flex items-center justify-between px-5">
        <Link href="/" className="hover:opacity-75 transition-opacity">
          <Image
            src="/vibe-store.png"
            alt="Vibe Store"
            width={400}
            height={533}
            className="h-8 w-auto"
            unoptimized
          />
        </Link>
        <nav className="flex items-center gap-4">
          <Link href="/dashboard" className="text-xs text-slate-500 hover:text-slate-800">
            Transações
          </Link>
          <Link href="/dashboard/analytics" className="text-xs text-slate-500 hover:text-slate-800">
            Relatórios
          </Link>
          <Link href="/dashboard/settings" className="text-xs text-slate-500 hover:text-slate-800">
            Config
          </Link>
        </nav>
      </header>

      <main className="flex-1 overflow-auto pt-14 md:pt-0">
        {children}
      </main>
    </div>
  );
}
