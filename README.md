# Heater Repair Workshop

Backend Java puro para gestionar órdenes de reparación de calefones desde su recepción hasta su cierre. El proyecto aplica Arquitectura Limpia y patrones tácticos de Domain-Driven Design (DDD), manteniendo las reglas del negocio aisladas de frameworks y mecanismos de persistencia.

## Lenguaje ubicuo y contexto delimitado

El contexto **Repair Management** comienza con la recepción de una orden y termina al completar la reparación. Facturación, inventario y entrega física quedan fuera de sus límites.

| Concepto | Definición |
|---|---|
| Repair Order | Agregado que representa el ciclo completo de una reparación. |
| Diagnosis | Evaluación técnica válida que permite iniciar el trabajo. |
| Repair Status | Estado de la orden: recibida, en progreso o completada. |
| Customer Contact | Número internacional utilizado para informar al cliente. |
| Repair Order Repository | Contrato para almacenar y recuperar órdenes sin decidir la tecnología. |

## Arquitectura

```text
src/main/java/com/heaterworkshop/
├── domain/                       # Reglas del negocio; Java puro
│   ├── entity/                   # RepairOrder como Aggregate Root
│   ├── valueobject/              # Records inmutables y auto-validantes
│   ├── exception/                # Excepciones del negocio
│   └── repository/               # Contratos de persistencia
├── application/
│   ├── port/                     # Contratos requeridos por los casos de uso
│   └── usecase/                  # Orquestación de flujos del negocio
└── infrastructure/
    └── persistence/              # Implementaciones tecnológicas reemplazables
```

La dirección de las dependencias es:

```text
infrastructure -> application -> domain
```

El dominio no contiene anotaciones ni dependencias de Spring, JPA, bases de datos u otros frameworks.

## Patrones aplicados

- `RepairOrder` es una entidad con identidad y la raíz del agregado.
- `RepairOrderId`, `CustomerContact` y `Diagnosis` son Value Objects implementados como `record`.
- Los Value Objects rechazan datos inválidos durante su construcción.
- Las transiciones de estado se protegen dentro de `RepairOrder`.
- `RepairOrderRepository` es una interfaz pura ubicada en el dominio.
- Los casos de uso reciben sus contratos mediante inyección por constructor.
- `InMemoryRepairOrderRepository` demuestra que la infraestructura puede sustituirse sin modificar el núcleo.

## Reglas del negocio

- Una orden nueva comienza con estado `RECEIVED` y sin diagnóstico.
- Una orden solo puede comenzar con un diagnóstico válido.
- Solo una orden recibida puede pasar a `IN_PROGRESS`.
- Solo una orden en progreso puede pasar a `COMPLETED`.
- Al iniciar o completar una reparación se persiste la orden.
- Al completar una reparación se notifica al cliente.

## Requisitos

- Java 17
- Maven 3.9 o superior

## Compilación y pruebas

Compilar el proyecto:

```bash
mvn clean compile
```

Ejecutar las pruebas:

```bash
mvn test
```

Ejecutar todas las verificaciones y exigir 100% de cobertura de líneas y ramas:

```bash
mvn clean verify
```

El informe de cobertura queda disponible en `target/site/jacoco/index.html`.
