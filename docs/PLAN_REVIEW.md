# Plan Review

> Audit date: 2026-07-10  
> Scope: planning documents only; no implementation was reviewed.

## Executive Summary

- **Overall readiness score: 72/100**
- **Implementation recommendation: READY WITH FIXES**
- **Phase 0 verdict:** Repository scaffolding may start after the architecture baseline, package boundaries, and build shape are explicitly closed.
- **Domain implementation verdict:** Do not start domain, persistence, API, or AI implementation until the blocking contract inconsistencies below are reconciled.

The plan has a coherent business purpose, a suitable modular-monolith direction, strong deterministic-first AI safeguards, and a realistic separation between a lightweight hosted demo and an optional local full stack. The main risk is not missing product scope; it is that several documents treat proposed decisions as final while disagreeing on dependency direction, port ownership, event catalogs, idempotency storage, and migration ownership.

### Top 5 risks before coding

1. **Architecture baseline is not accepted:** all seven ADRs remain `Proposed`, although the README, PRD, roadmap, and deployment plan treat them as settled.
2. **Hexagonal boundaries are contradictory:** dependency arrows and port locations would allow application code to depend on infrastructure.
3. **Persistence contracts are incomplete:** `app_config`, critical indexes, and non-import idempotency have no complete migration/issue plan.
4. **API and data contracts differ at important seams:** AI recommendation fields, scheduled ownership, import deduplication, risk dismiss authorization, and movement quantity semantics need one definition.
5. **Free-tier feasibility is plausible but unproven:** Java/Spring Boot should fit, but combined dashboard, scheduler, JPA, and AI-adapter memory must be measured before deployment.

## Blocking Issues

| ID | Severity | File(s) | Issue | Recommended Fix |
|---|---|---|---|---|
| B-01 | Critical | `docs/ADR/ADR-001-use-java-spring-boot.md:3` and ADR-002 through ADR-007; `docs/PRD.md:219-220`; `README.md:103-104` | Every ADR is `Proposed`, while the rest of the plan treats Java/Spring, Render/Neon, outbox, BYOK, risk-first AI, CloudEvents, and Spring AI as approved decisions. | Review and change ADR statuses to `Accepted` before implementation, or explicitly label downstream documents as provisional. |
| B-02 | Critical | `docs/ARCHITECTURE.md:129-142` | The dependency diagram shows `Application --> Infrastructure`, contradicting hexagonal architecture and the stated ports/adapters principle. | Define dependencies as domain inward: API and infrastructure depend on application/domain; application depends on domain and port interfaces, never concrete adapters. |
| B-03 | Critical | `docs/ARCHITECTURE.md:138,159,423`; `docs/ADR/ADR-007-ai-framework-selection-spring-ai-vs-langchain4j.md:55` | Outgoing ports are alternately placed in `domain`, `application/port`, and `domain/ai`. | Put use-case ports and outbound gateway ports in each feature's `application.port`; keep domain services/value objects framework-free. Document one exception only if a port is genuinely a domain abstraction. |
| B-04 | High | `docs/DATA_MODEL.md:550-551`; `docs/ROADMAP.md:146-148`; `docs/GITHUB_ISSUES.md:91` | `V11__app_config.sql` and `V12__indexes.sql` exist in the migration plan but have no roadmap/issue owner. Phase 3 schedulers and outbox polling depend on them. | Add migration ownership before Phase 3. Prefer creating each table's critical indexes in the same migration as the table; create `app_config` before configurable risk rules. |
| B-05 | High | `docs/API_CONTRACT.md:471-473`; `docs/DATA_MODEL.md:221-223` | `Idempotency-Key` is required for imports, AI generation, and risk scans, but only import idempotency is modeled. Risk scan also returns a job ID without a job table. | For MVP, keep imports idempotent and make risk scan/brief generation synchronous without mandatory idempotency, or add a generic `idempotency_records` table and a `risk_scan_jobs` model. The simpler option is recommended. |
| B-06 | High | `docs/ROADMAP.md:64`; `docs/ARCHITECTURE.md:146-174`; `docs/GITHUB_ISSUES.md:43,50` | The roadmap says Gradle multi-module, while architecture specifies one package tree and one deployable. No subproject-to-boundary mapping exists. | Use one Gradle Spring Boot module for MVP, with package-enforced feature boundaries. Reconsider Gradle subprojects only after module coupling is measured. |
| B-07 | High | `docs/ADR/ADR-006-use-cloudevents-compatible-outbox-envelope.md:97-111`; `docs/ARCHITECTURE.md:453-461` | The CloudEvents catalogs differ; architecture omits product-updated, order-status-changed, PO-sent, and supplier-created events. | Make ADR-006 the canonical event catalog and reference it rather than duplicating a partial catalog in architecture. Implement only events required by current consumers. |
| B-08 | High | `docs/PRD.md:222`; `docs/API_CONTRACT.md:64-84` | Whether `OPERATOR` may create/update purchase orders remains open, but the API matrix already grants it. | Decide before security implementation. Recommended MVP policy: OPERATOR may create/edit DRAFT POs and receive goods; MANAGER/ADMIN may send, cancel, or override. |

## Non-Blocking Improvements

| ID | File(s) | Improvement | Why It Matters |
|---|---|---|---|
| N-01 | `docs/PRD.md:67`; `docs/ARCHITECTURE.md:290` | State that 8 seconds is the performance target and 15 seconds is the hard provider timeout; specify whether the single retry may exceed 15 seconds total. | Prevents contradictory SLO and timeout tests. |
| N-02 | `docs/PRD.md:80`; `docs/ARCHITECTURE.md:416`; `docs/DATA_MODEL.md:31` | Select BCrypt for MVP and remove “or Argon2,” or create an ADR for password hashing. | Keeps tests, seed users, and security configuration deterministic. |
| N-03 | `docs/ARCHITECTURE.md:168`; `docs/ADR/ADR-007-ai-framework-selection-spring-ai-vs-langchain4j.md:23` | Replace “spring-ai or langchain4j adapter” with Spring AI primary and LangChain4j as a possible future adapter. | Aligns architecture with ADR-007. |
| N-04 | `docs/ADR/ADR-003-use-byok-ai-provider.md:23` | Remove the Netlify Functions aside. | It is unrelated to the selected Render backend and creates deployment ambiguity. |
| N-05 | `docs/API_CONTRACT.md:387-397`; `docs/DATA_MODEL.md:174-190` | Document DTO mapping: `summary/actions/messageDrafts` map to generated columns; `inputRiskEventIds` comes from the join table; `generatedBy` is an explicit enum or derived field. | Makes API persistence mapping implementable without guessing. |
| N-06 | `docs/DATA_MODEL.md:181`; `docs/ARCHITECTURE.md:260` | Allow scheduled recommendations to use nullable `created_by` plus `created_by_type=SYSTEM`, or define a seeded system principal. | A scheduler cannot satisfy a non-null human actor without a convention. |
| N-07 | `docs/PRD.md:149`; `docs/ROADMAP.md:235`; `docs/DATA_MODEL.md:221-223` | Define import semantics as: idempotency key replays a request; file hash detects duplicate content within an organization/import type. Add an index for the latter. | Avoids both accidental duplicate ingestion and false global collisions. |
| N-08 | `docs/API_CONTRACT.md:130,416` | Remove AI-provider failure from the 503 row when a fallback brief is returned successfully. Reserve 503 for failure of both AI and fallback. | Produces one unambiguous client contract. |
| N-09 | `docs/API_CONTRACT.md:339-340`; `docs/DATA_MODEL.md:115` | Accept a positive movement magnitude in the API and derive the signed database delta from `movementType`; allow signed values only for `ADJUSTMENT` if needed. | Prevents double-negation and client mistakes. |
| N-10 | `docs/DATA_MODEL.md:351` and immutable table definitions | Correct the audit-column convention for `outbox_events`, `processed_events`, `import_row_errors`, and `ai_usage_audit`. | Migration authors otherwise add inconsistent columns. |
| N-11 | `docs/API_CONTRACT.md:79,222`; `docs/PRD.md:152`; `docs/GITHUB_ISSUES.md:125` | Add risk dismiss to RBAC, audit requirements, and frontend acceptance criteria. | Ensures a sensitive terminal action is consistently authorized and audited. |
| N-12 | `docs/API_CONTRACT.md:187-207`; `docs/PRD.md:152` | State that orders and POs are never deleted after creation; use CANCELLED. Remove “order delete” from audit wording. | Preserves operational history and resolves missing DELETE endpoints. |
| N-13 | `docs/ROADMAP.md:312-316`; `docs/GITHUB_ISSUES.md:191-195` | Make the “first issues” lists identical and add the missing `security` label to the taxonomy. | Prevents the first implementation sprint from starting with two competing orders. |
| N-14 | `docs/ARCHITECTURE.md:24-42,178-205` | Define cross-feature calls: use application services for synchronous workflows; use outbox events only for asynchronous side effects. | Prevents hidden cyclic coupling inside the modular monolith. |
| N-15 | `docs/DEPLOYMENT.md:267` | Verify the current Redpanda image/repository when local full-stack work begins. | The documented Vectorized image path may be stale. |

## Scope Cuts for MVP

| Feature | Keep / Defer | Reason |
|---|---|---|
| Modular monolith | Keep | Correct complexity level and one deployable artifact. |
| Auth, JWT, four roles | Keep | Required to demonstrate secure operational workflows. |
| Products, suppliers, orders, purchase orders, inventory movements | Keep | Minimum dataset needed for meaningful operational risks. |
| Six deterministic risk rules | Keep, but phase delivery | Core differentiator; implement stockout, order delay, and supplier delay first, then the remaining three. |
| PostgreSQL outbox | Keep | Demonstrates reliable event capture without hosted broker infrastructure. |
| CloudEvents-compatible envelope | Keep | Low incremental cost and preserves future transport portability. |
| AI daily brief with rule-based fallback | Keep | Portfolio AI value remains grounded in deterministic metrics. |
| JSON reports | Keep | Low-cost read models; PDF adds little MVP value. |
| CSV import | Keep after core workflows | Necessary for a usable demo but should not block core domain validation. |
| React dashboard and essential domain pages | Keep | Required for portfolio demonstration. |
| Kafka/Redpanda publishing | Defer | No hosted need; outbox contracts can be tested without a broker. |
| Prometheus/Grafana stack | Defer to local polish | Actuator and structured logs are enough for initial MVP verification. |
| Redis/Upstash | Defer unless profiling proves need | PostgreSQL queries plus short in-process cache should meet demo scale. |
| SSE/WebSockets | Defer | Polling is sufficient and Render Free constraints complicate persistent connections. |
| PDF export | Defer | Explicitly out of scope and not needed to prove business value. |
| Multi-tenancy | Defer | Adds authorization and data-isolation risk; retain only a documented migration path. |
| Admin users UI | Defer | Admin users can be seeded or managed through documented APIs for MVP. |
| Demo reset endpoint | Defer or protect as deployment-only | A public destructive endpoint is unnecessary; use an administrative seed/reset task. |
| Per-user stored AI keys | Defer | Environment-only BYOK is safer and sufficient for a single demo deployment. |
| GraalVM native image | Defer | Adds build complexity before JVM memory is measured. |

## Cross-Document Consistency Matrix

| Area | README | PRD | Architecture | Data Model | API | ADR | Status |
|---|---|---|---|---|---|---|---|
| Business goals | Stockout, dead stock, delay, supplier reliability, daily brief | Same goals and success metrics | Supports those workflows | Models required operational data | Exposes required workflows | Risk-first ADR supports goals | Consistent |
| Modular monolith | Explicit | Avoids premature microservices | Explicit, but dependency arrows conflict | Single database model | Single API surface | ADRs support one deployable | Fix boundaries |
| Hosted slim architecture | Cloudflare/Render/Neon | Hard free-tier constraint | No hosted broker/monitoring stack | PostgreSQL outbox | Polling/action APIs | ADR-002/004 align | Consistent |
| Local full stack | Optional Kafka/Redis/Prometheus/Grafana | Optional | Separate profile | Transport-neutral events | No hosted dependency | ADR-002/006 align | Consistent |
| Risk engine | Six deterministic rules | Canonical requirement | Pluggable registry and scheduler | `risk_events` modeled | Risk scan/status endpoints | ADR-005 aligns | Consistent |
| AI framework | Spring AI stated | ADR-007 resolves choice | One “Spring AI or LangChain4j” remnant | Provider/model metadata | Provider-neutral DTO | ADR-007 chooses Spring AI | Minor fix |
| AI fallback/safety | BYOK and fallback | Explicit | Explicit flow | Prompt/audit/recommendation tables | Fallback example | ADR-003/005/007 align | Consistent |
| Outbox/CloudEvents | Explicit | Required | Correct flow; partial catalog | Compatible envelope/table | Mostly internal | ADR-002/006 canonical | Fix catalog |
| Auth/RBAC | Four roles | Four roles | Security layer | User/role tables | Detailed matrix | No dedicated RBAC ADR | PO policy unresolved |
| Import idempotency | File hash/idempotency | Key or hash | Key flow | Only key unique | Key required | Not addressed | Fix semantics |
| Roadmap/issues | Summary phases | Defines scope | Defines technical flows | Migration sequence | Contract dependencies | Decisions feed phases | Missing V11/V12 ownership |
| Deployment/pricing | Dated verification | Manual verification required | Free-tier sizing | Retention supports limits | Health endpoint | ADR-004 has dated verification | Periodic manual check required |

## Free-Tier Feasibility Review

### Hosted slim demo

The hosted architecture is feasible only if it remains:

- Cloudflare Pages for the static React/Vite frontend.
- One stateless Spring Boot Docker service on a currently available free Java-compatible host.
- Neon Free PostgreSQL using a pooled runtime connection and direct migration connection.
- PostgreSQL outbox processed by an in-process Spring Scheduler.
- API polling for dashboard/import status; no required persistent connection.
- Environment-based BYOK AI, with a deterministic fallback when no key is present.

The hosted demo **does not require** Kafka, Redpanda, Prometheus, Grafana, Redis, a separate worker process, or a second application service. Testcontainers is a build/test dependency only and must not be present in the runtime image or production classpath.

Redis is correctly optional. It should remain disabled until a measured dashboard query misses the 1.5-second warm target. A bounded in-process cache is acceptable for derived dashboard data because the single hosted instance does not require cross-node coherence.

### Local full stack

Redpanda/Kafka, Redis, Prometheus, and Grafana belong only in the optional local `fullstack` profile. They demonstrate migration and observability skills but must not become MVP acceptance dependencies. The local compose file may be created later than minimal local development so Phase 0 stays small.

### Java/Spring Boot feasibility

Java 21 and Spring Boot 3.x are appropriate for the portfolio goals and domain. Spring Web MVC, Security, Data JPA, Flyway, Actuator, springdoc, and Spring AI are coherent choices. Keep runtime dependencies minimal:

- Do not add WebFlux alongside MVC.
- Add only one Spring AI provider starter in production.
- Keep Testcontainers and mocking libraries test-scoped.
- Avoid Redis, Kafka, and Prometheus registry dependencies in the initial hosted artifact unless their profiles are enabled and their footprint measured.
- Use one bounded HikariCP pool sized for the free database and single application instance.
- Measure memory with migrations complete and with dashboard, scheduler, outbox, and AI adapter active before claiming the `<512MB` target.

### Pricing status

The planning documents record dated verification for Cloudflare Pages, Render, Neon, and Upstash. Pricing and free-tier limits are external, unstable facts. Therefore every platform recommendation is **requires periodic manual verification**, at minimum before first deployment and quarterly afterward. If a free plan cannot be confirmed from the provider's official pricing/limits page, implementation must not assume it remains free.

## Data Model/API Alignment Review

### Strong alignment

- Product, supplier, order, order-item, purchase-order, purchase-order-item, inventory-movement, risk-event, AI-recommendation, import, audit, and outbox concepts map to API groups.
- Order and purchase-order statuses match across PRD, data model, and API.
- The inventory transaction design is sound: lock the product row, validate stock, update the balance, insert an immutable movement, audit the operation, and append the outbox event in one transaction.
- `risk_events.source_metrics JSONB` provides a suitable immutable evidence snapshot.
- The outbox payload includes the required CloudEvents 1.0 attributes and separates processing metadata from the event envelope.
- AI recommendations have prompt version, provider/model, generated content, source-risk links, status, feedback, and usage audit.
- Imports model job state, file hash, idempotency key, and row-level errors.
- List APIs define pagination, filtering, sorting, and a consistent error envelope.

### Required alignment fixes

1. Define the AI API-to-persistence mapping and add/derive `generatedBy`.
2. Resolve scheduled recommendation ownership (`created_by`).
3. Define import duplicate-content uniqueness as organization + import type + file hash, not a global hash.
4. Remove mandatory idempotency for synchronous risk scan/AI generation unless a generic persistence model is added.
5. Define movement quantity as a positive magnitude at the API boundary and signed delta internally.
6. Add risk dismiss to RBAC/audit/UI contracts.
7. State that orders and POs are cancelled, not deleted.
8. Assign critical indexes with the migrations that create their tables.
9. Pin entity-type casing, recommended as uppercase enum values in persistence and API (`PRODUCT`, `ORDER`, `SUPPLIER`, `PURCHASE_ORDER`).

## AI Safety Review

| Control | Assessment |
|---|---|
| Deterministic rules precede AI | Defined consistently in PRD, architecture, README, and ADR-005. |
| AI input grounding | Risk events and structured source metrics are the canonical input. |
| Source traceability | Recommendation-to-risk join table supports input risk IDs; API should expose the linked IDs. |
| Hallucination handling | Response parser rejects unknown risk IDs and falls back to the rule-based brief. |
| API-key protection | Environment-only BYOK; no persistence, frontend exposure, DTO field, or log output. |
| Provider failure | Timeout, limited retry, sanitized usage audit, and deterministic fallback are defined. |
| Prompt versioning | `prompt_versions` and recommendation `prompt_version` are sufficient. |
| Audit trail | `ai_usage_audit`, recommendation status, feedback, provider/model, and source-risk links are sufficient. |
| Human approval | APPROVED/REJECTED states and manager/admin actions are defined. Generated message drafts are not auto-sent. |
| Framework choice | Spring AI is the clear recommendation; the internal application port prevents framework leakage. |

AI safety is implementation-ready after the port location and DTO mapping are fixed. The plan correctly treats AI as a narrator and prioritizer, not a source of operational facts or an autonomous actor.

## Recommended Phase 0 Implementation Plan

The first ten tasks below are ordered to close planning blockers before creating a large code surface.

1. **Accept the architecture baseline.**  
   Acceptance: ADR-001 through ADR-007 have an explicit accepted/rejected status; Spring AI, hosted topology, outbox, and CloudEvents choices are no longer provisional.

2. **Reconcile architecture and contracts.**  
   Acceptance: one dependency direction, one port location, one event catalog, one PO RBAC policy, one movement-sign convention, and one idempotency policy are documented.

3. **Choose Gradle and one deployable module.**  
   Acceptance: Gradle Wrapper is pinned; Java toolchain is 21; the project produces one Spring Boot executable JAR; no application subprojects exist for MVP.

4. **Create the Spring Boot foundation.**  
   Acceptance: minimal MVC application starts; only Web, Validation, Actuator, and test foundation are initially included; a context-load test passes.

5. **Create feature-first package boundaries.**  
   Acceptance: packages follow the structure below; an architecture test (for example ArchUnit) prevents application/domain packages from depending on infrastructure/API.

6. **Define configuration profiles.**  
   Acceptance: `local`, `test`, `prod`, and `fullstack` configuration files exist; secrets are environment placeholders; default startup cannot accidentally use production credentials.

7. **Create the slim Docker build.**  
   Acceptance: multi-stage Java 21 build produces a non-root runtime image; Testcontainers and build tools are absent from the runtime layer; image boots with a memory limit.

8. **Create minimal local Compose.**  
   Acceptance: PostgreSQL and backend start with health checks; persistent local volume is configured; Kafka, Redis, Prometheus, and Grafana are absent.

9. **Add CI and quality gates.**  
   Acceptance: CI runs Gradle build, unit tests, architecture tests, formatting/static analysis, and dependency caching on Java 21.

10. **Create the persistence and HTTP baseline.**  
    Acceptance: Flyway creates auth/user/role and audit foundations plus required indexes; Actuator health returns 200; correlation ID and consistent error-envelope tests pass.

The optional full-stack Compose file, Kafka publisher, Redis cache, Prometheus/Grafana, AI provider starter, and frontend scaffolding should not be part of these first ten tasks.

### Build-tool recommendation

Use **Gradle with the Kotlin DSL**:

- Existing planning commands already use `./gradlew`.
- Java toolchains, Spring Boot packaging, dependency constraints, Testcontainers, and future frontend/Compose tasks are straightforward.
- Kotlin DSL gives typed build configuration without changing the application language.
- A single-module build is simpler and faster for a constrained modular monolith; package boundaries plus ArchUnit provide enough enforcement for MVP.

Maven would also work, but switching the documented commands and issue plan provides no project benefit.

## Recommended Initial Backend Structure

One Gradle module, one deployable JAR, feature-first packages:

```text
backend/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
└── src/
    ├── main/
    │   ├── java/com/opspulse/ai/
    │   │   ├── OpsPulseApplication
    │   │   ├── auth/
    │   │   │   ├── domain/
    │   │   │   ├── application/
    │   │   │   │   ├── port/in/
    │   │   │   │   └── port/out/
    │   │   │   ├── api/
    │   │   │   └── infrastructure/
    │   │   ├── user/
    │   │   ├── product/
    │   │   ├── supplier/
    │   │   ├── order/
    │   │   ├── inventory/
    │   │   ├── purchaseorder/
    │   │   ├── risk/
    │   │   ├── ai/
    │   │   ├── report/
    │   │   ├── importjob/
    │   │   ├── audit/
    │   │   ├── outbox/
    │   │   ├── dashboard/
    │   │   └── shared/
    │   │       ├── config/
    │   │       ├── security/
    │   │       ├── observability/
    │   │       ├── error/
    │   │       └── persistence/
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml
    │       ├── application-test.yml
    │       ├── application-prod.yml
    │       ├── application-fullstack.yml
    │       └── db/migration/
    └── test/
        └── java/com/opspulse/ai/
            ├── architecture/
            ├── unit/
            └── integration/
```

Each feature repeats `domain`, `application`, `api`, and `infrastructure` only when needed. Domain packages remain pure Java. Application ports own external dependencies. Infrastructure implements outbound ports. API calls inbound use cases. Cross-feature synchronous workflows call application interfaces; asynchronous side effects use outbox events.

## Final Recommendation

**Start implementation only after the eight blocking issues are resolved in the planning baseline.** Once closed, begin the ten-task Phase 0 plan above. The project does not require redesign: its business scope, modular-monolith strategy, deterministic risk engine, PostgreSQL outbox, BYOK AI safety model, and hosted/local separation are sound.

Do not begin broad CRUD generation, JPA entity generation, or AI integration against the current contradictory contracts. A short planning correction pass should move the project from **READY WITH FIXES (72/100)** to **READY** without expanding MVP scope.
