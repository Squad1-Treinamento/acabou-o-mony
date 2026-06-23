"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, LogOut } from "lucide-react";

interface AppHeaderProps {
  showLogout?: boolean;
  backHref?: string;
  backLabel?: string;
}

export function AppHeader({ showLogout, backHref, backLabel }: AppHeaderProps) {
  const router = useRouter();

  function handleLogout() {
    sessionStorage.removeItem("mony_api_key");
    router.push("/login");
  }

  return (
    <header className="bg-primary text-white">
      <div className="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">
        <div className="flex items-center gap-4">
          {backHref && (
            <Link
              href={backHref}
              className="flex items-center gap-1.5 text-white/70 hover:text-white transition-colors text-sm"
            >
              <ArrowLeft className="w-4 h-4" />
              {backLabel ?? "Voltar"}
            </Link>
          )}
          <Link href="/" className="font-semibold text-base tracking-tight">
            Acabou o Mony
          </Link>
        </div>

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
