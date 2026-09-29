# HITO 04 — I2 stock core

I2 adds only the domain and persistence foundation for inventory items and
immutable stock movements. It does not expose item, initial-stock, entry,
adjustment, reversal, history or work-order-consumption APIs. The frontend has
no I2 changes. Operational workflows remain in I3-I6.

## Schema and precision

Apply `db/inventory-stock-v2.sql` after the HITO 03 baseline and
`db/inventory-catalogs-v1.sql`, with the application stopped:

```sh
psql -v ON_ERROR_STOP=1 <connection-options> -f db/inventory-stock-v2.sql
```

The migration is additive and transactional. It creates `inventory_items` and
`inventory_movements`, references I1 categories/units and `work_orders`, and
does not alter earlier tables or migrations. Hibernate remains
`ddl-auto: validate`; schema creation belongs to the explicit SQL migration.

Quantities and stock use Java `BigDecimal` and PostgreSQL `NUMERIC(19,3)`;
reference unit costs use `NUMERIC(19,4)`. Values exceeding scale or precision
are rejected, never rounded by the domain. Stock and minimum are nonnegative,
movement quantities are positive, and low stock is derived as
`stockCurrent < stockMinimum`. Decimal quantities are checked against the
referenced unit's `allowsDecimal` value.

New items start with zero stock. The internal stock operation accepts a
movement, not an arbitrary stock value. It writes the versioned item and its
movement in one transaction, so failure to append the movement rolls back the
stock update.

## Movements and concurrency

The supported movement types are `INITIAL_ENTRY`, `ENTRY`,
`WORK_ORDER_CONSUMPTION`, `ADJUSTMENT` and `REVERSAL`. Normal quantities are
positive; explicit `INCREASE` or `DECREASE` direction determines their effect.
The movement stores before/after stock, unit cost, actor, request ID, references,
optional work order, reversal link, and SKU/item/unit snapshots. Confirmed rows
are protected from UPDATE and DELETE by a PostgreSQL trigger. A partial unique
index permits at most one reversal per original movement.

`request_id` is globally unique in PostgreSQL. Replaying the same request and
payload returns its original movement; reusing the request with another payload
conflicts. The database unique constraint remains the final guard against
concurrent duplicate requests.

The application locks the item row with PostgreSQL `SELECT ... FOR UPDATE`,
then validates current stock and the unit before updating the versioned item and
appending the immutable movement. Both writes share one Spring transaction. Two
competing outgoing operations serialize; the second observes the committed
stock and rolls back if it is insufficient. PostgreSQL integration tests use
independent transactions, not H2, for this guarantee. `allowsDecimal` cannot be
changed after a unit has movement history.

## Validation

```sh
mvn test
python3 scripts/test-inventory-stock-migration.py
mvn verify
```

The Python script uses a disposable PostgreSQL 17 container with no persistent
volume and a random loopback-only port. It applies HITO 03, I1 and I2, checks
baseline preservation, precision and constraints, rollback/replay, movement
immutability, request/reversal uniqueness, and invokes a Java/JPA concurrency
test against that same disposable database.

## Deferred scope

I3 owns item create/edit APIs and functional initial stock. I4 owns entries and
movement history; I5 owns adjustment/reversal workflows; I6 owns work-order
consumption. Sales, purchasing, suppliers, tax, reservations, multiple
warehouses, locations, transfers, lots, serials and notifications are outside
HITO 04 v1.