# Phase 8 Portfolio Handoff

> Status: **COMPLETE — hosted portfolio milestone verified 2026-09-06**

OpsPulse is public and hosted end-to-end:

- Repository: https://github.com/Praciller/opspulse-ai
- Frontend: https://opspulse-ai.pages.dev
- Backend: https://opspulse-ai-7gle.onrender.com
- Database: Vercel-managed Neon Free (`opspulse-ai-demo`)
- Hosted role: one public synthetic-data **VIEWER** account; no public ADMIN/MANAGER/OPERATOR credential

## Verified evidence

- [x] GitHub CI green, including build, frontend, Docker, npm security, CodeQL, and dependency review.
- [x] Flyway V1–V11 validated on hosted Neon; schema version 11.
- [x] Render health returns HTTP 200 when live.
- [x] Hosted bootstrap evaluated 21 entities and created 20 deterministic risk events.
- [x] Hosted bootstrap persisted a system `RULE_BASED` brief via the normal fallback path (`fallback=true`).
- [x] `scripts/smoke-test.ps1` passes against the hosted backend with VIEWER positive reads and expected 403 write/action gates.
- [x] Cloudflare Pages production bundle points to the real Render backend.
- [x] Three screenshots were captured from the real hosted stack: dashboard, risk events, and AI brief.
- [x] No real personal/company data is used; hosted business data is synthetic.

## Accepted limitations

- Render Free cold/deploy starts for this Java workload were observed around 200–267 seconds.
- The Vercel-managed Neon resource currently reports PostgreSQL 18.6; Flyway warns that its latest formally tested PostgreSQL is 17, but migrations V1–V11 validated and executed successfully.
- The public hosted demo deliberately uses deterministic `RULE_BASED` output because no AI provider key is configured.
- VIEWER cannot import, run risk scans, generate/approve briefs, or perform other privileged mutations.

## Stopping condition

Phase 8 is portfolio-sufficient after the final evidence/documentation PR merges and temporary local credential bridge files are deleted. No new OpsPulse features should be added as part of this milestone.
