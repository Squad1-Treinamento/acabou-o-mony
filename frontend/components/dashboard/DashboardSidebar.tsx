"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname, useRouter } from "next/navigation";
import {
  LayoutDashboard,
  TrendingUp,
  RefreshCw,
  Webhook,
  Settings,
  LogOut,
  type LucideIcon,
} from "lucide-react";

const NAV: { href: string; icon: LucideIcon; label: string; exact?: boolean }[] = [
  { href: "/dashboard",                icon: LayoutDashboard, label: "Transações",    exact: true },
  { href: "/dashboard/analytics",      icon: TrendingUp,       label: "Relatórios"              },
  { href: "/dashboard/reconciliation", icon: RefreshCw,        label: "Reconciliação"           },
  { href: "/dashboard/webhooks",       icon: Webhook,          label: "Integrações"             },
  { href: "/dashboard/settings",       icon: Settings,         label: "Configurações"           },
];

export function DashboardSidebar() {
  const pathname = usePathname();
  const router = useRouter();

  function logout() {
    sessionStorage.removeItem("mony_api_key");
    router.push("/login");
  }

  function isActive(href: string, exact?: boolean) {
    if (exact) return pathname === href;
    return pathname === href || pathname.startsWith(href + "/");
  }

  return (
    <aside className="w-[220px] shrink-0 bg-white border-r border-slate-100 flex flex-col h-screen sticky top-0">
      <div className="flex items-center justify-center px-5 py-4 border-b border-slate-100 shrink-0">
        <Link href="/" className="hover:opacity-75 transition-opacity">
          <Image
            src="/vibe-store.png"
            alt="Vibe Store"
            width={400}
            height={533}
            className="w-[88px] h-auto"
            unoptimized
          />
        </Link>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-3 space-y-0.5">
        {NAV.map(({ href, icon: Icon, label, exact }) => {
          const active = isActive(href, exact);
          return (
            <Link
              key={href}
              href={href}
              className={`flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm transition-all ${
                active
                  ? "bg-[#0D2B1E]/[0.07] text-[#0D2B1E] font-medium"
                  : "text-slate-500 hover:text-slate-800 hover:bg-slate-50"
              }`}
            >
              <Icon
                className={`w-4 h-4 shrink-0 ${active ? "text-[#0D2B1E]" : "text-slate-400"}`}
              />
              {label}
            </Link>
          );
        })}
      </nav>

      <div className="px-3 py-3 border-t border-slate-100 shrink-0 space-y-0.5">
        {/* User profile */}
        <div className="flex items-center gap-2.5 px-3 py-2 mb-0.5">
          <div className="min-w-0">
            <p className="text-sm font-medium text-slate-800 truncate leading-tight">Vibe Store</p>
            <p className="text-[11px] text-slate-400 leading-tight">Comerciante</p>
          </div>
        </div>

        <button
          onClick={logout}
          className="w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-slate-500 hover:text-slate-800 hover:bg-slate-50 transition-all"
        >
          <LogOut className="w-4 h-4 shrink-0 text-slate-400" />
          Sair
        </button>
      </div>
    </aside>
  );
}
