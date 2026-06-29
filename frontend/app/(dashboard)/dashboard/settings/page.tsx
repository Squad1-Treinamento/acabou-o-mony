"use client";

import { useState } from "react";
import { Eye, EyeOff, Copy, Check, RotateCcw, Save } from "lucide-react";

const inputCls =
  "h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-900 " +
  "placeholder:text-slate-300 focus-visible:border-[#0D2B1E] focus-visible:ring-2 " +
  "focus-visible:ring-[#0D2B1E]/10 focus-visible:outline-none transition-all";

function Toggle({
  checked,
  onChange,
  label,
  description,
}: {
  checked: boolean;
  onChange: (v: boolean) => void;
  label: string;
  description?: string;
}) {
  return (
    <label className="flex items-start gap-3 cursor-pointer select-none">
      <button
        role="switch"
        aria-checked={checked}
        onClick={() => onChange(!checked)}
        className={`relative mt-0.5 w-9 h-5 rounded-full transition-colors shrink-0 ${
          checked ? "bg-[#0D2B1E]" : "bg-slate-200"
        }`}
      >
        <span
          className={`absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow-sm transition-transform ${
            checked ? "translate-x-4" : "translate-x-0"
          }`}
        />
      </button>
      <div>
        <p className="text-sm text-slate-700">{label}</p>
        {description && <p className="text-xs text-slate-400 mt-0.5">{description}</p>}
      </div>
    </label>
  );
}

function SectionCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 px-6 py-5">
      <h2 className="text-sm font-semibold text-slate-900 mb-4">{title}</h2>
      {children}
    </div>
  );
}

export default function SettingsPage() {
  const [apiKey] = useState<string>(() => {
    if (typeof window === "undefined") return "";
    return sessionStorage.getItem("mony_api_key") ?? "";
  });
  const [showKey,  setShowKey]  = useState(false);
  const [copied,   setCopied]   = useState(false);

  const [webhookUrl, setWebhookUrl] = useState<string>(() => {
    if (typeof window === "undefined") return "";
    return localStorage.getItem("mony_webhook_url") ?? "";
  });
  const [urlError, setUrlError] = useState<string | null>(null);
  const [urlSaved, setUrlSaved] = useState(false);

  const [notifyDeclined, setNotifyDeclined] = useState<boolean>(() => {
    if (typeof window === "undefined") return false;
    return localStorage.getItem("mony_notify_declined") === "true";
  });
  const [notifyFailed, setNotifyFailed] = useState<boolean>(() => {
    if (typeof window === "undefined") return false;
    return localStorage.getItem("mony_notify_failed") === "true";
  });

  function copyKey() {
    navigator.clipboard.writeText(apiKey);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  function saveWebhook() {
    if (!webhookUrl.match(/^https?:\/\/.+/)) {
      setUrlError("URL inválida. Use https:// ou http://localhost");
      return;
    }
    setUrlError(null);
    localStorage.setItem("mony_webhook_url", webhookUrl);
    setUrlSaved(true);
    setTimeout(() => setUrlSaved(false), 2000);
  }

  function handleNotifyDeclined(v: boolean) {
    setNotifyDeclined(v);
    localStorage.setItem("mony_notify_declined", String(v));
  }

  function handleNotifyFailed(v: boolean) {
    setNotifyFailed(v);
    localStorage.setItem("mony_notify_failed", String(v));
  }

  return (
    <div className="max-w-2xl mx-auto w-full px-6 py-8 space-y-5">

      <div>
        <h1 className="text-lg font-semibold text-slate-900">Configurações</h1>
        <p className="text-xs text-slate-400 mt-0.5">
          Gerencie sua integração e preferências
        </p>
      </div>

      {/* Section 1 — Autenticação */}
      <SectionCard title="Autenticação">
        <div className="space-y-3">
          <label className="text-xs font-medium text-slate-500 block">
            Chave da empresa
          </label>
          <div className="flex gap-2">
            <div className="relative flex-1">
              <input
                type={showKey ? "text" : "password"}
                value={apiKey}
                readOnly
                className={`${inputCls} pr-10 font-mono`}
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
            <button
              type="button"
              onClick={copyKey}
              title="Copiar chave"
              className="h-10 px-3 rounded-lg border border-slate-200 text-slate-500 hover:text-slate-800 hover:border-slate-300 transition-all flex items-center gap-1.5 text-xs"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Copy className="w-3.5 h-3.5" />}
              {copied ? "Copiado" : "Copiar"}
            </button>
          </div>

          <div>
            <button
              disabled
              title="Em breve — não disponível nesta versão"
              className="flex items-center gap-1.5 text-xs text-slate-300 cursor-not-allowed"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              Rotacionar chave
            </button>
            <p className="text-[11px] text-slate-300 mt-1">
              Rotação de chave não disponível nesta versão.
            </p>
          </div>
        </div>
      </SectionCard>

      {/* Section 2 — Integração */}
      <SectionCard title="Integração">
        <div className="space-y-3">
          <label className="text-xs font-medium text-slate-500 block">
            Endpoint de webhook
          </label>
          <div className="flex gap-2">
            <input
              type="url"
              value={webhookUrl}
              onChange={(e) => { setWebhookUrl(e.target.value); setUrlError(null); }}
              placeholder="https://seu-servidor.com/webhooks"
              className={inputCls}
            />
            <button
              type="button"
              onClick={saveWebhook}
              className="h-10 px-4 rounded-lg text-xs font-medium text-white transition-opacity hover:opacity-85 flex items-center gap-1.5 shrink-0"
              style={{ background: "#0D2B1E" }}
            >
              <Save className="w-3.5 h-3.5" />
              {urlSaved ? "Salvo!" : "Salvar"}
            </button>
          </div>
          {urlError && (
            <p className="text-xs text-red-500">{urlError}</p>
          )}
          {urlSaved && !urlError && (
            <p className="text-xs text-emerald-600">Endpoint salvo com sucesso.</p>
          )}
          <p className="text-[11px] text-slate-400 leading-relaxed">
            Os eventos de pagamento serão enviados para este endpoint via POST. Aceita{" "}
            <code className="font-mono">https://</code> ou{" "}
            <code className="font-mono">http://localhost</code> para testes locais.
          </p>
        </div>
      </SectionCard>

      {/* Section 3 — Notificações */}
      <SectionCard title="Notificações">
        <div className="space-y-4">
          <Toggle
            checked={notifyDeclined}
            onChange={handleNotifyDeclined}
            label="Alertar por email quando uma transação for recusada"
            description="Você receberá um email para cada DECLINED."
          />
          <Toggle
            checked={notifyFailed}
            onChange={handleNotifyFailed}
            label="Alertar por email quando houver falha técnica"
            description="Você receberá um email para cada FAILED."
          />
          <p className="text-[11px] text-slate-300 pt-1 border-t border-slate-100">
            Configurações de notificação são salvas localmente neste dispositivo.
          </p>
        </div>
      </SectionCard>

    </div>
  );
}
