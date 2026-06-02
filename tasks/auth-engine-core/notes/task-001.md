# task-001 — Scaffold Spring Boot WebFlux Project for 3DS Engine

## Implementation Notes

- **Decisions:** None. Straightforward scaffold per task spec.
- **Deviations:** Removed `testcontainers-redis` dependency (artifact does not exist on Maven Central) — using `testcontainers-bom` with `testcontainers` and `junit-jupiter` instead. Integration tests will use `GenericContainer` with Redis image directly.
- **Trade-offs:** Used Spring Boot 3.3.5 (latest stable 3.3.x line). Java 21 target, compiled with JDK 25.
- **Risks:** None identified.
