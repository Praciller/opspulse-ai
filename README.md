# OpsPulse-AI

> **Java/Spring Boot operations intelligence platform for SMEs** — detect stockout, overstock, order delay, and supplier reliability risks, then generate an AI-assisted daily action plan that reduces manual Excel triage.

[![CI](https://github.com/Praciller/opspulse-ai/actions/workflows/ci.yml/badge.svg)](https://github.com/Praciller/opspulse-ai/actions/workflows/ci.yml) [![Java](https://img.shields.io/badge/Java-21-orange)](#) [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)](#) [![License](https://img.shields.io/badge/license-MIT-blue)](#)

---

## Phase 5/6 operations console

The local `frontend/` application is implemented with React, TypeScript,
Vite, and Tailwind. Real backend APIs are the default; deterministic fixtures
are opt-in with `VITE_USE_MOCKS=true`. It includes protected role-aware routes,
inventory and purchasing views, risk lifecycle actions, AI brief generation and
feedback, import upload/status/error workflows, five JSON reports, Cloudflare
Pages SPA routing, unit/component tests, Playwright mock flow, and axe
accessibility coverage.
See [`frontend/README.md`](frontend/README.md) for setup and the optional
real-API smoke variables.

## Why this project exists

Small and medium enterprises lose revenue every day to four avoidable problems:

1. **Stockouts** — items run dry because no one re-ordered in time.
2. **Dead stock** — capital trapped in slow-moving SKUs nobody notices.
3. **Order delays** — orders slip without clear prioritization.
4. **Untracked supplier reliability** — late suppliers keep getting orders.

Managers typically start their day in Excel, manually cross-checking inventory, open orders, and supplier emails. **OpsPulse-AI replaces that 60-minute spreadsheet ritual with a 60-second daily brief** — and the AI never invents the numbers; it narrates and prioritizes risks that a deterministic engine has already computed and audited.

This project is also a **portfolio artifact** demonstrating end-to-end engineering: Java backend, data engineering, AI integration with safety rails, system design, free-tier deployment, and observability.

## Business impact

| Outcome | How OpsPulse-AI helps |
|---|---|
| Reduce stockout incidents | `STOCKOUT_RISK` rule surfaces SKUs whose current stock < avg daily sales × supplier lead time, daily. |
| Reduce dead stock | `SLOW_MOVING_INVENTORY` + `OVERSTOCK_RISK` rules flag SKUs idle for N days or over a multiple of 30-day demand. |
| Reduce order delays | `ORDER_DELAY_RISK` rule ranks at-risk orders by severity so operators act on the right order first. |
| Improve supplier follow-up | `SUPPLIER_DELAY_RISK` ranks suppliers by late-delivery rate; AI drafts follow-up messages. |
| Replace manual triage | A daily AI brief with executive summary + top 5 risks + recommended actions + message drafts. |
| Demonstrable engineering | Modular monolith, hexagonal architecture, PostgreSQL outbox + CloudEvents, BYOK AI with rule-based fallback, free-tier hosted demo. |

## Implementation status

- **Phases 0–6 — foundation through imports/reports:** implemented and verified (145 backend tests with Testcontainers, architecture tests, frontend lint/unit/build, Playwright + axe coverage). Source is public on GitHub with CI enforcing build, tests, Docker image size, npm audit, and dependency review.
- **Phase 7 — hosted hardening:** CI, JaCoCo coverage, security checks, optional full-stack observability profile, jlink slim image (108 MB, gated at 250 MB in CI), and smoke tooling are implemented and verified.
- **Phase 8 — portfolio completion:** documentation and handoff are published; the hosted slim demo (Render + Neon + Cloudflare Pages) is the remaining external step, tracked in [docs/PHASE8_HANDOFF.md](docs/PHASE8_HANDOFF.md).

Phase 2 adds bounded product, supplier, customer-order, purchase-order, and
inventory-movement APIs. Stock is changed only through immutable movement rows.
Each mutation locks the product row, computes the balance under PostgreSQL
`REPEATABLE READ`, and commits the product, movement, audit row, and pending
CloudEvents-compatible outbox row atomically. Serialization failures are retried
up to three times for standalone inventory requests. Negative stock is disabled
by default and may be enabled only through `ALLOW_NEGATIVE_STOCK=true` until a
later administration UI exists.

## MVP features

- **Auth & RBAC** — JWT, refresh rotation, 4 roles (`ADMIN`, `MANAGER`, `OPERATOR`, `VIEWER`).
- **Products, Suppliers, Orders, Purchase Orders, Inventory Movements** — full CRUD with audit log.
- **Transactional inventory** — stock updates, immutable movements, audit, and outbox event in one transaction.
- **Deterministic risk engine** — 6 risk types (`STOCKOUT_RISK`, `OVERSTOCK_RISK`, `SLOW_MOVING_INVENTORY`, `ORDER_DELAY_RISK`, `SUPPLIER_DELAY_RISK`, `LOW_MARGIN_RISK`), pluggable rules, scheduled scan, active-event deduplication, and system resolution.
- **AI daily operations brief** — provider-agnostic BYOK (OpenAI, Anthropic, Ollama), prompt versioning, audit trail, **rule-based fallback** when AI is unavailable or hallucinates.
- **Dashboard** — total products, open orders, delayed orders, high-risk products, open risks, supplier reliability ranking, top stockout risks, slow-moving list, recent movements, recent risks, latest brief.
- **Reports** — Daily Ops Brief, Inventory Risk, Supplier SLA, Order Delay, Product Margin (JSON-first).
- **CSV import** — idempotent, row-level errors, file-hash dedup, job status tracking.
- **Audit log** — every sensitive action logged with actor, before/after snapshot, request ID.
- **PostgreSQL outbox** — CloudEvents 1.0 envelope, scheduler poller, idempotent processed-event tracking, retry/dead-letter handling, and future Kafka migration without contract change.

## Architecture at a glance

```mermaid
flowchart TB
    subgraph Frontend[Frontend - Cloudflare Pages]
        React[React + Vite + TS]
    end
    subgraph Backend[Backend - Render Free - Spring Boot 3 / Java 21]
        API[REST + OpenAPI]
        Sec[Security + JWT + RBAC]
        UC[Application Use Cases]
        Dom[Domain + Risk Engine]
        AI[AI Adapter - BYOK]
        OB[Outbox Processor - Scheduler]
        Sch[Risk + Brief Schedulers]
        Imp[CSV Import]
        Aud[Audit]
    end
    subgraph Data[Data Layer]
        Neon[(Neon Free Postgres)]
        Upstash[(Upstash Redis - optional)]
    end
    subgraph ExtAI[External AI - BYOK]
        Provider[OpenAI / Anthropic / Ollama]
    end
    React -->|HTTPS + JWT| API
    API --> Sec --> UC --> Dom
    UC --> Neon
    UC --> AI --> Provider
    UC --> OB --> Neon
    Sch --> UC
    Imp --> UC
    Aud --> Neon
    UC -.optional.-> Upstash
```

### Container diagram (C4)

```mermaid
flowchart LR
    User[Operations Manager]
    Web[OpsPulse Web UI - React on Cloudflare Pages]
    API[OpsPulse API - Spring Boot on Render]
    DB[(Neon Postgres)]
    Cache[(Upstash Redis - optional)]
    AI[AI Provider - BYOK]
    User -->|uses| Web
    Web -->|REST/JWT| API
    API -->|JPA/Flyway| DB
    API -.cache.-> Cache
    API -->|BYOK key from env| AI
```

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.x, Spring Web MVC, Spring Security + JWT, Spring Data JPA, Flyway, Spring Scheduler, Spring Boot Actuator, springdoc-openapi |
| AI | Spring AI 1.0 (provider-agnostic), BYOK env-var key, rule-based fallback |
| Database | PostgreSQL (Neon Free), Flyway migrations, JSONB for `source_metrics` and outbox `payload` |
| Cache | Caffeine (default) or Upstash Redis (optional) |
| Tests | JUnit 5, Mockito, Testcontainers, JaCoCo |
| Observability | Actuator + Micrometer + Prometheus + Grafana (local full stack) |
| Frontend | React + Vite + TypeScript + Tailwind (or clean CSS) |
| Deployment | Docker (multi-stage JRE 21 slim), Cloudflare Pages, Render Free, Neon Free |
| Events | PostgreSQL outbox + CloudEvents 1.0 envelope; optional Kafka/Redpanda in local full stack |

## Target Hosted Slim vs Local Full Stack

| Aspect | Hosted Slim (free demo) | Local Full Stack (dev/observability) |
|---|---|---|
| Frontend | Cloudflare Pages | Vite dev server |
| Backend | Render Free Web Service (Docker) | Spring Boot in Docker Compose |
| Database | Neon Free Postgres | PostgreSQL in Docker Compose |
| Cache | Caffeine (or optional Upstash Redis) | Redis in Docker Compose |
| Event bus | PostgreSQL outbox (in-process consumers) | Outbox + Kafka/Redpanda publishing |
| Metrics | Actuator + Micrometer | Prometheus + Grafana dashboards |
| Cost | $0/month (verified 2026-07-13) | Local machine only |
| Compose file | `docker-compose.yml` (profile `local`) | `docker-compose.full.yml` (profile `fullstack`) |

See [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) for full topology, env vars, and platform validation.

## Demo links

> Placeholder — populated after Phase 7 deploy.

- **Frontend (Cloudflare Pages):** `https://opspulse-ai.pages.dev` _(TBD)_
- **Backend API (Render):** `https://opspulse-ai.onrender.com` _(TBD)_
- **Swagger UI:** `https://opspulse-ai.onrender.com/swagger-ui.html` _(TBD)_
- **Demo credentials:** _(TBD — documented after deploy; demo-only, no real data)_

## Quick start (local minimal)

Prerequisites: Docker Desktop and Git. Java 21 is required only when running Gradle directly on the host.

```bash
git clone https://github.com/Praciller/opspulse-ai.git
cd opspulse-ai
# Copy .env.example to .env and set JWT_SECRET to a random 32+ character value.
docker compose up --build -d
docker compose ps

# Backend health
curl http://localhost:8080/actuator/health

# Foundation metadata and API documentation
curl http://localhost:8080/actuator/info
curl http://localhost:8080/v3/api-docs
# Swagger UI: http://localhost:8080/swagger-ui.html

# Supply a correlation ID; it is returned in X-Request-Id and error responses
curl -H "X-Request-Id: local-check-1" http://localhost:8080/actuator/health

# Stop services without deleting the PostgreSQL volume
docker compose down
```

The local profile applies the identity migrations and seeds five demo-only users.
All use password `OpsPulseDemo!2026`: `admin@opspulse.demo`,
`manager@opspulse.demo`, `operator1@opspulse.demo`,
`operator2@opspulse.demo`, and `viewer@opspulse.demo`. Production never loads
this seed location. Registration defaults to `ADMIN_ONLY`; a self-hosted
environment may explicitly set `REGISTRATION_MODE=PUBLIC_VIEWER`, which can
create only `VIEWER` users.

Runtime configuration:

| Variable | Required/default | Purpose |
|---|---|---|
| `JWT_SECRET` | Required; at least 32 characters | Signs access and refresh JWTs. Never commit a real value. |
| `JWT_ACCESS_TOKEN_TTL_SECONDS` | `900` | Access-token lifetime. |
| `JWT_REFRESH_TOKEN_TTL_SECONDS` | `604800` | Refresh-token lifetime. |
| `REGISTRATION_MODE` | `ADMIN_ONLY` | Set `PUBLIC_VIEWER` only when public viewer registration is intentional. |
| `BCRYPT_STRENGTH` | `12` | BCrypt work factor. Tests override this for speed. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` locally | Comma-separated trusted frontend origins. |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Local defaults are in `application-local.yml` | Host-run local PostgreSQL connection. Docker Compose derives these from `POSTGRES_*`. |
| `ALLOW_NEGATIVE_STOCK` | `false` | Allows an inventory movement to produce a negative balance. Keep disabled unless the business explicitly approves backorders. |

`/actuator/health`, `/actuator/info`, OpenAPI, and Swagger UI are public. The
Prometheus scrape endpoint at `/actuator/prometheus` requires an `ADMIN` bearer
token.

To run the build directly with a local Java 21 installation:

```bash
./gradlew clean test bootJar
```

## Local full stack (optional — Phase 7)

`docker-compose.full.yml` provides an optional local profile with PostgreSQL,
Redpanda, Redis, Prometheus, and Grafana. Start it with
`docker compose -f docker-compose.full.yml --profile fullstack up --build`.
The current outbox delivery path remains durable in-process; Redpanda is
provisioned for future transport integration. The `fullstack` Spring profile
enables local Prometheus scraping without changing the default protected
actuator contract.

## Deployment overview

OpsPulse-AI targets **free-tier-only** services. Availability and limits were verified on 2026-07-13 but require periodic manual verification before deployment because provider pricing can change:

| Layer | Service | Verified limits |
|---|---|---|
| Frontend | Cloudflare Pages | Unlimited bandwidth/sites; 500 builds/month |
| Backend | Render Free Web Service | 750h/workspace/month; sleeps after 15 min idle |
| Database | Neon Free Postgres | 100 CU-hours/project/month; 0.5 GB storage; scales to zero |
| Cache (optional) | Upstash Redis Free | 500K commands/month; 256 MB data |

Full deploy steps, env vars, and the **platform validation checklist** (must pass before each deploy) live in [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## Screenshots

> Placeholder — populated in Phase 8.

| Page | Screenshot |
|---|---|
| Login | _(TBD)_ |
| Dashboard | _(TBD)_ |
| Risk events | _(TBD)_ |
| AI daily brief | _(TBD)_ |
| CSV import | _(TBD)_ |

## Sample data

The repeatable migration (`R__seed_demo_data.sql`) seeds five local-only
users: 1 admin, 1 manager, 2 operators, and 1 viewer. Their shared demo password
is documented in the local quick start above. Phase 2 deliberately adds no
business seed rows; products, suppliers, orders, purchase orders, and inventory
movements are created through their APIs. Phase 3 integration tests use isolated
transaction-scoped business fixtures; the local seed remains user-only.

## API docs

- **Swagger UI (hosted):** `https://opspulse-ai.onrender.com/swagger-ui.html` _(TBD)_
- **OpenAPI spec (hosted):** `https://opspulse-ai.onrender.com/v3/api-docs` _(TBD)_
- **API contract plan:** [docs/API_CONTRACT.md](docs/API_CONTRACT.md)
- **Phase 7 local UAT:** [docs/PHASE7_UAT.md](docs/PHASE7_UAT.md)
- **Phase 8 hosted handoff:** [docs/PHASE8_HANDOFF.md](docs/PHASE8_HANDOFF.md)

Implemented through Phase 6 on the backend, with the Phase 5/6 console in
`frontend/`: `/api/auth`, `/api/audit-logs`, `/api/products`,
`/api/suppliers`, `/api/orders`, `/api/purchase-orders`,
`/api/inventory-movements`, `/api/risks`, `/api/admin/outbox-events`,
`/api/ai/recommendations`, `/api/imports`, `/api/reports/*`,
`/actuator/health`, `/actuator/info`, and `/actuator/prometheus`. List APIs are paginated and support the filters and sort
fields defined by the service. All authenticated roles may read Phase 2 data;
`ADMIN` and `OPERATOR` perform operational writes, only `ADMIN` deactivates
products/suppliers, and `ADMIN`/`MANAGER` perform post-draft PO transitions.

Phase 2 verification commands:

```bash
./gradlew clean test --no-daemon
./gradlew clean build --no-daemon
./gradlew check --no-daemon
docker compose config --quiet
```

Phase 3 adds deterministic rule and delivery verification through the same
Gradle test suite, including Testcontainers coverage for risk scans, lifecycle
deduplication, CloudEvents idempotency, retry/dead-letter handling, and RBAC.

Flyway applies `V3` through `V11` after the Phase 1 migrations. The hosted
outbox processor dispatches supported CloudEvents through an in-process durable
acknowledgement boundary; external publication remains deferred.

Phase 4 exposes synchronous brief generation. The Spring AI OpenAI-compatible
adapter is enabled only when `AI_API_KEY` is configured; otherwise (or after a
provider failure, timeout, malformed response, or hallucinated risk reference)
the deterministic `RULE_BASED` brief is persisted and returned with `200 OK`.
Prompts, credentials, raw provider payloads, and stack traces are never returned
or stored.

Phase 6 adds CSV import jobs for products, suppliers, orders, inventory, and
purchase orders. Jobs are bounded to 10 MiB/10,000 rows, replayed by
`Idempotency-Key`, deduplicated by SHA-256 content hash, and expose sanitized
row errors. Reports are read-only JSON aggregates with Micrometer request and
latency metrics. The real-API frontend smoke remains gated by
`E2E_API_BASE_URL`, `E2E_EMAIL`, and `E2E_PASSWORD`.

Phase 7 adds JaCoCo coverage enforcement for risk/AI/CloudEvents code, GitHub
Actions frontend/backend/security jobs, an optional local full stack in
`docker-compose.full.yml`, Prometheus/Grafana dashboard provisioning, and the
`scripts/smoke-test.ps1` health/login/operations check. The full stack exposes
Prometheus locally only; hosted metrics remain protected.

## Known limitations

- **Render cold starts (about 60s)** after 15 min idle — accepted for the free demo and explained in the loading state.
- **Neon 0.5 GB storage** — demo dataset ~100 MB; cleanup jobs prune `outbox_events`, `processed_events`, `import_row_errors`.
- **AI brief requires BYOK** — the public hosted demo shows the **rule-based fallback brief** unless the viewer sets `AI_API_KEY` in their own instance.
- **No PDF export** — reports are JSON-first (out of scope per PRD §6).
- **Single-tenant for MVP** — schema has `organization_id` placeholder for future multi-tenant.
- **No live push in the MVP** — outbox polling is sufficient; SSE/WebSocket delivery is deferred.
- **Single backend instance on Render free** — no HA; durability lives in Neon.

## Future improvements

- Multi-tenant onboarding with `organizations` table + per-tenant isolation.
- PDF report export.
- SSE / WebSocket live dashboard driven by outbox tail.
- Kafka in production (contract already CloudEvents-ready; outbox publishing is the only addition).
- AI-assisted supplier message A/B testing with feedback loop.
- Native image (GraalVM) for sub-second cold starts on free tier.
- Mobile app for operators (warehouse scanning).
- Per-user AI key stored encrypted (multi-tenant only).

## Roadmap

Eight phases from scaffolding to portfolio polish. Full breakdown with deliverables, exit criteria, and issue mapping: [docs/ROADMAP.md](docs/ROADMAP.md).

```mermaid
gantt
    title OpsPulse-AI Implementation Phases
    dateFormat  YYYY-MM-DD
    axisFormat  %b
    section Foundation
    Phase 0 Scaffolding        :p0, 2026-07-15, 5d
    Phase 1 Backend Foundation :p1, after p0, 10d
    section Domain
    Phase 2 Core CRUD          :p2, after p1, 12d
    Phase 3 Risk Engine        :p3, after p2, 9d
    section AI + UI
    Phase 4 AI Recommendations :p4, after p3, 8d
    Phase 5 Frontend           :p5, after p2, 14d
    section Wrap
    Phase 6 Import + Reports   :p6, after p4, 7d
    Phase 7 Tests Obs Deploy   :p7, after p6, 8d
    Phase 8 Portfolio Polish   :p8, after p7, 5d
```

## Documentation index

| Document | Purpose |
|---|---|
| [docs/PRD.md](docs/PRD.md) | Product requirements — users, goals, FR/NFR, out-of-scope, success metrics |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Module boundaries, request flow, risk flow, AI flow, outbox flow, Mermaid diagrams |
| [docs/DATA_MODEL.md](docs/DATA_MODEL.md) | Entity list, table designs, indexes, ER diagram, Flyway strategy |
| [docs/API_CONTRACT.md](docs/API_CONTRACT.md) | Endpoint inventory, RBAC matrix, examples, error envelope, OpenAPI |
| [docs/ADR/](docs/ADR/) | 7 Architecture Decision Records (Spring Boot, Outbox, BYOK, Free-tier, Risk-first, CloudEvents, Spring AI) |
| [docs/ROADMAP.md](docs/ROADMAP.md) | 8-phase implementation plan with exit criteria |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | Cloudflare + Render + Neon + Upstash; env vars; platform validation checklist |
| [docs/RISK_REGISTER.md](docs/RISK_REGISTER.md) | 15 technical & product risks with mitigations and review cadence |
| [docs/GITHUB_ISSUES.md](docs/GITHUB_ISSUES.md) | ~55 suggested issues with labels, milestones, acceptance criteria |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Local prerequisites, verification commands, and project contribution rules |

## License

MIT — see [LICENSE](LICENSE).

## Author

Built as a portfolio project by Pakon Poomson — Java backend, data engineering, AI integration, and system design.
