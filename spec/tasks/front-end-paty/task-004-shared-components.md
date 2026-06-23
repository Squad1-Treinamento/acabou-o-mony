---
id: task-004
status: planned
links:
  - spec/tech-plans/plan-006-frontend.md
  - spec/specs/spec-006-frontend-checkout.md
  - spec/specs/spec-007-merchant-dashboard.md
---

# Build Shared UI Components

Criar os 6 componentes compartilhados usados por checkout e dashboard: StepIndicator, SecurityBadge, LoadingSpinner, EmptyState, ErrorState e CopyButton.

## Local Context

**Files to create:**
- `frontend/components/shared/StepIndicator.tsx`
- `frontend/components/shared/SecurityBadge.tsx`
- `frontend/components/shared/LoadingSpinner.tsx`
- `frontend/components/shared/EmptyState.tsx`
- `frontend/components/shared/ErrorState.tsx`
- `frontend/components/shared/CopyButton.tsx`

**Local dependencies:**
- `lucide-react`: `Check`, `Lock`, `Copy`, `AlertCircle` icons
- `frontend/components/ui/button.tsx` (task-003)
- `frontend/components/ui/card.tsx` (task-003)
- `frontend/components/ui/tooltip.tsx` (task-003)
- Tailwind tokens from task-002

## Scope

1. **StepIndicator.tsx** — accepts `steps: string[]` and `currentStep: number` props.
   - Renders circles connected by horizontal lines.
   - Completed step (index < currentStep): filled `bg-primary`, white `Check` icon inside.
   - Active step (index === currentStep): filled `bg-primary`, white step number inside.
   - Inactive step (index > currentStep): white bg, `border-2 border-border`, `text-textSecondary` number.
   - Connector line: `bg-border` default; `bg-primary` between completed steps.
   - Step label below each circle in `text-[13px] text-textSecondary`.

2. **SecurityBadge.tsx** — no props.
   - Renders `Lock` icon (16px, `text-textSecondary`) + "Pagamento seguro" text in `text-[13px] text-textSecondary`.
   - Horizontal flex layout, gap-1.5.

3. **LoadingSpinner.tsx** — accepts `size?: 'sm' | 'md' | 'lg'` (default `'md'`).
   - Animated ring using `animate-spin border-t-primary border-border rounded-full`.
   - Sizes: sm=16px, md=24px, lg=40px (width and height).

4. **EmptyState.tsx** — accepts `title: string`, `subtitle?: string`.
   - Centered flex column layout.
   - Title in `text-textSecondary font-medium`, subtitle in `text-[13px] text-textSecondary`.

5. **ErrorState.tsx** — accepts `message: string`, `onRetry?: () => void`.
   - Card with `border-error/20` border.
   - `AlertCircle` icon in `text-error` (20px).
   - Message in `text-textPrimary`.
   - "Tentar novamente" Button (secondary variant) rendered only when `onRetry` is defined.

6. **CopyButton.tsx** — accepts `value: string`.
   - Icon-only button with `Copy` icon (16px).
   - On click: `navigator.clipboard.writeText(value)`, then show "Copiado!" tooltip for 1500ms, then reset.
   - Uses shadcn `Tooltip` wrapping the button.
   - ARIA label: `"Copiar"`.

## Acceptance Criteria and Tests

- Success: `StepIndicator` renders 3 steps correctly for currentStep=0, 1, and 2.
- Success: `LoadingSpinner` renders at 3 sizes without layout shift.
- Success: `ErrorState` shows retry button when `onRetry` is passed; hides it when not passed.
- Success: `CopyButton` writes to clipboard and shows tooltip (manual test in browser).
- Success: `pnpm build` passes with no TypeScript errors.
- Failure: Any hardcoded hex color outside Tailwind classes → use token class instead.
- Tests: No automated tests required; visual verification in browser.

## Constraints and Negative Instructions

- Do NOT use `style={{}}` inline styles.
- Do NOT import from files outside `components/ui/` and `lucide-react`.
- Do NOT add animation beyond Tailwind's `animate-spin`; no CSS keyframes (per design.json `animations.avoid`).
- `CopyButton` must NOT use `document.execCommand` (deprecated); use `navigator.clipboard.writeText`.
- All icon-only interactive elements must have an `aria-label`.
