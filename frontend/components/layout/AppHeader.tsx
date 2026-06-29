"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { LogOut } from "lucide-react";

interface AppHeaderProps {
  showLogout?: boolean;
}

export function AppHeader({ showLogout }: AppHeaderProps) {
  const router = useRouter();

  function handleLogout() {
    sessionStorage.removeItem("mony_api_key");
    router.push("/login");
  }

  return (
    <header className="bg-white border-b border-slate-100">
      <div className="max-w-6xl mx-auto px-6 h-14 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <Link
            href="/"
            className="font-bold text-sm text-[#0D2B1E] hover:opacity-70 transition-opacity"
          >
            Vibe Store
          </Link>
          <span className="text-slate-300">/</span>
          <span className="text-sm text-slate-500 font-medium">Dashboard</span>
        </div>

        {showLogout && (
          <button
            onClick={handleLogout}
            className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-700 transition-colors"
          >
            <LogOut className="w-3.5 h-3.5" />
            Sair
          </button>
        )}
      </div>
    </header>
  );
}
