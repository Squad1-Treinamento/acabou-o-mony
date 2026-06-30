# Frontend — Session Summary

**Date:** 2026-06-30
**Branch:** `frontend-mvp`
**Stack:** Vite + React 19 + TypeScript + Tailwind CSS v4 + Axios

---

## Session 10 — Nubank Visual Identity (DNA Roxo)

Replaced all Mercado Livre (ML) styling with Nubank design system.

### Files Changed

| File | Change |
|---|---|
| `tailwind.config.js` | Esvaziado (config migrada pra CSS v4) |
| `src/index.css` | `@import "tailwindcss"` + `@theme` com cores `nu-*` + classes utilitárias (`nu-card`, `nu-btn-primary`, `nu-input`, `nu-badge-*`) + animações |
| `index.html` | Fonte Inter adicionada |
| `src/App.css` | **Removido** (legado Vite) |
| `src/App.tsx` | Header branco com título roxo, tabs com indicador roxo, shadow flutuante |
| `src/components/LoginForm.tsx` | Card `nu-card` + `animate-fadeIn`, botão `nu-btn-primary` (roxo pill) |
| `src/components/PaymentForm.tsx` | Inputs `nu-input` (rounded-xl, foco roxo), botões `rounded-full` |
| `src/components/TransactionList.tsx` | Badges `nu-badge-*`, tabela com hover roxo claro |
| `src/components/TransactionDetails.tsx` | Card `nu-card`, labels `nu-label`, code blocks `bg-nu-bg rounded-lg` |

### Design Tokens (em `src/index.css`)

| Token | Hex | Uso |
|---|---|---|
| `nu-purple` | `#820AD1` | Botões, links, header, tabs ativas |
| `nu-purple-dark` | `#6A07A8` | Hover de elementos roxos |
| `nu-purple-light` | `#F3E8FF` | Badges, backgrounds sutis |
| `nu-bg` | `#F5F5F5` | Fundo da página |
| `nu-surface` | `#FFFFFF` | Cards, superfícies |
| `nu-text-primary` | `#1A1A1A` | Títulos, valores |
| `nu-text-secondary` | `#6B6B6B` | Descrições, metadados |
| `nu-text-muted` | `#A3A3A3` | Placeholders, hints |
| `nu-border` | `#E8E8E8` | Bordas, divisores |
| `nu-success` | `#00A86B` | COMPLETED |
| `nu-error` | `#E74C3C` | DECLINED/FAILED |
| `nu-warning` | `#FF9500` | PROCESSING/CHALLENGE_PENDING |

### Key Design Decisions

- **Tailwind v4**: Config via `@theme` em CSS, não via `tailwind.config.js`
- **Component classes**: `.nu-card`, `.nu-btn-primary`, `.nu-input`, `.nu-badge-*` definidos em `@layer components` no `index.css`
- **Animações mínimas**: `fadeIn` em cards, `scale(0.97)` em botões ao click
- **Sem dark mode**: Light mode only
- **Fonte Inter**: carregada via Google Fonts no `index.html`

### Known Issue: 401 on GET /api/v1/payments

Ao clicar na aba Transactions, o backend retorna **401 Unauthorized**.
- **Não é causado pelas mudanças de CSS** — a lógica de auth (`api.ts`, `AuthContext.tsx`) não foi alterada.
- Provável causa: backend em `localhost:8080` não está rodando, ou a API key `test-key` não é mais aceita pelo backend atual.
- Credenciais usadas: `merchant_id = 00000000-0000-0000-0000-000000000001`, `api_key = test-key`.

---

## Commands

```bash
npm run dev          # Dev server (localhost:5173 → proxy /api → localhost:8080)
npm run type-check   # tsc --noEmit
npm run lint         # oxlint
npm run build        # tsc -b && vite build
```

---

## Next Steps

1. **Fix 401** — verificar se backend está rodando e se `test-key` é aceita
2. Real authentication (when backend supports it)
3. Refunds UI
4. Webhooks configuration
5. Transaction search
6. End-to-end testing with backend