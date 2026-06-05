---
id: spec-001
status: active
links:
  - spec/specs/index.md
  - spec/user-stories/index.md
  - spec/tech-plans/index.md
---
# Entry Point with ngrok + Nginx

## Context and Primary Objective
- Context: Local production-simulated entry point for the payment platform before the Java API exists.
- Objective: Provide a public HTTPS entry point and internal reverse proxy that can route to future Backend API instances using environment variables.

## Functional Requirements (Behavior)
- User story: As a platform operator, I want a local production-simulated entry point with ngrok and Nginx so that external HTTPS traffic can be tested end-to-end without a public server.
- Business rules:
  - The environment is a single local production simulation; dev behavior is toggled only by environment variables.
  - The API Gateway is the public ngrok endpoint and is required for the flow.
  - TLS terminates at ngrok; ngrok forwards HTTP to the Nginx Reverse Proxy & Load Balancer.
  - Nginx uses round robin for request distribution.
  - Backend API targets are defined by `UPSTREAMS` as a comma-separated list of `host:port`.
  - If only one backend is needed, `UPSTREAM_HOST` and `UPSTREAM_PORT` may be used to build `UPSTREAMS`.
  - If `UPSTREAMS` is empty or invalid, Nginx fails fast on startup with a clear error.
  - `/healthz` is served locally by Nginx and does not call the backend.
  - Rate limiting is enabled with `rate=10r/s burst=20 nodelay`.
  - Forward headers: `X-Forwarded-For`, `X-Forwarded-Proto`, `X-Forwarded-Host`, and `Host`.
  - Timeouts: `proxy_connect_timeout=5s`, `proxy_read_timeout=60s`, `proxy_send_timeout=60s`.
  - `client_max_body_size` is `2m`.
  - Gzip is disabled.
  - Nginx uses combined access logs.
  - Ports: Nginx is exposed as `8080:80`; ngrok dashboard is exposed as `4040:4040`.
  - `NGROK_AUTHTOKEN` is required; ngrok fails fast if missing.
  - Compose file lives at repo root.

## Acceptance Criteria (BDD)
- Given `NGROK_AUTHTOKEN` and a valid `UPSTREAMS`, when the stack starts, then ngrok logs the public HTTPS URL and Nginx starts successfully.
- Given the stack is running, when a request hits the ngrok public URL, then it is forwarded to Nginx and routed to the Backend API with the forwarded headers present.
- Given the stack is running, when `GET /healthz` is called on Nginx, then it returns `200 OK` with body `ok` without contacting the backend.
- Given `UPSTREAMS` is empty or malformed, when Nginx starts, then it exits with a non-zero status and a clear validation error.
- Given `NGROK_AUTHTOKEN` is missing, when ngrok starts, then the container exits with a non-zero status and logs the missing token.
- Given rate limit thresholds are exceeded, when excessive requests are sent, then some requests are rejected with `503` by Nginx.
- Given only `UPSTREAM_HOST` and `UPSTREAM_PORT` are provided, when the stack starts, then the generated config uses the equivalent single `UPSTREAMS` entry.

## Interface and Data Contracts
- Data schema (environment variables):
  - `NGROK_AUTHTOKEN`: string, required.
  - `UPSTREAMS`: string, required, format `host:port,host:port` (no scheme).
  - `UPSTREAM_HOST`: string, optional, used only if `UPSTREAMS` is empty.
  - `UPSTREAM_PORT`: integer, optional, used only if `UPSTREAMS` is empty.
- API contracts:
  - `GET /healthz` -> `200 OK` with body `ok`.
  - All other paths are proxied to the Backend API.
- Ports:
  - Host `8080` -> Nginx `80`.
  - Host `4040` -> ngrok dashboard `4040`.

## Tech Stack and Constraints
- Technologies: Docker Engine, Docker Compose v2+, `nginx:alpine`, `ngrok/ngrok:alpine`.
- Design standards: minimal services, env-driven configuration, fail fast on invalid configuration.

## Examples
- Input:
  - `.env`:
    - `NGROK_AUTHTOKEN=example-token`
    - `UPSTREAMS=app:8080`
- Output:
  - ngrok logs a public HTTPS URL.
  - `curl http://localhost:8080/healthz` returns `ok`.
