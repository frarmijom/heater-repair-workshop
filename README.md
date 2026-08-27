# Heater Repair Workshop

Pure Java backend for managing water-heater repair orders from reception to completion. The project applies Clean Architecture and tactical Domain-Driven Design (DDD) patterns while keeping business rules isolated from frameworks and persistence mechanisms.

## Ubiquitous Language and Bounded Context

The **Repair Management** bounded context begins when the workshop receives a repair order and ends when the repair is completed. Billing, inventory management, and physical delivery are outside its boundaries.

| Concept | Definition |
|---|---|
| Repair Order | Aggregate representing the complete lifecycle of a repair. |
| Diagnosis | Valid technical assessment required to start the repair. |
| Repair Status | Current stage of an order: received, in progress, or completed. |
| Customer Contact | International phone number used to notify the customer. |
| Repair Order Repository | Contract for storing and retrieving orders without selecting a persistence technology. |

## Architecture

```text
src/main/java/com/heaterworkshop/
|-- domain/                       # Pure Java business rules
|   |-- entity/                   # RepairOrder aggregate root
|   |-- valueobject/              # Immutable, self-validating records
|   |-- exception/                # Business exceptions
|   `-- repository/               # Persistence contracts
|-- application/
|   |-- port/                     # Contracts required by use cases
|   `-- usecase/                  # Business-flow orchestration
`-- infrastructure/
    `-- persistence/              # Replaceable technical implementations
```

The dependency direction is:

```text
infrastructure -> application -> domain
```

The domain contains no Spring, JPA, database, or other framework annotations or dependencies.

## Applied Patterns

- `RepairOrder` is an entity with a unique identity and acts as the aggregate root.
- `RepairOrderId`, `CustomerContact`, and `Diagnosis` are Value Objects implemented as Java records.
- Value Objects reject invalid data during construction.
- State transitions are protected within `RepairOrder`.
- `RepairOrderRepository` is a pure interface located in the domain.
- Use cases receive their contracts through constructor injection.
- `InMemoryRepairOrderRepository` demonstrates that infrastructure can be replaced without modifying the core.

## Business Rules

- A new repair order starts in the `RECEIVED` state without a diagnosis.
- A repair order can only start with a valid diagnosis.
- Only a received order can transition to `IN_PROGRESS`.
- Only an order in progress can transition to `COMPLETED`.
- Starting or completing a repair persists the order.
- Completing a repair notifies the customer.

## Requirements

- Java 17
- Maven 3.9 or newer

## Build and Test

Compile the project:

```bash
mvn clean compile
```

Run the unit tests:

```bash
mvn test
```

Run all checks and enforce 100% line and branch coverage:

```bash
mvn clean verify
```

The coverage report is generated at `target/site/jacoco/index.html`.
