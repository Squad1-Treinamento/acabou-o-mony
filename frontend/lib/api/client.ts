export function getApiKey(): string | null {
  if (typeof window === "undefined") return null;
  return sessionStorage.getItem("mony_api_key");
}

export async function apiFetch(
  path: string,
  options: RequestInit = {}
): Promise<Response> {
  // Prefer the merchant session key; fall back to the public store key for consumer checkout
  const sessionKey = getApiKey();
  const apiKey = sessionKey ?? process.env.NEXT_PUBLIC_STORE_API_KEY ?? null;

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };

  if (apiKey) {
    headers["Authorization"] = `Bearer ${apiKey}`;
  }

  const response = await fetch(path, {
    ...options,
    headers,
  });

  // Only redirect to merchant login if a merchant session key was the one being used.
  // Consumer checkout uses NEXT_PUBLIC_STORE_API_KEY — a 401 there is a config error,
  // not a session expiry, so let it propagate to the checkout error handler instead.
  if (response.status === 401 && sessionKey) {
    if (typeof window !== "undefined") {
      sessionStorage.removeItem("mony_api_key");
      window.location.href = "/login";
    }
  }

  return response;
}
