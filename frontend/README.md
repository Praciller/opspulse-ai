# OpsPulse Operations Console

This Vite/React application is the Phase 5/6 operations console for OpsPulse AI.

## Node.js requirement

Use Node.js 24 for local development and CI builds (see `.node-version`). Vite 8 requires Node 20.19+ or 22.12+; this project standardizes on Node 24 to match GitHub Actions.

## Local development

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

The default API target is `http://localhost:8080`. Set `VITE_API_BASE_URL` in
`.env.local` when the backend is hosted elsewhere. Real APIs are the default;
set `VITE_USE_MOCKS=true` only for deterministic UI demos and Playwright smoke
coverage. Mock mode is intentionally labelled in the dashboard, reports, and
imports experiences.

## Verification

```powershell
npm run lint
npm run test
npm run build
$env:VITE_USE_MOCKS='true'; npm run test:e2e
```

The optional real-API Playwright smoke test is enabled only when all three
variables are supplied: `E2E_API_BASE_URL`, `E2E_EMAIL`, and `E2E_PASSWORD`.
It exercises login, products, risks, and brief generation against a running
backend without storing credentials in the repository.

## Build output and deployment status

Run `npm ci && npm run build` to generate the Vite `dist/` bundle.
Set `VITE_API_BASE_URL` to the intended backend origin and do not enable
`VITE_USE_MOCKS` in a deployed environment.

The current production frontend runs on [Vercel](https://opspulse-ai-gamma.vercel.app), linked to GitHub `Praciller/opspulse-ai` and automatically built from `main`. Vercel project root is `frontend/`, framework Vite, Node.js 24, build command `npm run build` and output `dist`. The committed `vercel.json` provides SPA deep-link rewrites. Preview deployments remain protected by Vercel Authentication; production uses OpsPulse application authentication. The older `_redirects` file is kept only for historical static-host compatibility. See [the current deployment status](../docs/CURRENT_RELEASE_STATUS_2026-10-23.md).

Reports and imports now use the Phase 6 backend contracts. In mock mode they use
stateful deterministic fixtures; real APIs remain the default. Do not enable
mocks in a production environment.
