# Plan Fixes

> Applied: 2026-07-10  
> Source review: [PLAN_REVIEW.md](PLAN_REVIEW.md)  
> Scope: planning Markdown only; no implementation artifacts were created.

## Blocking Issue Resolution

| ID | Affected files | Fix applied | Status | Reason if not fully resolved |
|---|---|---|---|---|
| B-01 | `docs/ADR/ADR-001-*.md` through `ADR-007-*.md` | Changed all seven architecture decisions from `Proposed` to `Accepted`. PRD now lists the framework, deployment, SSE, and PO authorization outcomes as resolved decisions. | RESOLVED | — |
| B-02 | `README.md`, `docs/ARCHITECTURE.md` | Corrected dependency direction: API and infrastructure depend on application; application depends on domain. Runtime diagrams now show the application use case—not the domain—calling the AI adapter. Added an explicit rule forbidding application imports from infrastructure/API. | RESOLVED | — |
| B-03 | `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/ADR/ADR-007-ai-framework-selection-spring-ai-vs-langchain4j.md`, `docs/GITHUB_ISSUES.md` | Standardized inbound/outbound ports under each feature's application layer. Moved `OperationsBriefClient` conceptually to `ai.application.port.out`; domain retains only pure business types. | RESOLVED | — |
| B-04 | `docs/DATA_MODEL.md`, `docs/ROADMAP.md`, `docs/GITHUB_ISSUES.md` | Assigned `V11__app_config.sql` to Phase 3 and issue P3-1A. Required critical indexes in each owning table migration and reserved V12 for supplemental measured indexes. Updated issue acceptance criteria for auth/audit, risk, and outbox indexes. | RESOLVED | — |
| B-05 | `docs/API_CONTRACT.md` | Limited mandatory `Idempotency-Key` to CSV imports. Defined risk scan as a synchronous `200 OK` operation and retained synchronous AI generation, avoiding unmodeled job/idempotency tables. | RESOLVED | — |
| B-06 | `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/GITHUB_ISSUES.md`, `docs/ADR/ADR-001-use-java-spring-boot.md` | Selected Gradle Kotlin DSL with one Spring Boot deployable module and feature-first package boundaries enforced by architecture tests. Removed the remaining Maven alternative. Removed local full-stack Compose from Phase 0; it remains optional in Phase 7. | RESOLVED | — |
| B-07 | `docs/ARCHITECTURE.md`, `docs/ADR/ADR-006-use-cloudevents-compatible-outbox-envelope.md` | Made ADR-006 the canonical event catalog and removed the duplicate partial catalog from architecture. CloudEvents 1.0 remains the required envelope. | RESOLVED | — |
| B-08 | `docs/PRD.md`, `docs/API_CONTRACT.md` | Defined PO authorization: OPERATOR may create/edit DRAFT POs and receive goods; MANAGER/ADMIN control post-draft status transitions. Orders and POs use cancellation rather than deletion. | RESOLVED | — |

## Additional Consistency Fixes

The following non-blocking items from `PLAN_REVIEW.md` were fixed because they touched the same contracts:

| Review ID | Fix |
|---|---|
| N-01 | Clarified `<8s p95` as the AI latency target and 15s as the per-attempt hard timeout. |
| N-02 | Selected BCrypt consistently for MVP password hashing. |
| N-03 | Removed the remaining Spring AI/LangChain4j ambiguity; Spring AI is primary. |
| N-04 | Removed the unrelated Netlify Functions aside from the BYOK ADR. |
| N-05 | Documented AI DTO-to-table mapping and added explicit `generated_by`. |
| N-06 | Added scheduled/system recommendation actor semantics with nullable `created_by` and `created_by_type`. |
| N-07 | Separated import request replay from duplicate-content detection and added the scoped unique index. |
| N-08 | Reserved HTTP 503 for failure of both a primary operation and its defined fallback. |
| N-09 | Defined positive API movement magnitudes and internally derived signed deltas. |
| N-10 | Corrected immutable-table provenance column conventions. |
| N-11 | Added risk dismiss to RBAC and audit requirements. |
| N-12 | Confirmed orders and POs are cancelled rather than deleted. |
| N-13 | Aligned the first Phase 0 issue order and added missing security/observability labels. |
| N-14 | Defined synchronous cross-feature calls through inbound application ports and asynchronous side effects through outbox events. |

## Remaining Non-Blocking Improvements

| Review ID | Status | Reason |
|---|---|---|
| N-15 | DEFERRED | Verify the current Redpanda container image when optional local full-stack work begins in Phase 7. It does not affect hosted MVP or Phase 0. |
| Hosted demo seed task | DEFERRED | The deployment plan already assigns production demo seeding to Phase 7. Its detailed command/task contract should be finalized with deployment implementation, not by adding an MVP API. |
| Memory target validation | DEFERRED | `<512MB` remains a measurable deployment criterion. It cannot be proven from planning documents and must be tested before hosted deployment. |
| Free-tier pricing validation | DEFERRED | Provider pricing is external and requires manual verification before deployment and quarterly thereafter. |

## Readiness After Fixes

- Architecture decisions: accepted.
- Architecture style: one-deployable modular monolith with feature-first hexagonal boundaries.
- Hosted deployment: slim; no required Kafka, Redpanda, Prometheus, Grafana, Redis, or separate worker.
- Local full stack: optional Phase 7 scope only.
- AI: deterministic risk engine first; Spring AI is an adapter; rule-based fallback and source-risk traceability remain mandatory.
- Events: PostgreSQL outbox with CloudEvents-compatible payloads.
- Build direction: Gradle Kotlin DSL, single application module.

**Result: READY for Phase 0 implementation.** This status authorizes scaffolding only; it does not start implementation and does not remove the need to validate memory and provider free tiers before deployment.
