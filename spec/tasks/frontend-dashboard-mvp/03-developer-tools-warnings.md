# Task 03: Add Warning Banners to Developer Tools

**Status**: Ready to implement  
**Estimated Time**: 30 minutes  
**Dependencies**: Task 02 (Navigation structure)

---

## Goal

Add clear warning banners to the Developer Tools section to indicate that these features are for **testing and development purposes only**, not for production merchant use.

---

## Context

Real merchants don't manually create payments in a dashboard. Payments come from customers via:
- E-commerce checkout pages
- API integrations
- Mobile apps
- Live commerce platforms

The "Create Payment" and "Idempotency Test" features are **developer tools** for testing the payment API, not merchant-facing features.

Adding warning banners makes this distinction clear and prevents confusion.

---

## What You're Building

### Warning Banner Design

```
┌─────────────────────────────────────────────────────────┐
│ ⚠️  Developer Tools                                     │
│                                                         │
│ This section is for testing and development purposes   │
│ only. In production, payments are created via API      │
│ integration, not manually.                             │
└─────────────────────────────────────────────────────────┘
```

---

## Files to Create

### 1. `frontend/src/components/DevToolsWarning.tsx`

Reusable warning banner component.

```typescript
export function DevToolsWarning() {
  return (
    <div className="mb-6 bg-orange-50 border border-nu-warning/30 rounded-xl p-4">
      <div className="flex items-start gap-3">
        <span className="text-2xl">⚠️</span>
        <div>
          <h4 className="font-semibold text-nu-warning mb-1">
            Developer Tools
          </h4>
          <p className="text-sm text-nu-text-secondary leading-relaxed">
            This section is for testing and development purposes only. 
            In production, payments are created via API integration, not manually.
          </p>
        </div>
      </div>
    </div>
  );
}
```

---

## Files to Modify

### 1. `frontend/src/components/PaymentForm.tsx`

Add warning banner at the top of the component.

#### Changes Required:

1. **Import DevToolsWarning**

```typescript
import { DevToolsWarning } from './DevToolsWarning';
```

2. **Add Warning Banner**

```typescript
export function PaymentForm() {
  // ... existing state and logic ...

  return (
    <div className="max-w-2xl mx-auto">
      {/* NEW: Warning Banner */}
      <DevToolsWarning />
      
      {/* Existing content */}
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-bold text-nu-text-primary">Create Payment</h2>
      </div>

      {/* Rest of existing form */}
      <form onSubmit={handleSubmit} className="nu-card mb-6">
        {/* ... existing form fields ... */}
      </form>

      {/* ... existing response display ... */}
    </div>
  );
}
```

---

### 2. `frontend/src/components/IdempotencyTest.tsx`

Add warning banner at the top of the component (will be created in Task 04).

**Note**: This will be implemented in Task 04 when we extract the IdempotencyTest component.

For now, if you're implementing tasks in order, you can skip this and add it in Task 04.

---

## Alternative: Inline Warning (if IdempotencyTest doesn't exist yet)

If you haven't created `IdempotencyTest.tsx` yet, you can add the warning inline to `PaymentForm.tsx` when in advanced mode:

```typescript
// In PaymentForm.tsx, if advancedMode is still there
{advancedMode && (
  <div className="mb-6">
    <DevToolsWarning />
  </div>
)}
```

---

## Visual Design

### Color Scheme

```css
/* Warning banner colors (already in index.css) */
--color-nu-warning: #FF9500;

/* Additional styles for warning banner */
.warning-banner {
  background: #FFF7ED;  /* Light orange background */
  border: 1px solid rgba(255, 149, 0, 0.3);  /* Orange border */
  border-radius: 12px;
  padding: 1rem;
}
```

### Layout

- **Icon**: Large warning emoji (⚠️) on the left
- **Title**: Bold "Developer Tools" in warning color
- **Description**: Gray text explaining the purpose
- **Spacing**: Margin bottom to separate from content below

---

## Acceptance Criteria

- [ ] DevToolsWarning component created
- [ ] Warning banner appears on Create Payment page
- [ ] Warning banner appears on Idempotency Test page (Task 04)
- [ ] Banner has orange/warning color scheme
- [ ] Banner is visually distinct from other content
- [ ] Text is clear and concise
- [ ] Banner doesn't interfere with form functionality
- [ ] Banner is responsive (works on mobile)

---

## Testing Checklist

### Manual Testing

1. **Create Payment Page**
   - [ ] Navigate to Developer Tools → Create Payment
   - [ ] Warning banner appears at top
   - [ ] Banner has warning icon and text
   - [ ] Form still works correctly below banner

2. **Idempotency Test Page** (after Task 04)
   - [ ] Navigate to Developer Tools → Idempotency Test
   - [ ] Warning banner appears at top
   - [ ] Test functionality still works

3. **Visual Check**
   - [ ] Banner has orange background
   - [ ] Text is readable
   - [ ] Icon is visible
   - [ ] Spacing is appropriate

4. **Responsive Check**
   - [ ] Banner looks good on desktop
   - [ ] Banner looks good on tablet
   - [ ] Banner looks good on mobile

---

## Validation Commands

```bash
# Type checking
npm run type-check

# Linting
npm run lint

# Development server
npm run dev
```

---

## Notes

- The warning banner uses existing color variables from `index.css`
- The component is reusable across multiple developer tool pages
- The warning is informational only (doesn't block functionality)
- Future developer tools can reuse this component

---

## Optional Enhancements (Out of Scope for MVP)

If you want to enhance this later:

1. **Dismissible Banner**: Add close button to hide warning
2. **Local Storage**: Remember if user dismissed warning
3. **Different Warning Levels**: Info, Warning, Error variants
4. **Links**: Add link to API documentation
5. **Sandbox Mode Toggle**: Add toggle to switch between test/production modes

---

**Next Task**: `04-idempotency-test-extraction.md`
