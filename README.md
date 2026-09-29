# Heater Repair Workshop API

A Spring Boot microservice for managing workshop work orders. It preserves the framework-independent domain model introduced in Milestone 3 and adds REST adapters, JPA/PostgreSQL persistence, centralized JSON error responses, and OpenAPI documentation restricted to the `dev` profile.

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

All work-order operations require a server session. See [AUTH-01 operations and QA](docs/AUTH-01.md)
for initial user provisioning, CSRF, cookies, configuration and the manual QA checklist.
There is no public registration, default user or default password.

`GET /api/health` is the sole non-authentication public endpoint: it returns `200`
with an empty body for infrastructure liveness monitoring.

## REST API

Work Orders v1 uses `/api/work-orders`. See [the lifecycle, API and migration guide](docs/WORK-ORDERS-V1.md)
for action payloads and the explicit historical-data policy. The old `/api/repair-orders`
resource is no longer exposed. Backend and frontend must be released together.

| Method | Path | Result |
|---|---|---|
| POST | `/api/work-orders` | Creates a V1 order; 201 |
| GET | `/api/work-orders` | Lists orders newest first; 200 |
| GET | `/api/work-orders/{id}` | Retrieves an order; 200 |
| PATCH | `/api/work-orders/{id}/diagnosis/begin` | Begins repair diagnosis |
| PATCH | `/api/work-orders/{id}/diagnosis` | Records `{ "diagnosis": "..." }` |
| PATCH | `/api/work-orders/{id}/diagnosis/complete` | Waits for customer decision |
| PATCH | `/api/work-orders/{id}/approve` | Records approval and `{ "partsAvailable": true/false }` |
| PATCH | `/api/work-orders/{id}/reject` | Closes as NOT_APPROVED |
| PATCH | `/api/work-orders/{id}/waiting-parts` | Maintenance waits for parts |
| PATCH | `/api/work-orders/{id}/start` | Starts eligible work, without a diagnosis payload |
| PATCH | `/api/work-orders/{id}/complete` | Completes work and invokes the existing notifier |

All PATCH actions return the updated order with HTTP 200. Invalid transitions return 409;
invalid input returns 400. Session and CSRF remain required.

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
curl -i https://heater-repair-workshop-api.onrender.com/api/work-orders
curl -i https://heater-repair-workshop-api.onrender.com/route-that-does-not-exist
```

Both unauthenticated requests must return `401`. `GET /api/health` must return
`200` with an empty body. Authenticated work-order requests retain their existing responses.

## Work Orders v1

`ServiceType` remains `REPAIR` or `MAINTENANCE`. Repairs require `reportedIssue`;
maintenance observations are optional and normalize to `""`. Their lifecycles now differ.

**Schema initialization is explicit:** Hibernate uses `ddl-auto: validate` and will not
create an empty replacement table. Before starting this version, apply
`db/work-orders-v1.sql` to an existing installation with the old app stopped, or
`db/schema-v1.sql` to an empty database. See the [migration runbook](docs/WORK-ORDERS-V1.md).
Existing Docker volumes also require this step; no migration runs automatically.

Verification:

```bash
mvn clean verify
python3 scripts/test-work-orders-migration.py
```

The second command requires Docker and a local `postgres:17-alpine` image. It uses a
disposable container without host ports or database volumes and removes it afterward.

## HITO 04 — Inventory catalogs (I1)

Category and unit administration requires the additive
`db/inventory-catalogs-v1.sql` migration after the Work Orders v1 schema.
See [I1 contract, migration and verification](docs/INVENTORY-I1.md).
