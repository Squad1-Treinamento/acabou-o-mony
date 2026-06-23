export function generateIdempotencyKey(): string {
  return "req_" + crypto.randomUUID();
}

export function getOrCreateCheckoutKey(): string {
  const existing = sessionStorage.getItem("checkout_idempotency_key");
  if (existing) return existing;
  const key = generateIdempotencyKey();
  sessionStorage.setItem("checkout_idempotency_key", key);
  return key;
}

export function clearCheckoutSession(): void {
  sessionStorage.removeItem("checkout_idempotency_key");
  sessionStorage.removeItem("checkout_transaction_id");
  sessionStorage.removeItem("checkout_challenge_id");
}
