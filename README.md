# Heater Repair Workshop

Heater Repair Workshop is a pure Java domain project for managing water-heater repair orders from reception to completion.

## Architecture

The project implements a Pure Domain Core inspired by Clean Architecture and Ports and Adapters. Domain classes have no dependencies on web frameworks, databases, or infrastructure libraries.

External operations are represented by the `RepairOrderRepository` and `CustomerNotifier` interfaces. They are provided to `RepairService` through constructor injection.

## Business rules

- A new repair order starts with the `RECEIVED` status and no diagnosis.
- A diagnosis must not be null, empty, or blank.
- Only a received order can move to `IN_PROGRESS`.
- Only an order in progress can move to `COMPLETED`.
- Closing an order saves it and notifies the customer.

## Testing and quality

- JUnit 5 for automated unit tests.
- Mockito Core for dependency isolation.
- Arrange, Act, Assert structure in every test.
- Custom business exceptions verified with `assertThrows`.
- Parameterized validation tests.
- JaCoCo rules requiring 100% line and branch coverage.

## Requirements

- Java 17
- Maven 3.9 or newer

## Commands

Run the tests:

```bash
mvn clean test
```

Run the tests, generate the coverage report, and enforce the coverage rules:

```bash
mvn clean verify
```

Open the generated coverage report at:

```text
target/site/jacoco/index.html
```
