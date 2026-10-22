# OpsPulse AI — Hardening Review (2026-10-22)

## Scope

Static review of GitHub `main` at `f7c07557cfbf9c53169501b6706c211428af2e4c`.
This branch addresses bounded issues without merging, deploying, modifying production settings, rotating credentials, or changing production data.

## Implemented in this branch

1. Accept `Idempotency-Key` in CORS preflight for cross-origin CSV uploads; assert both trusted and untrusted origins.
2. Retry 503/504 automatically only for safe read methods, never for mutation requests that may already have committed.
3. Mark in-memory import jobs interrupted by a single-instance restart as `FAILED`; preserve completed jobs; cover the behavior in an integration test.
4. Bound AI error-class sanitization using the sanitized string length.
5. Correct historical hosted milestone status and unsupported AI/cache claims in README and deployment documentation.
6. Upgrade frontend build/test toolchain together (Vite 8, Vitest 5, Tailwind CSS 4, React plugin 6, and Tailwind PostCSS adapter). Remove the unused Autoprefixer direct dependency.

## Manual repository-admin action (not applied by this pull request)

Configure a GitHub **branch ruleset for `main`** under Settings → Rules → Rulesets:

- Require pull requests before merging.
- Require relevant GitHub Actions status checks, such as `build`, `frontend`, `docker`, `security`, and `codeql`, using exact check names from GitHub.
- Block branch deletion and force pushes.
- Disable bypasses where the account plan supports it.
- Confirm the ruleset rejects a failing branch and permits a passing one.

At audit time `main` was not protected and no rulesets were configured. This connector cannot modify repository administration settings.

## Deferred — not solved here

- **Durable imports:** uploaded CSV bytes are not persisted for retry, and successful rows may remain after an interrupted/failed job. Design durable uploads, work claiming, explicit partial-completion semantics, retry/idempotency and recovery.
- **AI quality and availability:** current output validation checks risk references but not the truth of every narrated metric. Add structured output validation and a grounding evaluation dataset. Separately refactor the AI provider request out of the open database transaction with concurrency and rollback tests.
- **Dependency follow-up:** the frontend dependency tree was upgraded together and passed a local clean install and `npm audit` with zero known findings on 2026-10-22. Continue monitoring future advisories and scan Java dependencies / the Docker image separately. Do not force-merge old Dependabot upgrade PRs.
- **Hosted operations:** no new hosted smoke, performance test, free-tier verification, credential rotation, or deployment was performed.
- **Portfolio UI:** redesign once the safety regression suite is passing.

## Local frontend verification (2026-10-22)

- Clean npm ci: PASS.
- Full npm audit --audit-level=high: PASS (0 findings).
- npm run lint: PASS.
- npm run test: PASS (17 unit tests).
- npm run build: PASS (Tailwind 4 CSS compile including custom design tokens).
- Mock Playwright E2E: 2 passed, 1 intentionally skipped (real API credentials not provided).
- These checks are local evidence; final GitHub CI and rendered visual review are still required before merging.

## Post-PR verification

- [ ] Java/Gradle CI and Testcontainers integration suite green.
- [ ] Frontend lint, tests, build, browser tests green.
- [ ] Security, CodeQL and Docker checks green.
- [ ] Review dependency findings by runtime exposure; do not conflate test-tool alerts with deployed server vulnerabilities.
- [ ] GitHub admin applies and validates main branch rules.
- [ ] Production smoke is performed only after a separately authorized deploy.
