"""
BDD Timing Test:
  Dado que eu inicio uma transação,
  quando ela é processada,
  então ela foi concluída em menos de 1 segundo.

Requires:
  - All Docker services running: docker compose up -d
  - Merchant seeded in DB (V5 migration inserts 'teste_key' merchant)
  - pip install requests pytest
"""

import time
import uuid
import urllib.request
import urllib.error
import json
import unittest


BASE_URL = "http://localhost:8080"
API_KEY = "teste_key"
SLA_SECONDS = 1.0


def _post_payment(idempotency_key: str) -> tuple[int, dict, float]:
    payload = json.dumps({
        "amount": 5000,
        "currency": "BRL",
        "idempotency_key": idempotency_key,
        "payment_method": {
            "type": "CARD",
            "masked_card": "411111XXXXXX1111",
            "card_token_id": "tok-test-timing-001"
        }
    }).encode("utf-8")

    req = urllib.request.Request(
        f"{BASE_URL}/api/v1/payments",
        data=payload,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {API_KEY}",
        },
        method="POST",
    )

    start = time.monotonic()
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            elapsed = time.monotonic() - start
            body = json.loads(resp.read().decode("utf-8"))
            return resp.status, body, elapsed
    except urllib.error.HTTPError as e:
        elapsed = time.monotonic() - start
        body = json.loads(e.read().decode("utf-8")) if e.fp else {}
        return e.code, body, elapsed


class TransactionTimingTest(unittest.TestCase):

    def test_given_transaction_when_processed_then_completed_in_under_one_second(self):
        """
        Dado que eu inicio uma transação (baixo risco, < R$500),
        quando ela é processada pelo gateway,
        então a resposta chega em menos de 1 segundo.
        """
        idem_key = str(uuid.uuid4())

        status, body, elapsed = _post_payment(idem_key)

        print(f"\n  → Status: {status} | Elapsed: {elapsed:.3f}s | Body: {body}")

        # Transação low-risk (5000 centavos = R$50): deve responder 200 ou 202
        self.assertIn(
            status, (200, 202),
            f"Expected 200 or 202, got {status}. Body: {body}"
        )

        # SLA: resposta em menos de 1 segundo
        self.assertLess(
            elapsed, SLA_SECONDS,
            f"Transaction took {elapsed:.3f}s — expected < {SLA_SECONDS}s (SLA violated)"
        )

        print(f"  ✓ PASS: transaction completed in {elapsed:.3f}s (SLA < {SLA_SECONDS}s)")

    def test_given_cached_transaction_when_replayed_then_fast_path_under_100ms(self):
        """
        Dado que uma transação já foi processada (idempotency key cached),
        quando a mesma requisição é reenviada,
        então a resposta vem do cache em menos de 100ms (fast-path).
        """
        idem_key = str(uuid.uuid4())

        # First call — processes transaction
        status1, _, _ = _post_payment(idem_key)
        self.assertIn(status1, (200, 202), f"First call failed with {status1}")

        # Second call — should hit cache
        status2, body2, elapsed2 = _post_payment(idem_key)

        print(f"\n  → Cache hit status: {status2} | Elapsed: {elapsed2:.3f}s")

        self.assertIn(status2, (200, 202), f"Cache replay got {status2}")
        self.assertLess(
            elapsed2, 0.1,
            f"Cache fast-path took {elapsed2:.3f}s — expected < 100ms"
        )

        print(f"  ✓ PASS: cache fast-path returned in {elapsed2:.3f}s")


if __name__ == "__main__":
    unittest.main(verbosity=2)
