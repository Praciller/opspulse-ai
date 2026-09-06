# OpsPulse Operations Console

This Vite/React application is the Phase 5/6 operations console for OpsPulse AI.

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

## Cloudflare Pages

Use the following Pages settings (no deployment is performed by this project):

- Build command: `npm ci && npm run build`
- Build output directory: `frontend/dist` when the repository root is used, or
  `dist` when `frontend/` is configured as the Pages root
- Environment variables: `VITE_API_BASE_URL`, and optionally `VITE_USE_MOCKS`
- The committed `frontend/_redirects` file keeps client-side routes working on
  direct navigation.

Reports and imports now use the Phase 6 backend contracts. In mock mode they use
stateful deterministic fixtures; real APIs remain the default. Do not enable
mocks in a production environment.
