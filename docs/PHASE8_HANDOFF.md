# Phase 8 Portfolio Handoff

Phase 7 local hardening is complete. Phase 8 is ready for the external hosted
verification step; no provider credentials, hosted URLs, screenshots, commits,
or pushes are stored in this repository.

## Before publishing

- Provision the Render, Neon, and Cloudflare Pages projects using
  [DEPLOYMENT.md](DEPLOYMENT.md), then re-run its free-tier validation table.
- Set production-only secrets in the provider consoles. Never place
  `JWT_SECRET`, `AI_API_KEY`, database passwords, or refresh tokens in Git.
- Run Flyway migrations and the transaction-scoped seed fixture against the
  hosted database; confirm the migration history ends at V11.
- Set `CORS_ALLOWED_ORIGINS` to the published Pages URL and keep
  `/actuator/prometheus` protected outside the local `fullstack` profile.

## Hosted evidence checklist

- [ ] `scripts/smoke-test.ps1` passes for health, login, dashboard, products,
  risks, brief, imports, and all five reports.
- [ ] Cold-start timing is recorded after an idle period and the frontend
  waking-up/retry state is visible.
- [ ] README receives the final API, frontend, and hosted URLs.
- [ ] Add at least three redacted screenshots or a short GIF for login,
  dashboard/risk operations, and the AI/import/report workflows.
- [ ] Document demo credentials out-of-band or use a seeded account with a
  rotated password; never commit a password.
- [ ] Link the relevant ADRs and this handoff from the portfolio write-up.

## Hosted versus local

The local default uses the in-process PostgreSQL outbox and deterministic
fallback AI. The optional `docker-compose.full.yml` adds Redpanda, Redis,
Prometheus, and Grafana for infrastructure experiments, while the hosted
contract remains the same. Real AI provider use is BYOK and is not required
for a successful brief response.

## Explicitly gated

The Playwright real-API flow remains skipped until `E2E_API_BASE_URL`,
`E2E_EMAIL`, and `E2E_PASSWORD` are supplied. Deployment, hosted URLs,
screenshots, publication, commit, and push require the owner’s external
accounts and approval.
