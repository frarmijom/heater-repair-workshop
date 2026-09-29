# HITO 04 — I3 articles and initial stock

I3 makes inventory articles operational. It adds item listing, detail, create,
metadata edit, activation/deactivation and low-stock visibility. It does not
add later entries, adjustments, reversal UI/history or work-order consumption.

## Database migration

**DATABASE MIGRATION REQUIRED FOR I3: YES.** Apply
`db/inventory-items-v3.sql` after HITO 03, I1 and the deployed I2 migration
`db/inventory-stock-v2.sql`:

```sh
psql -v ON_ERROR_STOP=1 <connection-options> -f db/inventory-items-v3.sql
```

I2 stores request IDs only on movements. A zero-stock article has no movement,
so it needs a durable creation request. The additive v3 table stores the
request ID, payload hash and resulting item ID. Its deferred item FK permits
claiming the request before inserting the item in the same transaction; commit
requires a real item. No deployed I2 migration or table is rewritten. Hibernate
continues to use `ddl-auto: validate`.

## API

All endpoints use the existing authenticated session and CSRF requirements. The
actor is always the authenticated principal; clients cannot supply it.

| Method | Resource | Behavior |
|---|---|---|
| GET | `/api/inventory/items` | Lists all active and inactive items with category/unit summaries and derived lowStock/hasMovements |
| GET | `/api/inventory/items/{id}` | Reads item metadata and stock without movement history |
| POST | `/api/inventory/items` | Creates an item and optional initial stock |
| PATCH | `/api/inventory/items/{id}` | Updates allowed metadata/status with `expectedVersion` |

Create accepts SKU, name, optional description, active category/unit IDs,
stockMinimum, referenceUnitCost, initialStock and requestId. Omitted numeric
defaults are zero. Decimal values accept JSON numbers or exact decimal strings;
the frontend sends strings to preserve all digits across JavaScript. Unknown
fields are rejected. PATCH does not accept stockCurrent, IDs, timestamps or
movement data. SKU conflicts, changed request payloads and stale versions return
409. There is no DELETE endpoint.

## Stock and idempotency

An item is first created at zero stock. A zero `initialStock` creates no
movement. A positive initialStock creates one `INITIAL_ENTRY` through the I2
movement use case, with zero stockBefore, exact stockAfter, reference-cost
snapshot, creation reference, reason, unit/item snapshots and authenticated
actor. Item, request claim, stock and movement share one transaction.

The creation request table has a unique request ID. Replaying the same
normalized payload returns the original item; reusing the ID for a different
payload conflicts. PostgreSQL uses `INSERT ... ON CONFLICT DO NOTHING`; H2 has a
test-only SQL variant. Both paths preserve rollback if item or initial movement
creation fails.

New category/unit assignments require active catalog records. An item may
change unit only while it has no movements; the check and item lock run in the
edit transaction. I2 also prevents changing a unit's `allowsDecimal` after it
has movement history. Stock remains writable only through authorized
movements. Deactivation/reactivation changes metadata only and preserves stock
and history.

## Frontend behavior

The inventory sidebar opens Articles. Its tabs retain Articles, Categories and
Units. Article search by SKU/name, active-state/category filters and the low
stock filter run client-side over the v1 collection. The list has loading,
empty, retry and conflict feedback states. The create form defaults initial
stock to zero and validates decimal scale and whole-unit rules before submit;
the backend remains authoritative. After a movement exists, the unit selector
is read-only and stock is displayed without an editable control. Low stock is
shown as text, not color alone.

## Verification and scope

The disposable PostgreSQL verification applies HITO 03, I1, I2 and I3, verifies
baseline preservation, migration replay, and runs Java/JPA integration for
concurrent request replay, initial movement, unit lock and full rollback. H2
HTTP tests cover auth/CSRF, active catalog checks, conflict responses, exact
decimals, edits and activation.

I4 remains responsible for later entries and full movement history. I5 owns
adjustments/reversals; I6 owns work-order consumption. Purchasing, sales, tax,
suppliers, reservations, warehouses, locations, lots, serials and notifications
remain out of HITO 04 v1.