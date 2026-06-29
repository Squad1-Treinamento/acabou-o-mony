---
id: task-021
status: planned
links:
  - spec/specs/spec-007-merchant-dashboard.md
  - spec/tasks/front-end-paty/task-017-dashboard-sidebar-layout.md
---

# Settings Page

Criar a página `/dashboard/settings` com três seções: gerenciamento de chave de API, configuração de endpoint de webhook e preferências de notificação.

## Local Context

- **Branch:** `front-end-paty`
- **Spec:** spec-007 §Settings Page
- **File to create:** `frontend/app/(dashboard)/dashboard/settings/page.tsx`

## Sections

### Section 1 — Autenticação

Read API key from `sessionStorage.getItem("mony_api_key")` on mount.

```tsx
const [showKey, setShowKey] = useState(false);
const [apiKey] = useState(() =>
  typeof window !== "undefined" ? sessionStorage.getItem("mony_api_key") ?? "" : ""
);
```

Layout:
```
Chave da empresa
[••••••••••••      ] [👁] [📋 Copiar]
[Rotacionar chave] → disabled button, tooltip "Em breve — não disponível nesta versão"
```

Copy button: uses the CopyButton component or inline copy logic.
Eye toggle: same Eye/EyeOff pattern as ApiKeyForm.
Rotate button: `disabled` with `title="Em breve"` and reduced opacity.

### Section 2 — Integração

Webhook endpoint URL input. Stored in `localStorage` under `mony_webhook_url`.

```tsx
const [webhookUrl, setWebhookUrl] = useState(() =>
  typeof window !== "undefined" ? localStorage.getItem("mony_webhook_url") ?? "" : ""
);
const [saved, setSaved] = useState(false);

function saveWebhookUrl() {
  if (!webhookUrl.match(/^https?:\/\/.+/)) {
    setUrlError("URL inválida. Use https:// ou http://localhost");
    return;
  }
  localStorage.setItem("mony_webhook_url", webhookUrl);
  setSaved(true);
  setTimeout(() => setSaved(false), 2000);
}
```

Layout:
```
Endpoint de webhook
[https://seu-servidor.com/webhooks    ] [Salvar]
Inline error: "URL inválida. Use https:// ou http://localhost"
Success toast: "Salvo!" (inline green text, 2s)
```

### Section 3 — Notificações

Two toggle switches. Stored in `localStorage`.

```tsx
const [notifyDeclined, setNotifyDeclined] = useState(() =>
  localStorage.getItem("mony_notify_declined") === "true"
);
const [notifyFailed, setNotifyFailed] = useState(() =>
  localStorage.getItem("mony_notify_failed") === "true"
);
```

Toggle implementation: styled `<button role="switch">` with `aria-checked`.
On change: immediately write to `localStorage`.

Layout (each toggle row):
```
[●○] Receber alerta ao email quando uma transação for recusada
[●○] Receber alerta ao email quando houver falha técnica
```

Footer note: "Configurações de notificação são salvas localmente neste dispositivo."

## Toggle Component (inline)

```tsx
function Toggle({ checked, onChange, label }: { checked: boolean; onChange: (v: boolean) => void; label: string }) {
  return (
    <label className="flex items-center gap-3 cursor-pointer select-none">
      <button
        role="switch"
        aria-checked={checked}
        onClick={() => onChange(!checked)}
        className={`relative w-9 h-5 rounded-full transition-colors ${checked ? "bg-[#0D2B1E]" : "bg-slate-200"}`}
      >
        <span className={`absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white shadow-sm transition-transform ${checked ? "translate-x-4" : "translate-x-0"}`} />
      </button>
      <span className="text-sm text-slate-700">{label}</span>
    </label>
  );
}
```

## Page Layout

```
Page header: "Configurações"
Section card 1: "Autenticação" — API key management
Section card 2: "Integração" — webhook URL
Section card 3: "Notificações" — email alerts
```

Section cards: `bg-white rounded-xl border border-slate-200 px-6 py-5 space-y-4`
Section heading: `text-sm font-semibold text-slate-900 mb-4`

## Acceptance Criteria and Tests

- Success: API key read from `sessionStorage`, shown masked by default
- Success: Eye toggle reveals/hides key
- Success: Copy button copies key to clipboard
- Success: Rotate button is disabled and shows "Em breve" tooltip
- Success: Valid URL saves to localStorage; "Salvo!" shown for 2s
- Success: Invalid URL shows inline error, does not save
- Success: Toggles persist to localStorage immediately on change
- Success: On page reload, all localStorage values are restored
- Failure: API key written to `localStorage` at any point

## Constraints

- API key must only be read from `sessionStorage`, NEVER written
- `localStorage` keys: `mony_webhook_url`, `mony_notify_declined`, `mony_notify_failed`
- Do NOT add new npm dependencies
- Rotation is UI-only disabled state — no API call
