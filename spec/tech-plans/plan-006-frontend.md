---
id: plan-006
status: active
links:
  - spec/tech-plans/index.md
  - spec/specs/spec-006-frontend-checkout.md
  - spec/specs/spec-007-merchant-dashboard.md
  - spec/adrs/adr-004-frontend-architecture.md
  - spec/tasks/index.md
---
# Frontend: Checkout + Merchant Dashboard

Plano técnico para construção do frontend da plataforma Acabou o Mony a partir do zero na branch `front-end-paty`. Cobre a aplicação Next.js 14 completa: fluxo de checkout do cliente (spec-006) e dashboard de transações do lojista (spec-007).

---

## Architecture Overview and Data Flow

### Componentes

```
Browser
  ↓ HTTPS
Nginx :8080  ←→  core-payment :8082
                        ↕
                  3ds-engine :8081

Next.js App (frontend/ :3000 em dev)
  → rewrites /api/* → NEXT_PUBLIC_API_URL (http://localhost:8080)
```

A aplicação Next.js é um cliente puro: não possui banco de dados próprio, não processa pagamentos, e não armazena dados sensíveis. Todo estado persistente vive no backend.

### Fluxo: Checkout

```
1. Cliente acessa /checkout
2. CheckoutShell inicializa: estado = FORM
3. Cliente preenche card_token_id → clica "Pagar"
4. CheckoutShell → estado = PROCESSING
5. POST /api/v1/payments (com Idempotency-Key gerado e salvo em sessionStorage)
6a. HTTP 200 COMPLETED → estado = SUCCESS
6b. HTTP 200 DECLINED/FAILED → estado = ERROR
6c. HTTP 202 CHALLENGE_PENDING:
    → transaction_id + challenge_id salvos em sessionStorage
    → redirect browser para acs_url
    → ACS autentica → redireciona de volta para /checkout?3ds_complete=true
    → CheckoutShell remonta → lê transaction_id de sessionStorage → estado = PROCESSING
    → polling GET /api/v1/payments/{id} a cada 2s
    → terminal state → SUCCESS ou ERROR
```

### Fluxo: Dashboard

```
1. Lojista acessa /login → insere API key → key salva em sessionStorage
2. Redireciona para /dashboard
3. Lojista insere transaction IDs (um ou mais)
4. useTransactions dispara GET /api/v1/payments/{id} para cada ID (paralelo)
5. Tabela renderiza com sort/filter/paginação (client-side)
6. Auto-refresh a cada 30s se alguma transação em estado não-terminal
7. Click em linha → /dashboard/transactions/{id} → TransactionDetail
8. TransactionDetail polling a cada 10s se estado não-terminal
```

---

## Stack and Dependencies

| Categoria | Tecnologia | Versão mínima |
|---|---|---|
| Runtime | Node.js | 18.x LTS |
| Package manager | pnpm | 9.x |
| Framework | Next.js | 14.x (App Router) |
| Linguagem | TypeScript | 5.x, strict mode |
| Estilização | TailwindCSS | 3.4.x |
| Componentes | shadcn/ui | latest (CLI) |
| Ícones | lucide-react | latest |
| Estado servidor | @tanstack/react-query | 5.x |
| Formulários | react-hook-form | 7.x |
| Validação | zod | 3.x |
| Fonte | next/font (Inter) | nativo Next.js |

**Dependências não permitidas:**
- Redux, Zustand, Jotai (overkill para este escopo)
- Axios (fetch nativo é suficiente)
- CSS Modules ou styled-components (Tailwind é o único sistema de estilos)
- Qualquer biblioteca de tokenização de cartão

---

## Design Patterns and Code Conventions

### Estrutura de pastas

```
frontend/
  app/
    layout.tsx                    ← root layout: Inter font, QueryClientProvider
    globals.css                   ← Tailwind base + CSS vars de design.json
    (checkout)/
      checkout/page.tsx           ← CheckoutShell (client component)
    (dashboard)/
      login/page.tsx              ← ApiKeyForm
      dashboard/page.tsx          ← TransactionLookup + TransactionTable
      dashboard/transactions/[id]/page.tsx  ← TransactionDetail
  components/
    ui/                           ← shadcn/ui primitivos (gerados via CLI)
    checkout/                     ← componentes específicos do checkout
    dashboard/                    ← componentes específicos do dashboard
    shared/                       ← componentes reutilizados por ambos
  lib/
    api/                          ← cliente HTTP e funções de API
    utils/                        ← formatadores, idempotência
  hooks/                          ← hooks de negócio (usePayment, useTransactions)
  types/                          ← interfaces TypeScript do domínio
```

### Convenções

- **Nomenclatura:** PascalCase para componentes, camelCase para hooks e funções, kebab-case para arquivos de página.
- **Client vs Server components:** toda interatividade (formulários, polling, sessionStorage) exige `"use client"` explícito. Páginas de rota que apenas compõem componentes client podem ser server components.
- **Importações:** alias `@/*` aponta para `frontend/` (configurado em `tsconfig.json`).
- **Estilos:** apenas classes Tailwind. Nenhum `style={{}}` inline. Tokens de `design.json` mapeados em `tailwind.config.ts` como extensões do tema.
- **Sem comentários óbvios:** comentar apenas decisões não-óbvias (ex: workaround de comportamento do App Router, lógica de polling timeout).

### Padrão: State Machine do Checkout

`CheckoutShell` é o único detentor do estado de step. Os step components (`StepPaymentForm`, `StepProcessing`, `StepThreeDs`, `StepSuccess`, `StepError`) são pure display components que recebem dados e callbacks via props.

```
Estado interno: 'FORM' | 'PROCESSING' | 'THREE_DS' | 'SUCCESS' | 'ERROR'
Transições:
  FORM → PROCESSING (ao submeter)
  PROCESSING → SUCCESS (status COMPLETED)
  PROCESSING → ERROR (status DECLINED | FAILED | timeout)
  PROCESSING → THREE_DS (HTTP 202)
  THREE_DS → PROCESSING (ao retornar do ACS com ?3ds_complete=true)
  ERROR → FORM (retry, apenas para erros de rede e FAILED)
```

### Padrão: API Client

`lib/api/client.ts` expõe uma função que lê a API key de `sessionStorage` e injeta o header `Authorization: Bearer`. Em qualquer resposta 401, limpa a key e redireciona para `/login`. Nunca lança exceção para 401 — trata internamente.

```
apiFetch(path, options?) → Promise<Response>
  - Injeta Authorization: Bearer {sessionStorage.mony_api_key}
  - Em 401: sessionStorage.removeItem('mony_api_key') + router.push('/login')
  - Para outros erros HTTP: retorna Response para o chamador tratar
```

---

## Persistence and Data Modeling

O frontend não possui banco de dados. O único estado persistente entre sessões é gerenciado via:

| Store | Chave | Valor | TTL |
|---|---|---|---|
| `sessionStorage` | `mony_api_key` | string (API key) | sessão do browser |
| `sessionStorage` | `checkout_idempotency_key` | `req_<uuid-v4>` | sessão do browser |
| `sessionStorage` | `checkout_transaction_id` | UUID | sessão do browser |
| `sessionStorage` | `checkout_challenge_id` | string | sessão do browser |

`localStorage` não é utilizado. Cookies não são utilizados.

### TypeScript — Tipos principais

**`types/payment.ts`**
```
PaymentStatus: enum com 9 valores (CREATED | VALIDATED | PROCESSING | ...)
PaymentMethod: { card_token_id: string; masked_card?: string }
PaymentRequest: { amount: number; currency: string; idempotency_key: string; payment_method: PaymentMethod; customer_email?: string }
PaymentResponse: { transaction_id: string; status: PaymentStatus; amount: number; currency: string; masked_card?: string; challenge_id?: string; acs_url?: string; message?: string; created_at: string; updated_at: string }
```

---

## Non-Functional Requirements and Security

- **Dados sensíveis:** nenhum PAN, CVV ou dado de cartão bruto trafega pelo frontend. `card_token_id` é um token opaco.
- **API key:** armazenada exclusivamente em `sessionStorage`. Nunca em `localStorage`, cookies, ou logs de console.
- **CORS em dev:** Next.js `rewrites` em `next.config.ts` proxia `/api/*` para o Nginx, eliminando o problema de CORS em desenvolvimento.
- **HTTPS:** o Nginx (via ngrok) fornece TLS na camada de transporte. O frontend não gerencia certificados.
- **WCAG AA:** todos os componentes interativos devem passar no teste de contraste (≥ 4.5:1 para texto, ≥ 3:1 para UI). Os tokens de `design.json` foram validados contra este critério.
- **Performance:** first load do checkout deve completar em menos de 3s em conexão 4G simulada (Lighthouse). A aplicação não carrega assets pesados — sem imagens de produto, sem bibliotecas de gráfico na rota de checkout.
- **Polling timeout:** o hook `usePayment` cancela o polling e transita para ERROR após 120s sem estado terminal. Evita requests infinitos em caso de falha no backend.
- **Idempotência:** a `Idempotency-Key` é gerada uma vez por sessão de checkout e persistida em `sessionStorage`. Retries dentro da mesma sessão reutilizam a mesma key.

---

## Task Breakdown Preview

| # | Tarefa | Status |
|---|---|---|
| 001 | Scaffold Next.js 14 + pnpm + TS + Tailwind + shadcn init | planned |
| 002 | Design system: tailwind.config.ts tokens de design.json, globals.css, Inter font | planned |
| 003 | shadcn/ui base: button, input, card, badge, label, skeleton, tooltip, alert | planned |
| 004 | Shared components: StepIndicator, SecurityBadge, LoadingSpinner, EmptyState, ErrorState, CopyButton | planned |
| 005 | API client + tipos TypeScript: client.ts, payments.ts, types/payment.ts | planned |
| 006 | usePayment hook: mutation + polling + state machine transitions | needs-research (verificar GET /api/v1/payments/{id} no backend) |
| 007 | CheckoutShell + StepPaymentForm (React Hook Form + Zod) | planned |
| 008 | StepProcessing (spinner animado, mensagem de status) | planned |
| 009 | StepThreeDs (redirect + polling pós-retorno ACS) | planned |
| 010 | StepSuccess + StepError (telas terminais) | planned |
| 011 | Rota de checkout: app/(checkout)/checkout/page.tsx | planned |
| 012 | TransactionTable + FilterBar + useTransactions hook | planned |
| 013 | StatusBadge + TransactionDetail (polling 10s) | planned |
| 014 | Rotas do dashboard: login, dashboard, transactions/[id] | planned |
| 015 | Review: impeccable skill em todas as telas | planned |
| 016 | Commits semânticos por grupo de tarefa | planned |

---

## Implementation Checklist

- [ ] Verificar se `GET /api/v1/payments/{id}` existe no `PaymentController` do backend antes de task-006
- [ ] Verificar CORS no `docker/nginx/nginx.conf.template` ou confirmar que `next.config.ts` rewrites resolve o problema em dev
- [ ] Confirmar a URL de retorno do ACS com o time de backend (precisa ser configurável via env var: `NEXT_PUBLIC_CHECKOUT_RETURN_URL`)
- [ ] Node 18+ disponível no ambiente de desenvolvimento
- [ ] pnpm instalado globalmente (`npm install -g pnpm`)

---

## Examples

### Checkout — HTTP 202 → redirect → polling → SUCCESS

```
1. POST /api/v1/payments
   Body: { amount: 24990, currency: "BRL", idempotency_key: "req_abc", payment_method: { card_token_id: "tok_highrisk" } }
   Response: 202 { status: "CHALLENGE_PENDING", transaction_id: "txn-123", acs_url: "https://bank.example/acs?token=..." }

2. sessionStorage: { checkout_transaction_id: "txn-123", checkout_challenge_id: "ch_abc" }
   browser.location = "https://bank.example/acs?token=..."

3. [ACS authentication completes]
   browser.location = "http://localhost:3000/checkout?3ds_complete=true&transaction_id=txn-123"

4. CheckoutShell remonta → lê checkout_transaction_id = "txn-123" → PROCESSING

5. GET /api/v1/payments/txn-123 (t=0s) → status: "AUTHENTICATED"
   GET /api/v1/payments/txn-123 (t=2s) → status: "PROCESSING"
   GET /api/v1/payments/txn-123 (t=4s) → status: "COMPLETED"

6. CheckoutShell → SUCCESS
   StepSuccess exibe: "Pagamento confirmado", R$ 249,90, txn-123
```

### Dashboard — Lookup + auto-refresh UNKNOWN

```
1. Lojista insere IDs: "txn-123\ntxn-456"
2. useTransactions dispara 2x GET /api/v1/payments/{id} em paralelo
3. txn-123 → COMPLETED; txn-456 → UNKNOWN
4. Tabela: txn-456 renderiza com fundo warning + badge "Em reconciliação"
5. 30s depois → auto-refresh → txn-456 agora COMPLETED → row atualiza sem reload
```
