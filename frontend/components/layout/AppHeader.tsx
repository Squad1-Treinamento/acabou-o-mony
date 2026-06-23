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
    <header className="bg-primary text-white">
      <div className="px-6 h-14 flex items-center justify-between">
        <Link href="/" className="font-semibold text-base tracking-tight hover:text-white/80 transition-colors">
          Acabou o Mony
        </Link>

        {showLogout && (
          <button
            onClick={handleLogout}
            className="flex items-center gap-1.5 text-white/70 hover:text-white transition-colors text-sm"
          >
            <LogOut className="w-4 h-4" />
            Sair
          </button>
        )}
      </div>
    </header>
  );
}
