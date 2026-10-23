# OpsPulse-AI — Current deployment (verified 2026-10-23)

This is the source of truth for the **current hosting architecture**. The September 2026 [deployment guide](DEPLOYMENT.md) and [Phase 8 handoff](PHASE8_HANDOFF.md) are retained as historical evidence; Cloudflare Pages is **not the selected hosting provider** for new releases.

## Production architecture

| Layer | Service | Address / configuration |
| --- | --- | --- |
| Frontend | Vercel Hobby, React + TypeScript + Vite | https://opspulse-ai-gamma.vercel.app |
| Backend | Render Free, Java 21 + Spring Boot | https://opspulse-ai-7gle.onrender.com |
| Database | Neon PostgreSQL (provider-managed) | Credentials remain in hosting secret stores |
| Source and CI | GitHub | https://github.com/Praciller/opspulse-ai |

Vercel project: `prj_Sfp03pCv1XJXVGJJeQpfthLddlv5`, Git repository `Praciller/opspulse-ai`, production branch `main`, root directory `frontend`, Node.js `24.x`, framework `vite`, install `npm ci`, build `npm run build`, output `dist`.

Build-time configuration for both preview and production:
- `VITE_API_BASE_URL=https://opspulse-ai-7gle.onrender.com`
- `VITE_USE_MOCKS=false` (the hosted demo must use real API requests)

`frontend/vercel.json` rewrites deep links to `/index.html` for React Router. Preview deployments retain Vercel Authentication; production is public and application users authenticate through OpsPulse itself.

## Release verification

- [PR #27](https://github.com/Praciller/opspulse-ai/pull/27) merged to `main` as `06b60f5dcf1c8e95d37ed78eeb44e6b98abece7a`; seven GitHub checks passed on the PR.
- Vercel **automatically** built that `main` commit as production deployment `dpl_28ubbDdviHDJiHVvbdaEHRKjLmeb`, state `READY`. This verifies that the Vercel GitHub App now has repository access and Git integration is active.
- Prior to that merge, an authenticated browser smoke against the same frontend code on the Vercel production alias passed: login with least-privilege synthetic VIEWER, navigation to `/dashboard`, and zero browser JavaScript errors. Re-check after changes.
- Render backend health returned HTTP 200; its 2026-10-22 startup took approximately 217–235 seconds on the free instance. Cold starts can exceed typical client timeouts.
- The Render CORS configuration was updated with `replace=false` to allow the production Vercel origin while temporarily preserving the September legacy origin, and the deployed backend returned HTTP 200 to the Vercel login OPTIONS preflight with matching `Access-Control-Allow-Origin`. Render automatically deploys every `main` commit, including frontend-only/docs-only commits.
- The Render auto-deployment for PR #27 merged SHA `06b60f5` completed as `live` (`dep-dbd6k20473hc73aen230`). A post-merge check confirmed Vercel `/`, `/login`, and `/dashboard` HTTP 200; Render `/actuator/health` HTTP 200; and Vercel-origin login OPTIONS HTTP 200 with a matching `Access-Control-Allow-Origin`. No database migration was part of PR #27.

## Release checklist

1. Run the GitHub CI checks and inspect the exact reviewed PR commit.
2. Confirm Vercel preview builds and SPA deep links. Avoid making previews public.
3. Merge to protected `main`; expect **both** Vercel and Render to auto-deploy.
4. Verify the Vercel production deployment SHA, HTTP status for `/`, `/login`, and `/dashboard`, and browser-side auth with the synthetic VIEWER.
5. Confirm Render deployment is `live`, `/actuator/health` is HTTP 200, and Vercel-origin CORS preflight succeeds.
6. Keep secrets out of Git. Do not enable `VITE_USE_MOCKS` on production.
7. Consider retiring the unused Cloudflare Pages resource and removing the legacy origin **only after** verifying there are no remaining consumers. Do not delete historical screenshots or deployment evidence.

## Open engineering follow-ups

- The backend is redeployed even for frontend-only and docs-only GitHub commits; split deployment triggers or use an explicit Render build filter when safe.
- Free-tier Render cold starts remain slow: a 2026-10-23 long-running health request completed with HTTP 200 after **165.39 s**, whereas a subsequent warm health call returned HTTP 200 in **0.48 s**. Maintain visible frontend loading/retry states and avoid mistaking a short timeout for a persistent outage.
- Flyway has logged a warning for PostgreSQL 18.6 because the bundled version advertises support up to PostgreSQL 17; track upstream compatibility before database upgrades.
- [PR #28](https://github.com/Praciller/opspulse-ai/pull/28) was **closed without merging**: live OPTIONS tests allow `https://opspulse-ai-gamma.vercel.app` with HTTP 200 and reject unauthorized origins with HTTP 403. The proposed additive CORS code is not required.
- A read-only Vercel Integration view confirms a Neon-managed resource named `opspulse-ai-demo` exists, while Render startup logs confirm PostgreSQL **18.6**, successful validation of **11 migrations**, and schema `public` at version **11**. The exact direct ownership/linkage between this Neon resource and Render's active database remains **unverified**; do not expose connection strings to establish it.
- [Documentation PR #26](https://github.com/Praciller/opspulse-ai/pull/26) passed **7/7 GitHub checks** but remains a **Draft** with a Vercel Git status `Deployment rate limited — retry in 24 hours` (reported 2026-10-22 20:06:55 UTC). Do not force a paid deployment or merge just to clear a provider status. Merging docs to `main` would also trigger the existing Render auto-deploy.
- Historical Cloudflare-specific instructions in [DEPLOYMENT.md](DEPLOYMENT.md) are archival, **not** a current runbook.
