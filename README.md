# Acabou o Mony

**Payment platform for live and conversational commerce.** Process transactions in under one second, scale horizontally during demand spikes, and keep customers inside their stream or chat — no redirects, no friction.

---

## Architecture at a Glance

| Layer | Technology |
|---|---|
| Runtime | Java 21, Spring Boot 3.3.5, Spring WebFlux (Netty) |
| Database | PostgreSQL 16 via R2DBC (reactive, non-blocking) |
| Cache | Redis via Spring Data Redis Reactive |
| Auth | Spring Security, JWT, 3DS 2.x |
| Proxy | Nginx (TLS termination, rate limiting) |
| Tunnel | ngrok (local/staging public exposure) |
| Acquiring | Mercado Pago API |
| CI | GitHub Actions |

The system is fully reactive from edge to database, using Project Reactor's event loop for non-blocking I/O. A separate 3DS Engine handles step-up authentication via Redis state, keeping the fast path under 1 second. Nginx terminates TLS 1.3 and rate-limits before traffic reaches the application.

For the full architectural blueprint, see [`ARCHITECTURE.md`](ARCHITECTURE.md).

---

## Repository Structure

```
├── 3ds-engine/          # Java microservice — 3D Secure 2.x auth engine
├── core-payment/        # Additional payment module
├── src/                 # Source code root
├── spec/                # SDD documentation (vision, user stories, specs, ADRs, tech plans, tasks)
├── docker/              # Docker infrastructure configs
├── docs/                # Supplementary documentation
├── tests/               # Python integration tests
├── bruno-collection/    # API request collection (Bruno)
├── meu_ambiente/        # Environment setup scripts
├── venv_locust/         # Locust load testing environment
├── docker-compose.yml   # Full-stack orchestration
└── pom.xml              # Maven root (Java modules)
```

---

## Quick Start

```bash
# Full stack (Nginx + ngrok + app)
docker compose up -d --wait

# Run Java tests
cd 3ds-engine && mvn test

# Run Python integration tests
pytest tests/

# Integration test against live stack
docker compose up -d --wait && pytest tests/test_entrypoint.py
```

See [`CONTEXT.md`](CONTEXT.md) for the complete command reference and environment details.

---

## Documentation

| Document | Purpose |
|---|---|
| [`spec/index.md`](spec/index.md) | Entry point for all project specs (vision, user stories, ADRs, tech plans, tasks) |
| [`CONTEXT.md`](CONTEXT.md) | Stack, commands, environments |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Detailed architectural blueprint |
| [`AGENTS.md`](AGENTS.md) | AI agent workflow rules and guardrails |
| [`CODEGEN.md`](CODEGEN.md) | Code generation conventions |
| [`TESTING_GUIDE.md`](TESTING_GUIDE.md) | Testing approach and guidelines |

---

## Design Tenets

- **Specs first** — requirements are documented before code changes; specs are the source of truth.
- **Reactive by default** — non-blocking I/O from edge to database for maximum throughput under load.
- **<1s SLA** — fast-path validation, idempotency caching, and risk-based authentication keep p99 under one second.
- **Security at every layer** — TLS 1.3 termination, AES-256-GCM tokenization, 3DS 2.x, and write-once audit logs.
- **Horizontal scaling** — stateless containers with CPU/memory-based auto-scaling and graceful shutdown.
