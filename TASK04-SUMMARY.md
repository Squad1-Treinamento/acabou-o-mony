# Task 04 — Login Screen

## What was done

### Created
- `frontend/src/components/LoginForm.tsx` — Mock login component with:
  - Merchant ID and API Key fields
  - Client-side validation (required fields)
  - Submit button disabled when fields empty
  - Error message display
  - Calls `login()` from AuthContext on valid submit
  - Test credentials hint box

### Modified
- `frontend/src/App.tsx` — Replaced temporary test code (tasks 02-03) with:
  - Conditional rendering: `<LoginForm />` when `!isAuthenticated`, Dashboard when authenticated
  - `useEffect` to sync `apiKey` with `apiClient`

### Unchanged
- `AuthContext`, `api.ts`, `payment.ts`, `auth.ts` — no modifications needed

## Next
Task 05 — Payment Form (`frontend/src/components/PaymentForm.tsx`)

# Need
Change MerchantID to be the correct and the API key to be the correct