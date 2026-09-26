# AUTH-01 implementation report

Both independent repositories remain on `feature/authentication`. The complete
workspace design contract was inspected before changes. The authorized health
exception and Cloudflare proxy are implemented. No commits, merges or deployments
were performed; no production secrets or production data were accessed.

## Implemented

- Spring Security 7.1.1 on the existing Spring Boot 4.1.1 / Java 17 stack.
- Persistent JPA users with unique email, salted password hash, enabled state and timestamps.
- Session login/check/logout, session identifier rotation, CSRF protection, generic errors.
- Protection for every existing repair-order route; authenticated business behavior retained.
- HttpOnly/Secure/SameSite=Lax host-only session cookie; local HTTP exception only for dev.
- Public `GET /api/health`: empty `200`; Render health check updated.
- Frontend session-first startup, login/loading/errors, logout and session-expiry handling.
- Same-origin Cloudflare `/api/*` proxy using the repository's actual static-assets Worker
  configuration; fixed existing Render origin, no API caching, no redirect following.
- Administrative offline password-hash tool and provisioning/QA documentation.

## File inventory

Paths below are relative to the indicated repository.

### heater-repair-workshop

**Created**

- `docs/AUTH-01-IMPLEMENTATION.md`
- `docs/AUTH-01.md`
- `scripts/HashPassword.java`
- `src/main/java/com/heaterworkshop/infrastructure/config/SecurityConfiguration.java`
- `src/main/java/com/heaterworkshop/infrastructure/persistence/JpaUserEntity.java`
- `src/main/java/com/heaterworkshop/infrastructure/persistence/SpringDataUserRepository.java`
- `src/main/java/com/heaterworkshop/infrastructure/web/AuthenticationController.java`
- `src/main/java/com/heaterworkshop/infrastructure/web/HealthController.java`
- `src/test/java/com/heaterworkshop/infrastructure/web/AuthenticationIntegrationTest.java`
- `src/test/java/com/heaterworkshop/infrastructure/web/ProductionCookieTest.java`

**Modified**

- `README.md`
- `pom.xml`
- `render.yaml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application.yml`

### heater-repair-workshop-frontend

**Created**

- `src/services/api.ts`
- `src/services/auth-service.ts`
- `tests/auth.test.ts`
- `tests/proxy.test.ts`
- `worker/api-proxy.mjs`

**Modified**

- `.env.example`
- `README.md`
- `package-lock.json`
- `package.json`
- `src/main.ts`
- `src/services/repair-order-service.ts`
- `src/style.css`
- `wrangler.jsonc`

## Dependencies

Backend: added `spring-boot-starter-security` (4.1.1, bringing Spring Security 7.1.1)
and test-only `h2` (2.4.240), all versions managed by the existing Boot parent.
No Spring/Java upgrade. Existing tests plus real HTTP/JPA integration tests are used;
no unused security-test dependency was retained.

Frontend: added development-only `vitest` 4.1.11 and `jsdom` 26.1.0, selected for the
available Node 20.19 runtime. `package-lock.json` includes their transitive dependencies.
All preexisting locked dependency versions remain unchanged. The existing Wrangler
version remains 4.129.0; its dry-run used a temporary Node 24 runtime because it
requires Node >=22. No new runtime library/framework was introduced.

## Verification

- Backend `mvn clean verify`: passed before the final defensive adjustments.
- Backend final `mvn verify`: passed, 45 tests, zero failures/errors; includes real HTTP
  authentication/CSRF/cookie/repair workflow tests and existing regression tests.
- JaCoCo: 97.32% line / 84.29% branch coverage. Existing gates remain 80% / 60%;
  they were not relaxed.
- Frontend `npm ci`: passed, reproducible installation of the final lockfile.
- Frontend `npm test`: passed, 11 tests (7 UI/transport, 4 proxy), zero failures.
- Frontend `npm run build`: passed, TypeScript validation and Vite production build.
- Wrangler 4.129.0 `deploy --dry-run`: successful validation, no deployment.
- `javac` compiled `scripts/HashPassword.java` using the resolved application classpath;
  no real password was entered and the utility was not used to provision any account.
- `git diff --check`: no whitespace errors in either repository.

Initial compile/tooling failures were corrected: the CSRF access-denied handler uses
the Spring Security 7 exception-handling configuration; Node-compatible test tool
versions replaced incompatible latest versions; a transient npm 9 resolution error
was handled using npm 10 for installation. Subsequent `npm ci` uses the normal npm.
An npm invocation from the workspace parent was corrected to the frontend directory.

`npm audit` reports one preexisting high-severity development dependency advisory:
`nanoid` 3.3.16, via Vite/PostCSS, [GHSA-2v37-7h3g-55p8](https://github.com/advisories/GHSA-2v37-7h3g-55p8).
It was not upgraded as part of AUTH-01; no new production dependency is involved.

## Configuration and remaining work

No new mandatory environment variable or secret. Existing database/CORS/profile/port
variables remain; production uses `prod` and HTTPS. Remove obsolete frontend
`VITE_API_URL` from Cloudflare build settings. Local Vite retains
`VITE_BACKEND_PROXY_TARGET`. Provision the first user administratively with a generated
hash. See [operations and QA](AUTH-01.md) for exact commands and the complete checklist.

The implementation and local automated verification do not establish production
readiness. Still pending: real-browser HTTPS/cookie/proxy checks on Cloudflare/Render,
PostgreSQL/Neon schema and existing-data verification, actual idle-expiry/restart QA,
visual/functional QA of the dashboard and filters, Render health-check confirmation,
and code review. No external deployment or production QA was performed. These are the
remaining acceptance items from sections 16–18; no implementation feature is deferred.
