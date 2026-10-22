# OpsPulse-AI — Verified release status (2026-10-23)

This document supersedes historical hosting assumptions in [the September deployment guide](DEPLOYMENT.md). The project owner states that Cloudflare is **not** the current deployment platform.

## Confirmed backend release

- GitHub [PR #25](https://github.com/Praciller/opspulse-ai/pull/25) merged to protected `main` at `3e660cc56db2a456ff131e1ebaddabd29de8324c`.
- [Post-merge CI](https://github.com/Praciller/opspulse-ai/actions/runs/39824182022) succeeded.
- [Render Web Service](https://dashboard.render.com/web/srv-daemol740ujc73fshgg0) `opspulse-ai` uses Docker, Free, Oregon, GitHub `Praciller/opspulse-ai`, `main`, and **auto-deploy on commit**.
- Render deployment `dep-dbd4j6nlk1mc73ft14a0` for the same SHA reports **live**, completed 2026-10-23 00:42:26 Asia/Bangkok.
- `/actuator/health` returned HTTP 200. An anonymous GET to `/api/dashboard` returned HTTP 401. Neither is an authenticated end-to-end smoke test.

## Frontend and data status

- React/Vite frontend builds in CI. A new **Vercel Hobby project exists, but is NOT deployed or live**: project `opspulse-ai` (`prj_Sfp03pCv1XJXVGJJeQpfthLddlv5`), with reserved domain `opspulse-ai-gamma.vercel.app`.
- The Vercel project uses Vite, root `frontend`, Node 24, output `dist`; preview/production environment variables set the Render backend URL and `VITE_USE_MOCKS=false`.
- **Git integration is not connected**: Vercel GitHub App installation cannot access `Praciller/opspulse-ai`; owner must add this specific repository via GitHub Installed GitHub Apps. Draft [PR #27](https://github.com/Praciller/opspulse-ai/pull/27) contains the Vercel SPA rewrite configuration.
- Before go-live, verify the Render CORS allowlist accepts the real Vercel frontend origin. The connected Render workspace has no OpsPulse static-site service.
- The September Cloudflare Pages site and screenshots are **historical evidence only**; do not treat them as the current frontend release.
- Historical docs identify an external Vercel-managed Neon database. Its current account/branch and schema were not independently verified. The connected Neon search showed no matching project; the Render workspace has no managed Postgres.
- The post-merge authenticated VIEWER smoke has **not** been verified.

## Remaining release tasks

1. Grant the Vercel GitHub App access to `Praciller/opspulse-ai`; connect the existing Vercel project, validate PR #27 SPA routing and a real preview, then verify CORS and release only after approval.
2. Run the approved least-privilege VIEWER smoke and record the actual results.
3. Verify external database ownership and schema read-only.
4. Treat durable CSV imports and AI metric grounding as separate future engineering work.

No Production settings or data are changed by this documentation PR.
