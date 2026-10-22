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
- **Dependency vulnerabilities:** inspect open Dependabot PRs, `npm audit`, Java dependencies, and Docker image advisories. Major upgrades (for example Tailwind v4) require a migration plan, not `npm audit fix --force`.
- **Hosted operations:** no new hosted smoke, performance test, free-tier verification, credential rotation, or deployment was performed.
- **Portfolio UI:** redesign once the safety regression suite is passing.

## Post-PR verification

- [ ] Java/Gradle CI and Testcontainers integration suite green.
- [ ] Frontend lint, tests, build, browser tests green.
- [ ] Security, CodeQL and Docker checks green.
- [ ] Review dependency findings by runtime exposure; do not conflate test-tool alerts with deployed server vulnerabilities.
- [ ] GitHub admin applies and validates main branch rules.
- [ ] Production smoke is performed only after a separately authorized deploy.
