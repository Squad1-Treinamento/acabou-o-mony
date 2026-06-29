"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Eye, EyeOff } from "lucide-react";
import { getPayment } from "@/lib/api/payments";

const inputCls =
  "h-12 w-full rounded-lg border border-slate-200 bg-white px-4 text-sm text-slate-900 " +
  "placeholder:text-slate-300 focus-visible:border-[#0D2B1E] focus-visible:ring-2 " +
  "focus-visible:ring-[#0D2B1E]/10 focus-visible:outline-none transition-all";

export function ApiKeyForm() {
  const router = useRouter();
  const [apiKey, setApiKey] = useState("");
  const [showKey, setShowKey] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!apiKey.trim()) return;

    setIsLoading(true);
    setError(null);

    sessionStorage.setItem("mony_api_key", apiKey.trim());

    try {
      await getPayment("00000000-0000-0000-0000-000000000000");
      router.push("/dashboard");
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 401) {
        // Explicit rejection from the backend — key is wrong
        sessionStorage.removeItem("mony_api_key");
        setError("Chave de API inválida. Verifique e tente novamente.");
        setIsLoading(false);
      } else {
        // 404 (payment not found = key is valid), network error, or any other status —
        // let the user through; the dashboard will surface real errors on data load
        router.push("/dashboard");
      }
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-1.5">
        <label htmlFor="api-key" className="text-xs font-medium text-slate-500 block">
          Chave da empresa
        </label>
        <div className="relative">
          <input
            id="api-key"
            type={showKey ? "text" : "password"}
            placeholder="••••••••••••"
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
            autoComplete="off"
            className={`${inputCls} pr-10`}
          />
          <button
            type="button"
            onClick={() => setShowKey((v) => !v)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors"
            aria-label={showKey ? "Ocultar chave" : "Mostrar chave"}
          >
            {showKey ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
          </button>
        </div>
        {error && (
          <p className="text-xs text-red-500 mt-1">{error}</p>
        )}
      </div>

      <button
        type="submit"
        disabled={!apiKey.trim() || isLoading}
        className="w-full h-12 rounded-xl font-semibold text-sm text-white transition-opacity disabled:opacity-40"
        style={{ background: "#0D2B1E" }}
      >
        {isLoading ? "Verificando..." : "Acessar painel"}
      </button>
    </form>
  );
}
