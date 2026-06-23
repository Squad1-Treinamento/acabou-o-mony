---
id: task-008
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/specs/spec-006-frontend-checkout.md
---

# Build StepProcessing and Wire into CheckoutShell

Criar o componente de estado de carregamento animado e integrá-lo ao `CheckoutShell` para o passo de processamento.

## Local Context

**Files to create:**
- `frontend/components/checkout/StepProcessing.tsx`

**Files to modify:**
- `frontend/components/checkout/CheckoutShell.tsx` (replace PROCESSING placeholder div)

**Local dependencies:**
- `frontend/components/shared/LoadingSpinner.tsx` (task-004)
- Tailwind tokens from task-002

## Scope

1. **StepProcessing.tsx**
   - Props: `message?: string` (default: `'Processando pagamento...'`).
   - Layout: full-width centered flex column, min-height sufficient to avoid layout shift.
   - Renders `<LoadingSpinner size="lg" />`.
   - Renders message text below spinner in `text-textSecondary text-[16px]` (body size from design.json).
   - Gap between spinner and text: `24px` (`gap-6`, matching `design.json spacingRules.betweenCards`).
   - No button, no user interaction in this state.

2. **Wire into CheckoutShell.tsx**
   - Replace PROCESSING placeholder `<div>` with `<StepProcessing />`.
   - Pass `message` prop:
     - When polling after 3DS return: `"Verificando autenticação..."`
     - On initial submission: `"Processando pagamento..."` (default)
   - Add internal state `isPollingAfterThreeDs: boolean` to distinguish the two messages.

## Acceptance Criteria and Tests

- Success: `StepProcessing` renders centered spinner and default message on initial mount.
- Success: Message prop `"Verificando autenticação..."` renders when passed from `CheckoutShell` during polling.
- Success: Spinner animates (Tailwind `animate-spin` visible in browser).
- Success: `pnpm build` passes.
- Failure: Any CSS transform, scale, or aggressive animation → remove (per design.json `animations.avoid`).
- Tests: Manual visual check; no automated tests required.

## Constraints and Negative Instructions

- Do NOT add a cancel or back button to this component; the processing state is uninterruptible.
- Do NOT use `@keyframes` or custom CSS animations; `animate-spin` from Tailwind only.
- Do NOT use `position: fixed` or overlay effects; plain centered layout only.
- Spinner border color must use `border-t-primary` and `border-border` (design.json tokens).
