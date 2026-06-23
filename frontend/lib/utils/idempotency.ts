export function generateIdempotencyKey(): string {
  return crypto.randomUUID();
}

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function getOrCreateCheckoutKey(): string {
  const existing = sessionStorage.getItem("checkout_idempotency_key");
  if (existing && UUID_REGEX.test(existing)) return existing;
  const key = generateIdempotencyKey();
  sessionStorage.setItem("checkout_idempotency_key", key);
  return key;
}

export function clearCheckoutSession(): void {
  sessionStorage.removeItem("checkout_idempotency_key");
  sessionStorage.removeItem("checkout_transaction_id");
  sessionStorage.removeItem("checkout_challenge_id");
}
