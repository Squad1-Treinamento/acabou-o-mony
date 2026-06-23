"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getPayment } from "@/lib/api/payments";

export function ApiKeyForm() {
  const router = useRouter();
  const [apiKey, setApiKey] = useState("");
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
      if (status === 404) {
        router.push("/dashboard");
      } else if (status === 401) {
        sessionStorage.removeItem("mony_api_key");
        setError("Chave de API inválida");
        setIsLoading(false);
      } else {
        router.push("/dashboard");
      }
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-5">
      <div className="space-y-2">
        <Label htmlFor="api-key">Chave de API</Label>
        <Input
          id="api-key"
          type="password"
          placeholder="sk_..."
          value={apiKey}
          onChange={(e) => setApiKey(e.target.value)}
          autoComplete="off"
          className="h-[var(--height-input)] rounded-input border-input-border px-4 text-base"
        />
        {error && <p className="text-xs text-error">{error}</p>}
      </div>

      <Button
        type="submit"
        className="w-full bg-primary hover:bg-primary/90 text-white rounded-btn"
        style={{ height: "var(--height-btn)" }}
        disabled={!apiKey.trim() || isLoading}
      >
        {isLoading ? "Verificando..." : "Entrar"}
      </Button>
    </form>
  );
}
