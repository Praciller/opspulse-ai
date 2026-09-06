# Phase 7 Local Hardening UAT

Status: local checks implemented and verified on 2026-07-18. Hosted deployment
and hosted evidence remain intentionally gated by external credentials and
platform accounts.

## Local verification matrix

| Area | Evidence | Status |
|---|---|---|
| Backend regression | `gradlew clean test --no-daemon` | PASS |
| Coverage gate | `gradlew check --no-daemon`; JaCoCo risk/AI/CloudEvents bundle >= 80% | PASS |
| Frontend quality | `npm ci`, `npm run lint`, `npm run test`, `npm run build` | PASS |
| Mock browser flow | `VITE_USE_MOCKS=true npm run test:e2e` | PASS; real API spec skipped without credentials |
| Docker config | `docker compose config --quiet`; `docker compose -f docker-compose.full.yml config --quiet` | PASS |
| Optional observability | Prometheus scrape config and Grafana dashboard provisioned under `infra/` | READY |
| Security automation | Dependabot, npm audit, CodeQL, dependency review in CI | CONFIGURED |
| Source hygiene | `git diff --check`; no secret values added | PASS |

## Optional full stack

```powershell
$env:JWT_SECRET='phase7-local-placeholder-0123456789'
docker compose -f docker-compose.full.yml up --build
```

The local `fullstack` profile permits Prometheus scraping. The default and
hosted profiles keep `/actuator/prometheus` ADMIN-protected. Grafana is
available on port `3001`, Prometheus on `9090`, and the backend on `8081` by
default to avoid colliding with the minimal compose stack. Redpanda and Redis
are available for local infrastructure experiments; the hosted outbox remains
the durable in-process delivery boundary until a later Kafka integration.

## Smoke test

The environment-gated API smoke is available as:

```powershell
$env:SMOKE_BASE_URL='https://<backend-host>'
$env:SMOKE_EMAIL='demo@example.invalid'
$env:SMOKE_PASSWORD='<provided-out-of-band>'
.\scripts\smoke-test.ps1
```

It checks health, login, dashboard, products, risks, brief generation, imports,
and all five reports without printing tokens or response payloads. It exits `2`
when the three required variables are absent.

## External handoff

- Render/Neon/Cloudflare provisioning and hosted migration verification are
  pending platform access.
- Hosted URLs, production CORS values, cold-start timing, RSS, and real API
  smoke evidence must be recorded after deployment.
- Screenshots/GIFs, demo credentials, and portfolio publication require
  user-provided hosted assets; no credentials or private values are stored here.
