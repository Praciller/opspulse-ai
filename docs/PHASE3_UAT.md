# Phase 3 UAT — Deterministic Risk Engine and Outbox

**Status:** Locally verified — 121 tests passed, 0 failures, 0 errors  
**Scope:** Phase 3 only; no AI provider, Kafka/Redpanda, frontend, import, reports, or deployment work.

## Fixture and reset

- Integration tests use PostgreSQL 16 Testcontainers through the existing test support.
- Flyway applies V1–V9 and the test profile loads only the existing user/demo seed.
- The scan-to-delivery business fixture is transaction-scoped; API and outbox fixtures use unique random UUIDs and do not expand the user seed.
- A clean test run starts a fresh container; individual tests use unique entities so they do not depend on execution order.
- The outbox scheduler is slowed in processor tests so delivery assertions invoke the processor deterministically.

## End-to-end matrix

| Area | Verified behavior | Evidence |
|---|---|---|
| Persistence | V8/V9 tables, constraints, indexes, typed defaults, Flyway ordering | `Phase2MigrationIntegrationTest` |
| Rules | Six rules, missing metrics, no-risk cases, threshold/severity boundaries, deterministic output | `RiskRuleTest` |
| Metrics | Product/order/supplier aggregates and fixed batched query count | `MetricRepositoryIntegrationTest` |
| Scheduler | `riskScanCron` is read through typed configuration and evaluated in UTC | `RiskSchedulerConfigurationTest` |
| Scan lifecycle | Scan creates events, repeat scans refresh without duplicates, cleared findings system-resolve, recurrence creates a new row | `RiskScanIntegrationTest` |
| Delivery flow | Risk-created outbox row moves to `PROCESSED`; CloudEvents ID is recorded once in `processed_events` | `RiskScanIntegrationTest` |
| Risk API | Authentication/RBAC, filters, pagination, date bounds, sorting, request IDs, status conflicts, audit snapshots, OpenAPI schemas | `RiskApiIntegrationTest` |
| Outbox processor | CloudEvents validation, duplicate IDs, 1/2/4/8/16-second retries, fifth-failure `DEAD`, sanitized errors, poison isolation, delivery-lag metric | `OutboxDeliveryIntegrationTest` |
| Dead-letter controls | ADMIN-only replay/discard, DEAD-only guard, payload preservation, payload omitted from responses, audit rows | `OutboxAdminApiIntegrationTest` |

## Verification commands

```powershell
.\gradlew.bat clean test --no-daemon
.\gradlew.bat clean build --no-daemon
.\gradlew.bat check --no-daemon
$env:JWT_SECRET='12345678901234567890123456789012'; docker compose config --quiet
git diff --check
```

The Compose command uses a temporary non-secret placeholder only for interpolation validation; it does not modify repository or user environment configuration.

The final `clean test` run completed with 121 tests, 0 failures, and 0 errors.

## Observability evidence

The scan and processor tests assert Micrometer counters/timers for scan duration, created/resolved risks, processed/retried/dead deliveries, and delivery lag. Structured logs include scan totals and sanitized outbox failure metadata without payloads or stack traces.

## Known limitations and Phase 4 handoff

- The hosted consumer registry remains a durable in-process no-op boundary; external Kafka/Redpanda consumers are deferred.
- The daily production cron is validated deterministically; UAT does not wait for the wall-clock 07:00 schedule.
- Phase 4 may consume the stable risk-event source metrics and CloudEvents contracts for AI recommendations without changing V1–V9 migrations.
