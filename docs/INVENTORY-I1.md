# HITO 04 — I1 catalog administration

I1 implements only InventoryCategory and UnitOfMeasure. There are no items,
stock, movements, material consumptions, fake references or seeded catalogs.

## Contract

Authenticated sessions and CSRF retain their existing behavior. No roles added.

| Method | Resource | Input |
|---|---|---|
| GET | /api/inventory/categories | none |
| POST | /api/inventory/categories | name |
| PATCH | /api/inventory/categories/{uuid} | expectedVersion and at least one of name, active |
| GET | /api/inventory/units | none |
| POST | /api/inventory/units | name, symbol, allowsDecimal |
| PATCH | /api/inventory/units/{uuid} | expectedVersion and at least one of name, symbol, allowsDecimal, active |

Create returns 201 and an active record at version zero. Lists and edits return
200. All responses contain stable id, name, active, version, createdAt, updatedAt;
units also contain symbol and allowsDecimal. Lists include inactive records.
PATCH rejects unknown fields, explicit nulls, missing/negative/noninteger versions
and requests with no change fields. Client-supplied IDs and timestamps are rejected.
No DELETE endpoint exists. API errors use the existing ApiError structure.
Conflicts (duplicate or stale version) return 409; missing records 404; invalid
input 400. Inventory conflict handling is scoped to the new controller.

## Normalization and concurrency

Names: required, at most 120 UTF-16 code units in the domain. Symbols: required,
at most 16. Text is normalized to Unicode NFC, whitespace collapsed and trimmed.
Names are unique within each catalog, and unit symbols independently unique.
PostgreSQL lower() computes stored comparison keys, backed by UNIQUE constraints,
including inactive records. JPA writes keys using the database lower() function,
so Java and database Unicode lowercase differences do not reject valid records.
Display casing is retained. Categories and units are separate uniqueness scopes.

ID and createdAt never change. Timestamps use microsecond precision, matching the
schema. The domain preserves immutability; the adapter applies edits to a managed
entity and flushes with @Version. expectedVersion is checked before changing state,
and the SQL version predicate also protects concurrent transactions. Editing an
inactive record is supported; active=false/true deactivates/reactivates it.

## Additive migration

For an existing Work Orders v1 database, back up and apply with the new application
stopped, using an explicitly selected connection:

```sh
psql -v ON_ERROR_STOP=1 <connection-options> -f db/inventory-catalogs-v1.sql
```

For an empty installation, first apply db/schema-v1.sql, then the I1 migration.
Do not change Hibernate ddl-auto from validate. Existing migration files are not
rewritten. The new migration creates inventory_categories and inventory_units in
one transaction, requiring the existing work_orders and workshop_users tables.
It creates no records and does not change either baseline table. Replay, missing
baseline and name collision fail; transaction rollback prevents partial tables.

No migration was applied to an existing installation during I1 development.
QA used a disposable PostgreSQL database and a synthetic user only.

## Frontend

Inventory in the existing sidebar opens #inventory/categories; the other catalog
uses #inventory/units. The module supports loading, empty/error/retry states,
creation, edit, confirmation before deactivation and reactivation. Writes are
blocked while a catalog loads or a write is pending. Entered values survive errors.
On a stale-version conflict use Actualizar listado and reopen the current record.
Route changes and logout discard obsolete responses. Business messages are shown
as text; catalog values are HTML-escaped.

## Verification

```sh
mvn clean verify
python3 scripts/test-inventory-catalogs-migration.py
```

The Python check uses an already installed postgres:17-alpine image in an isolated
container with tmpfs, no host ports, no existing volumes and no DB_URL access.
It verifies baseline preservation, constraints, rollback and concurrent version
predicates through independent PostgreSQL connections. HTTP tests exercise the
JPA version mechanism too. Browser QA used the actual API and PostgreSQL at
1440, 900, 390 and 320 px, including concurrent HTTP edits (one 200 and one 409).
Frontend validation: npm test and npm run build.

## Deferred to I2+

When InventoryItem exists, category/unit assignment must reject new inactive
references. UnitOfMeasureUseCases.edit is the point to add the allowsDecimal guard
for assigned units, coordinated transactionally with item assignment. There is
no fabricated assigned-item repository in I1. No inventory item or stock-related
code, tables or screens are introduced here.
