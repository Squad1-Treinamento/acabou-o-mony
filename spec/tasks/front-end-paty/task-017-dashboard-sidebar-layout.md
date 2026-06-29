---
id: task-017
status: planned
links:
  - spec/specs/spec-007-merchant-dashboard.md
  - spec/tasks/front-end-paty/task-014-wire-dashboard-routes.md
---

# Dashboard Sidebar Navigation + Shared Layout

Adicionar sidebar de navegação persistente ao dashboard do merchant, substituindo o `AppHeader` nas páginas internas por um layout compartilhado com sidebar.

## Local Context

- **Branch:** `front-end-paty`
- **Spec:** spec-007 §Extended Features — Dashboard Navigation
- **Affected files:**
  - `frontend/components/dashboard/DashboardSidebar.tsx` (new)
  - `frontend/app/(dashboard)/dashboard/layout.tsx` (new)
  - `frontend/app/(dashboard)/dashboard/page.tsx` (update: remove AppHeader + auth check)
  - `frontend/app/(dashboard)/dashboard/transactions/[id]/page.tsx` (update: remove AppHeader + wrapper)

## Implementation Steps

### 1. DashboardSidebar.tsx

```tsx
"use client";
// Nav items: Transações (/dashboard exact), Analytics, Reconciliação, Webhooks, Configurações
// Active detection: exact match for /dashboard; startsWith(href + "/") for others
// Sidebar footer: masked API key + Logout button that clears sessionStorage
// Width: 220px, bg-white, border-r border-slate-100, sticky top-0 h-screen
```

Nav items (icon, label, href):
- `LayoutDashboard` → Transações → `/dashboard` (exact)
- `TrendingUp` → Analytics → `/dashboard/analytics`
- `RefreshCw` → Reconciliação → `/dashboard/reconciliation`
- `Webhook` → Webhooks → `/dashboard/webhooks`
- `Settings` → Configurações → `/dashboard/settings`

Active item styles: `bg-[#0D2B1E]/[0.07] text-[#0D2B1E] font-medium`
Inactive styles: `text-slate-500 hover:text-slate-800 hover:bg-slate-50`

### 2. dashboard/layout.tsx

Client component. Auth check via `useEffect` — redirects to `/login` if no `mony_api_key` in sessionStorage.

Layout structure:
```
<div className="flex min-h-screen bg-[#F8F9FB]">
  <div className="hidden md:block"> <DashboardSidebar /> </div>
  <header className="md:hidden fixed top-0 ..."> mobile top bar </header>
  <main className="flex-1 overflow-auto pt-14 md:pt-0">
    {children}
  </main>
</div>
```

Mobile top bar: logo "Vibe Store" + links Transações / Analytics / Config.

### 3. Update dashboard/page.tsx

Remove:
- `<AppHeader showLogout />` import and usage
- `useEffect` auth check
- Outer `<div className="min-h-screen bg-[#F8F9FB] flex flex-col">` wrapper

Keep: `<div className="max-w-6xl mx-auto w-full px-6 py-8 space-y-6">` content wrapper.

### 4. Update transactions/[id]/page.tsx

Remove:
- `<AppHeader showLogout />` import and usage
- `<div className="min-h-screen bg-[#F8F9FB] flex flex-col">` wrapper

Keep: `<main className="flex-1 max-w-2xl mx-auto w-full px-6 py-8">` content wrapper.

## Acceptance Criteria and Tests

- Success: Sidebar visible on md+ screens; mobile top bar visible on <md
- Success: Active nav item highlights correctly on each route
- Success: Logout clears `sessionStorage` and redirects to `/login`
- Success: Unauthenticated access to any `/dashboard/*` redirects to `/login`
- Success: `dashboard/page.tsx` no longer imports `AppHeader`
- Failure: `AppHeader` still rendered inside any dashboard page (layout provides nav)

## Constraints

- Do NOT use `localStorage` for any session state
- Do NOT add new npm dependencies
- Sidebar width must be exactly 220px; do not exceed
