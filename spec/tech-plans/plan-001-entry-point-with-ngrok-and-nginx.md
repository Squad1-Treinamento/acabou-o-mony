---
id: plan-001
status: active
links:
  - spec/tech-plans/index.md
  - spec/specs/spec-001-entry-point-with-ngrok-and-nginx.md
  - spec/specs/index.md
  - spec/tasks/index.md
---
# Entry Point with ngrok + Nginx

## Architecture Overview and Data Flow
- Components: ngrok (API Gateway), Nginx (Reverse Proxy & Load Balancer), Backend API (future service).
- Flow: External HTTPS -> ngrok -> HTTP to Nginx -> Backend API instances via round robin.
- Example: A client hits the ngrok public URL; ngrok forwards to Nginx; Nginx routes to `app:8080` and returns the response.

## Stack and Dependencies
- Runtime versions: Docker Engine, Docker Compose v2+.
- Allowed libraries: `nginx:alpine`, `ngrok/ngrok:alpine`.
- Restricted libraries: any additional reverse proxy or tunnel services.

## Design Patterns and Code Conventions
- Folder structure: `docker/nginx/` for entrypoint and template; `docker-compose.yml` at repo root.
- Patterns: env-driven configuration, fail-fast validation, minimal services.
- Naming: kebab-case file names, uppercase environment variables.

## Persistence and Data Modeling
- Engine: none.
- Transaction rules: not applicable.
- Data rules: configuration comes from env vars only.

## Non-Functional Requirements and Security
- Auth: `NGROK_AUTHTOKEN` required for ngrok.
- Performance: Nginx rate limit `10r/s` with burst `20` and `nodelay`.
- Security: TLS termination at ngrok; no TLS between ngrok and Nginx.

## Task Breakdown Preview
- [planned] Add `docker-compose.yml` with ngrok and Nginx services and network.
- [planned] Add `docker/nginx/entrypoint.sh` to validate envs and render template.
- [planned] Add `docker/nginx/nginx.conf.template` with upstream and proxy rules.
- [planned] Add `.env.example` and update `.gitignore` to ignore `.env`.

## Implementation Checklist
- Define `UPSTREAMS` parsing and validation (regex `host:port`, non-empty).
- Support `UPSTREAM_HOST` + `UPSTREAM_PORT` fallback.
- Render Nginx config with `envsubst`.
- Configure `proxy_*` timeouts, headers, rate limit, `/healthz`.
- Expose `8080:80` and ngrok dashboard `4040:4040`.

## Examples
- Input:
  - `UPSTREAMS=app:8080`
  - `NGROK_AUTHTOKEN=token`
- Output:
  - ngrok logs a public HTTPS URL.
  - `curl http://localhost:8080/healthz` returns `ok`.
