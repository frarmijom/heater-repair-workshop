# AUTH-01 operations and QA

Implementation contract: workspace `iterations/01_AUTHENTICATION_DESIGN.md`, plus the
explicit authorization to add a minimal public infrastructure health endpoint and
Cloudflare same-origin API proxy. No production deployment is part of this change.

## HTTP contract

| Operation | Behavior |
| --- | --- |
| `GET /api/health` | Anonymous, `200`, empty body; process liveness only, no database or configuration details |
| `GET /api/auth/csrf` | Anonymous, establishes a pre-login session, returns `headerName` and masked `token` |
| `POST /api/auth/login` | JSON `{ "email": "...", "password": "..." }`, valid CSRF required; `200` with email, invalid credentials `401` |
| `GET /api/auth/session` | `200` with email for an authenticated session; otherwise `401` |
| `POST /api/auth/logout` | Valid CSRF required; invalidates session and expires cookie; `204` |
| Existing `/api/repair-orders/**` | Requires authentication; anonymous requests return `401`, including mutations without CSRF |

Fetch CSRF before login and refresh it after successful login (the previous token
is invalidated). Send the returned token under its returned header name on POST
and PATCH, including logout. After logout, obtain a new token before another login.
An authenticated mutation with missing/invalid CSRF returns `403`. Do not retry
mutations automatically. Cookie jars must be enabled for curl/Bruno clients.
Passwords are never returned; wrong passwords, unknown and disabled users receive
the same generic `401` body. Empty/oversized login fields also return generic `401`.
Malformed JSON returns a generic `400` response.

## Sessions and deployment

The servlet container maintains sessions in this application instance's memory.
Idle timeout is 30 minutes. Restart, Render restart/sleep, or redeployment can require
users to sign in again. No distributed session infrastructure is introduced.
Successful login changes the session identifier. Logout invalidates it server-side,
including attempts to replay the previous cookie.

Cookie: `WORKSHOP_SESSION`, host-only, `Path=/`, `HttpOnly`, `SameSite=Lax`,
`Secure` by default and in production. Only the `dev` profile disables Secure for
local HTTP. Production requires HTTPS; do not enable `dev` to work around TLS.
The existing plain-HTTP production Compose example requires HTTPS termination
before authenticated production use. There is no authentication data in browser storage.
The in-memory CSRF token is not a session credential.

Existing backend variables remain `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
`CORS_ALLOWED_ORIGINS`, `SPRING_PROFILES_ACTIVE`, and optional `PORT`.
No new production secret or mandatory environment variable is introduced.
Use `SPRING_PROFILES_ACTIVE=prod` on Render. Keep `CORS_ALLOWED_ORIGINS`
restricted to the existing exact frontend URL; the same-origin proxy does not need
credentialed CORS. Render's health path becomes `/api/health`; its configured
production branch remains `main`, unchanged by this feature branch.

The frontend always uses relative `/api` URLs. Remove obsolete `VITE_API_URL`
from Cloudflare build settings; it is no longer consumed. The existing Wrangler
static-assets deployment gains `worker/api-proxy.mjs` and `run_worker_first` for
`/api/*`. Its upstream is the HTTPS Render origin already documented by the repo.
Cookies and CSRF headers pass through; API responses are never cached, redirects
are not followed, and foreign Origin headers are rejected. Static assets retain
Cloudflare's normal serving behavior. No new service or Cloudflare secret is needed.
Local Vite and Docker/Nginx continue using their existing API proxies.

## Administrative first-user provisioning

No user is created automatically. Existing Hibernate `ddl-auto: update` creates
`workshop_users` with the new entity. This retains the existing schema-management
mechanism; no migration framework or alteration to repair-order mappings is introduced.
Back up the database and verify the schema addition against a non-production copy
before release. Tests use isolated H2 databases, not Neon or existing local volumes.

Generate a hash locally using the project's Spring Security encoder, without
putting a password in command arguments, shell history, environment variables or Git:

```bash
mvn dependency:build-classpath -Dmdep.outputFile=target/hash-classpath.txt
java --class-path "$(cat target/hash-classpath.txt)" scripts/HashPassword.java
```

Run from an interactive terminal. Input is hidden and confirmation is required.
The utility prints only a salted `{bcrypt}` hash (default Spring strength 10).
Use a strong, unique password, at most 72 UTF-8 bytes, bcrypt's input limit.
Treat the resulting hash as sensitive; do not commit it or paste it into tickets.
The application accepts email addresses, normalized with trim/lowercase on login.

Using an authorized PostgreSQL administrative connection (credentials managed outside
Git), insert the account. This SQL is a template; substitute locally, never commit
actual values. Keep SQL history/logging and screen recordings from retaining hashes.

```sql
INSERT INTO workshop_users (id, email, password_hash, enabled, created_at, updated_at)
VALUES ('USER-<uuid>', lower(trim('<email>')), '<generated {bcrypt} hash>', true, now(), now());
```

The unique email constraint prevents duplicate accounts. This procedure does not reset
or overwrite existing accounts. No public user administration endpoint is provided.

## Verification and release QA

Automated: `mvn clean verify` in the backend and `npm test && npm run build` in the
frontend. Backend integration tests run a real HTTP server and isolated JPA/H2
persistence. They cover all protected endpoints, valid/invalid/disabled login,
password hashing and log redaction, CSRF, session fixation, cookies, logout replay,
and the authenticated create/list/get/start/complete workflow. Existing domain,
controller, persistence adapter and CORS tests remain in place. Frontend DOM tests
cover initial authentication, duplicate submission, refresh, logout, expiry and
safe errors. Proxy tests cover cookies, headers, redirects and origin restrictions.

Remaining manual QA, in a separately authorized non-production environment:

- Provision an administrative user against PostgreSQL/Neon; verify the additive
  schema change preserves existing repair records.
- In a real browser over HTTPS through Cloudflare, verify login, invalid credentials,
  refresh, logout and all repair-order operations, filters and dashboard statistics.
- Inspect Set-Cookie and confirm Secure, HttpOnly, SameSite=Lax, Path=/ and no Domain.
- Wait for session idle expiry or restart the QA backend; the next protected request
  must return to login without automatic retries or stale dashboard access.
- Confirm `/api/*` reaches Render (including browser navigation), never the SPA fallback;
  confirm API responses are not cached and no browser cross-origin API calls remain.
- Confirm `/api/health` is public and empty while direct anonymous repair APIs return
  `401`; verify Render accepts the new health path without exposing workshop data.
- Inspect application logs and browser responses for credential or diagnostic leakage.

Production readiness remains pending this manual QA and code review. No deploy,
merge, account creation on production, or production database access was performed.

References: [Spring session persistence](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html),
[Spring CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html),
[Cloudflare Worker routing](https://developers.cloudflare.com/workers/static-assets/routing/worker-script/).
