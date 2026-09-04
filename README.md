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

Swagger is disabled by default and under the `prod` profile. Set this value in
`.env` for a production-like run:

```bash
SPRING_PROFILES_ACTIVE=prod
```

Then run `docker compose up -d --build` again.

## REST API

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/repair-orders` | Creates an order in the `RECEIVED` state (`201`) |
| `GET` | `/api/repair-orders` | Lists orders newest first (`200`) |
| `GET` | `/api/repair-orders/{id}` | Retrieves an order (`200`) |
| `PATCH` | `/api/repair-orders/{id}/start` | Starts a received order with a diagnosis (`200`) |
| `PATCH` | `/api/repair-orders/{id}/complete` | Completes an order and notifies the customer (`200`) |

Create an order:

```bash
curl -i -X POST http://localhost:8080/api/repair-orders \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Maria Gonzalez","customerContact":"+56911112222","heaterBrand":"Bosch","heaterModel":"Therm 5700","reportedIssue":"The heater turns off after a few minutes."}'
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

The `bruno/` directory contains an executable collection that verifies the complete create → start → complete workflow. Select its `local` environment while the Docker services are running.

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
security rules and the VM firewall, then open `http://PUBLIC_IP`.

Apply later application updates with:

```bash
git -C ../heater-repair-workshop-frontend pull --ff-only
git pull --ff-only
docker compose -f docker-compose.prod.yml up -d --build
```
