# Heater Repair Workshop API

A Spring Boot microservice for managing heater repair orders. It preserves the framework-independent domain model introduced in Milestone 3 and adds REST adapters, JPA/PostgreSQL persistence, centralized JSON error responses, and OpenAPI documentation restricted to the `dev` profile.

## Requirements

- Docker Engine or Docker Desktop
- Docker Compose

Java 17 and Maven 3.9+ are only required when running the application without Docker.

## Environment configuration

Create a local environment file before starting Docker:

```bash
cp .env.example .env
```

Replace `POSTGRES_PASSWORD` with a strong environment-specific value. The `.env`
file is ignored by Git and must never be committed. The example selects the
`dev` profile for local development; deployments that omit
`SPRING_PROFILES_ACTIVE` use `prod` by default.

## Run with Docker

```bash
docker compose up -d --build
```

The frontend repository must be cloned beside this repository using its default
directory name:

```text
parent-directory/
|-- heater-repair-workshop/
`-- heater-repair-workshop-frontend/
```

The command builds and starts the Nginx frontend, Spring Boot backend, and
PostgreSQL database. Java, Maven, Node.js, npm, and PostgreSQL do not need to be
installed on the host.

Check the services:

```bash
docker compose ps
docker compose logs -f frontend app postgres
```

- Web application: <http://localhost:8081>
- API: <http://localhost:8080>

The frontend sends `/api` requests to Nginx, which proxies them to the backend
over the internal Docker network. No server IP needs to be compiled into the
frontend image. Change `FRONTEND_PORT` in `.env` when port 8081 is unavailable.

## OpenAPI and profiles

The example environment uses the `dev` profile. Under this profile, Swagger UI and the OpenAPI specification are available at:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Documentation endpoints also require an authenticated session. Sign in on the same
host before opening them directly on the backend in development.

Swagger is disabled by default and under the `prod` profile. Set this value in
`.env` for a production-like run:

```bash
SPRING_PROFILES_ACTIVE=prod
```

Then run `docker compose up -d --build` again.

## Authentication (AUTH-01)

All repair-order operations require a server session. See [AUTH-01 operations and QA](docs/AUTH-01.md)
for initial user provisioning, CSRF, cookies, configuration and the manual QA checklist.
There is no public registration, default user or default password.

`GET /api/health` is the sole non-authentication public endpoint: it returns `200`
with an empty body for infrastructure liveness monitoring.

## REST API

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/repair-orders` | Creates an order in the `RECEIVED` state (`201`) |
| `GET` | `/api/repair-orders` | Lists orders newest first (`200`) |
| `GET` | `/api/repair-orders/{id}` | Retrieves an order (`200`) |
| `PATCH` | `/api/repair-orders/{id}/start` | Starts a received order with a diagnosis (`200`) |
| `PATCH` | `/api/repair-orders/{id}/complete` | Completes an order and notifies the customer (`200`) |

The following examples require an authenticated cookie jar and CSRF token as described in [AUTH-01](docs/AUTH-01.md). Add `-b cookies.txt -H "X-CSRF-TOKEN: $CSRF_TOKEN"` to mutation requests.

Create an order:

```bash
curl -i -X POST http://localhost:8080/api/repair-orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Maria Gonzalez","customerContact":"+56911112222","heaterBrand":"Bosch","heaterModel":"Therm 5700","serviceType": "REPAIR", "reportedIssue":"The heater turns off after a few minutes."}'
```

The backend returns the generated `ORDER-<UUID>` identifier. Substitute that
value for `ORDER_UUID` when starting and completing the order:

```bash
curl -i -X PATCH http://localhost:8080/api/repair-orders/ORDER_UUID/start \
  -H "Content-Type: application/json" \
  -d '{"diagnosis":"Damaged ignition sensor"}'

curl -i -X PATCH http://localhost:8080/api/repair-orders/ORDER_UUID/complete
```

Errors use a consistent JSON contract containing `timestamp`, `status`, `error`, `message`, `path`, and `validationErrors`.

## Contract testing

The `bruno/` directory contains the create → start → complete workflow. Authenticate first, enable the cookie jar, and provide the current CSRF header on mutations; see [AUTH-01](docs/AUTH-01.md). Anonymous collection execution now correctly returns `401`.

## Run without Docker

After creating `.env`, start only PostgreSQL:

```bash
docker compose up -d postgres
```

Then run Spring Boot with Java 17 and Maven 3.9+:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Automated tests

```bash
mvn clean verify
```

The JaCoCo report is generated at `target/site/jacoco/index.html`.

## Architecture

The project follows Clean Architecture and keeps this dependency direction:

```text
infrastructure → application → domain
```

- `domain`: entities, value objects, business exceptions, and repository contracts.
- `application`: use cases and outbound ports.
- `infrastructure/web`: REST controllers, DTOs, validation, and global error handling.
- `infrastructure/persistence`: JPA entities, Spring Data repositories, and domain mapping.
- `infrastructure/config`: dependency wiring and development-only OpenAPI configuration.

The domain contains no Spring or JPA annotations. Persistence entities remain in the infrastructure layer and are mapped to the domain aggregate.

## Stop the services

```bash
docker compose down
```

The `heater_workshop_data` volume preserves PostgreSQL data when containers are recreated. Remove it only when a full database reset is required:

```bash
docker compose down --volumes
```

## Deploy on an Oracle Cloud VM

Use an Ubuntu VM with Docker Engine and Docker Compose installed. Clone the
backend and frontend repositories as sibling directories, then prepare the
production environment from the backend directory:

```bash
cp .env.production.example .env
nano .env
```

Replace `POSTGRES_PASSWORD` with a strong unique value. If the VM serves the
application on a domain, set `CORS_ALLOWED_ORIGINS` to its `https://` URL. For
an initial IP-based deployment through the Nginx gateway, the example value is
sufficient because the backend is not exposed publicly.

Build and start the production stack:

```bash
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps
```

The production Compose file publishes only the Nginx frontend on port 80.
Spring Boot and PostgreSQL remain accessible exclusively through the internal
Docker network. Allow inbound TCP port 80 in both the Oracle Cloud network
security rules and the VM firewall, then configure an HTTPS terminator before using AUTH-01 in production. Plain HTTP does not support the required production Secure session cookie.

Apply later application updates with:

```bash
git -C ../heater-repair-workshop-frontend pull --ff-only
git pull --ff-only
docker compose -f docker-compose.prod.yml up -d --build
```

## Deploy the API on Render with Neon

The repository includes a `render.yaml` Blueprint for the free Docker web
service. Create a PostgreSQL project in Neon and obtain its JDBC connection
details. In Render, create a new Blueprint from this repository and provide the
secret values requested during the initial setup:

| Variable | Value |
|---|---|
| `DB_URL` | Neon JDBC URL, for example `jdbc:postgresql://HOST/DATABASE?sslmode=require` |
| `DB_USERNAME` | Neon database role |
| `DB_PASSWORD` | Neon database password |
| `CORS_ALLOWED_ORIGINS` | Exact Cloudflare Workers URL |

Render provides `PORT` automatically; Spring Boot reads it through
`server.port=${PORT:8080}`. The Blueprint selects the `prod` profile and uses
`/api/health` as its health check. Never commit Neon credentials to this
repository.

The production service is deployed from `main` at:

- API: <https://heater-repair-workshop-api.onrender.com>

Neon may display a connection string beginning with `postgresql://`. For Spring
Boot, set `DB_URL` to the equivalent JDBC form beginning with
`jdbc:postgresql://`. Store all four variables in Render, not in a committed
`.env` file, and deploy the latest commit from `main`.

Validate the production API after deployment:

```bash
curl -i https://heater-repair-workshop-api.onrender.com/api/repair-orders
curl -i https://heater-repair-workshop-api.onrender.com/route-that-does-not-exist
```

Both unauthenticated requests must return `401`. `GET /api/health` must return
`200` with an empty body. Authenticated repair-order requests retain their existing responses.

## Service types (I6)

New create requests require `serviceType`: `REPAIR` or `MAINTENANCE`.
`REPAIR` requires a nonblank `reportedIssue`. For `MAINTENANCE`, the field may
be omitted, null, empty, whitespace, or observations. Absence is normalized to
`""`; supplied text is trimmed. Every order response includes `serviceType` and
always represents `reportedIssue` as a string. Both types use the existing
RECEIVED → IN_PROGRESS → COMPLETED lifecycle and require diagnosis to start.

Repair request:

```json
{
  "customerName": "Juan Pérez",
  "customerContact": "+56912345678",
  "heaterBrand": "Junkers",
  "heaterModel": "WR11",
  "serviceType": "REPAIR",
  "reportedIssue": "No enciende"
}
```

Maintenance request:

```json
{
  "customerName": "Juan Pérez",
  "customerContact": "+56912345678",
  "heaterBrand": "Junkers",
  "heaterModel": "WR11",
  "serviceType": "MAINTENANCE",
  "reportedIssue": ""
}
```

Missing/null service types, unknown enum names, and repairs without an issue
return HTTP 400. Existing authentication and CSRF requirements still apply.

### Existing database compatibility

`service_type` uses `@Enumerated(EnumType.STRING)` and a nullable column of
length 32. New domain orders always require and persist an explicit type.
Only the persistence adapter interprets a historical SQL NULL as `REPAIR`;
reading does not backfill or modify historical rows. Saving such an order
through the existing lifecycle writes its explicit `REPAIR` type.

`reported_issue` now has a nullable JPA mapping (length 2000). The domain and
adapter save absent maintenance observations as the empty string, not SQL NULL.
A persisted MAINTENANCE with SQL NULL observations also restores as `""`.
Historical repairs still require a valid issue; fallback does not bypass domain
restoration invariants.

The installed Hibernate 7.4.5.Final `StandardTableMigrator` adds missing columns
but does not alter nullability of existing columns. With `ddl-auto: update`,
PostgreSQL therefore gets a nullable `service_type varchar(32)` column, leaving
historical rows NULL. An old `reported_issue NOT NULL` constraint can remain;
empty-string normalization makes new maintenance records compatible with it.
Fresh schemas allow SQL NULL according to the mapping. No manual data update,
column default, table recreation, or migration framework is needed for I6.

`JpaRepairOrderPostgresDdlTest` checks generated PostgreSQL DDL offline using
the actual entity mapping. `JpaRepairOrderSchemaUpdateTest` executes Hibernate
update against a populated legacy H2 schema in PostgreSQL mode and verifies
both service types across all statuses after flushing and clearing JPA state.
These tests do not connect to production or claim validation against a live
PostgreSQL server. Normal schema-update permissions are still required at startup.
