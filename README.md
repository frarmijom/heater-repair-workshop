# Heater Repair Workshop — Hito 4

Microservicio Spring Boot para gestionar órdenes de reparación de calefones. Conserva el dominio del Hito 3 aislado de frameworks y agrega adaptadores REST, persistencia JPA/PostgreSQL, errores JSON centralizados y documentación OpenAPI restringida al perfil `dev`.

## Requisitos

- Java 17+
- Maven 3.9+
- Docker con Docker Compose

## Ejecución local

```bash
docker compose up -d
```

Este comando construye y ejecuta tanto la API Spring Boot como PostgreSQL. No es necesario instalar Java o Maven dentro de WSL: la compilación se realiza en una imagen Maven y la aplicación final corre en una imagen JRE separada.

Con el perfil `dev`, Swagger UI está en <http://localhost:8080/swagger-ui.html> y el contrato en <http://localhost:8080/v3/api-docs>.

En el perfil por defecto y en `prod`, ambos endpoints están deshabilitados:

```bash
SPRING_PROFILES_ACTIVE=prod docker compose up -d --build
```

## API REST

| Método | Ruta | Resultado |
|---|---|---|
| `POST` | `/api/repair-orders` | Crea una orden en estado `RECEIVED` (`201`) |
| `GET` | `/api/repair-orders/{id}` | Consulta una orden (`200`) |
| `PATCH` | `/api/repair-orders/{id}/start` | Inicia una orden con diagnóstico (`200`) |
| `PATCH` | `/api/repair-orders/{id}/complete` | Completa una orden y notifica (`200`) |

Crear una orden:

```bash
curl -i -X POST http://localhost:8080/api/repair-orders \
  -H "Content-Type: application/json" \
  -d '{"id":"ORDER-001","customerContact":"+56911112222"}'
```

Iniciar y completar:

```bash
curl -i -X PATCH http://localhost:8080/api/repair-orders/ORDER-001/start \
  -H "Content-Type: application/json" \
  -d '{"diagnosis":"Damaged ignition sensor"}'
curl -i -X PATCH http://localhost:8080/api/repair-orders/ORDER-001/complete
```

Los errores se entregan en un contrato JSON uniforme con `timestamp`, `status`, `error`, `message`, `path` y `validationErrors`.

La carpeta `bruno/` contiene una colección ejecutable que verifica el flujo crear → iniciar → completar.

## Pruebas

```bash
mvn clean verify
```

La arquitectura mantiene la dirección de dependencias `infrastructure -> application -> domain`. Las entidades JPA viven exclusivamente en infraestructura y se traducen al agregado de dominio.

## Operación con Docker

```bash
docker compose ps
docker compose logs -f app
docker compose down
```

El volumen `heater_workshop_data` conserva la información de PostgreSQL al recrear los contenedores. Sólo se elimina si se solicita explícitamente con `docker compose down --volumes`.
