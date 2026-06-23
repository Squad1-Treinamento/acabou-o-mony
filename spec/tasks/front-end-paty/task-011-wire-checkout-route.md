---
id: task-011
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/specs/spec-006-frontend-checkout.md
---

# Wire Checkout Route and Root Layout

Criar a rota `/checkout` do Next.js, configurar o root layout com `QueryClientProvider` e verificar o fluxo e2e completo no browser.

## Local Context

**Files to create:**
- `frontend/app/(checkout)/checkout/page.tsx`
- (update) `frontend/app/layout.tsx`
- `frontend/next.config.ts` — add API rewrites

**Local dependencies:**
- `@tanstack/react-query`: `QueryClient`, `QueryClientProvider`
- `next/navigation`: `useSearchParams`
- `frontend/components/checkout/CheckoutShell.tsx` (task-007–010)

## Scope

1. **`app/layout.tsx`** (root layout — server component with client wrapper):
   - Create a `Providers` client component (separate file `app/providers.tsx`) that wraps children with `QueryClientProvider` and a `new QueryClient({ defaultOptions: { queries: { retry: false } } })`.
   - Root layout imports `Providers` and wraps `{children}`.
   - Inter font applied via `next/font/google`, class applied to `<html>`.
   - `lang="pt-BR"` on `<html>`, `bg-background min-h-screen` on `<body>`.

2. **`next.config.ts`** — add rewrites:
   ```
   async rewrites() {
     return [{ source: '/api/:path*', destination: `${process.env.NEXT_PUBLIC_API_URL}/api/:path*` }]
   }
   ```
   This proxies all `/api/*` requests to the backend, eliminating CORS issues in development.

3. **`app/(checkout)/checkout/page.tsx`** (`'use client'`):
   - Reads `useSearchParams()` to detect `?3ds_complete=true`.
   - Renders `<CheckoutShell amount={24990} currency="BRL" />` (hardcoded test values for dev — will be replaced by merchant config in a future task).
   - The `CheckoutShell` handles `3ds_complete` detection internally via sessionStorage + URL check on mount.
   - Page wrapper: centered layout `max-w-[480px] mx-auto px-4 py-16` (matching design.json `contentWidth: 480px`).

4. **Manual e2e verification** in browser at `http://localhost:3000/checkout`:
   - Step 1: Form renders with payment summary (R$ 249,90) and card token input.
   - Step 2: Enter any token → click "Pagar" → spinner appears.
   - Step 3: Verify mock API responses work for COMPLETED, DECLINED, and FAILED paths.
   - Step 4: Verify 3DS path by simulating a `?3ds_complete=true&transaction_id=xxx` URL on the checkout page.

## Acceptance Criteria and Tests

- Success: `http://localhost:3000/checkout` loads without errors; checkout form visible.
- Success: `QueryClientProvider` wraps the entire app (verify with React DevTools).
- Success: `/api/v1/payments` proxied correctly (no CORS error in browser DevTools Network tab when making API calls).
- Success: Layout has Inter font (visible in DevTools → Elements → computed font).
- Success: `pnpm build` and `pnpm start` both work.
- Failure: CORS error in browser when calling the API → verify `next.config.ts` rewrites are applied.
- Tests: Manual browser e2e.

## Constraints and Negative Instructions

- Do NOT use hardcoded API key in the checkout page; the key comes from the merchant integration (not in scope for this task — leave `getApiKey()` to return from sessionStorage).
- Do NOT put `QueryClientProvider` in a server component; wrap it in a separate `'use client'` `Providers` component.
- Do NOT add multiple `QueryClient` instances; one shared instance per app.
- The `contentWidth: 480px` constraint from design.json must be respected for the checkout container.
