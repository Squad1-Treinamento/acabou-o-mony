# Implementation Notes — task-006: Async HTTP/2 Callback to Core Service

## Decisions
- `CallbackNotifier.notifyCore()` returns `Mono<Void>` for testability; `AuthVerificationService` calls `.subscribe()` on it for fire-and-forget semantics.
- `WebClient.Builder` injected instead of creating a raw `WebClient` — follows Spring Boot conventions and makes it configurable by auto-configuration.
- Callback failure logged at WARN level (not ERROR) since Core can recover via Redis polling.
- Retry: 1 retry after 1s delay on any error (HTTP error or connection failure).

## Deviations
- No explicit HTTP/2 protocol configuration on `WebClient.Builder` — Spring Boot's auto-configuration with Netty already supports HTTP/2 via `server.http2.enabled`. The WebClient inherits the connector from the application context. If explicit H2 is needed, add `.clientConnector(new ReactorClientHttpConnector(HttpClient.create().protocol(HttpProtocol.H2)))`.

## Trade-offs
- Using `doOnSuccess` + `.subscribe()` for fire-and-forget means the callback may be lost if the application shuts down between the response and the callback completion. Acceptable for MVP — Core polls Redis as fallback.
- `CallbackRequest` uses `record` for immutability, consistent with other DTOs.

## Risks
- If the callback URL is misconfigured or unreachable, retry adds up to 3 attempts with exponential backoff (1s–5s). After that, the callback is silently dropped (logged at WARN). This is by design — Core recovers via Redis polling.
- No circuit breaker — repeated failures could cause resource buildup from retries. Acceptable for MVP volume.

## Post-implementation Changes (recommendations applied)
- **Retry upgrade:** `Retry.fixedDelay(1, 1s)` → `Retry.backoff(3, 1s).maxBackoff(5s)` — exponential backoff with up to 3 retry attempts.
- **Thread isolation:** `.subscribe()` → `.subscribeOn(Schedulers.boundedElastic()).subscribe()` — callback HTTP call runs on a separate thread pool, preventing Netty event loop starvation.
