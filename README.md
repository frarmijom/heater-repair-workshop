# Heater Repair Workshop - Milestone 4

A Spring Boot microservice for managing heater repair orders. It preserves the framework-independent domain model introduced in Milestone 3 and adds REST adapters, JPA/PostgreSQL persistence, centralized JSON error responses, and OpenAPI documentation restricted to the `dev` profile.

## Requirements

- Docker Engine or Docker Desktop
- Docker Compose

Java 17 and Maven 3.9+ are only required when running the application without Docker.

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

The Docker environment uses the `dev` profile by default. Under this profile, Swagger UI and the OpenAPI specification are available at:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Swagger is disabled by default and under the `prod` profile. Start the production profile with:

```bash
SPRING_PROFILES_ACTIVE=prod docker compose up -d --build
```

In PowerShell, use:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
docker compose up -d --build
```

## REST API

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/repair-orders` | Creates an order in the `RECEIVED` state (`201`) |
| `GET` | `/api/repair-orders/{id}` | Retrieves an order (`200`) |
| `PATCH` | `/api/repair-orders/{id}/start` | Starts a received order with a diagnosis (`200`) |
| `PATCH` | `/api/repair-orders/{id}/complete` | Completes an order and notifies the customer (`200`) |

Create an order:

```bash
curl -i -X POST http://localhost:8080/api/repair-orders \
  -H "Content-Type: application/json" \
  -d '{"id":"ORDER-001","customerContact":"+56911112222"}'
```

Start and complete the order:

```bash
curl -i -X PATCH http://localhost:8080/api/repair-orders/ORDER-001/start \
  -H "Content-Type: application/json" \
  -d '{"diagnosis":"Damaged ignition sensor"}'

curl -i -X PATCH http://localhost:8080/api/repair-orders/ORDER-001/complete
```

Errors use a consistent JSON contract containing `timestamp`, `status`, `error`, `message`, `path`, and `validationErrors`.

## Contract testing

The `bruno/` directory contains an executable collection that verifies the complete create → start → complete workflow. Select its `local` environment while the Docker services are running.

## Run without Docker

Start only PostgreSQL:

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
