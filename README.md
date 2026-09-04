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

This command builds and starts both the Spring Boot API and PostgreSQL. Java and Maven do not need to be installed on the host because the application is compiled in a Maven container and runs in a separate JRE image.

Check the services:

```bash
docker compose ps
docker compose logs -f app
```

The API is available at <http://localhost:8080>.

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
