# Work Orders v1

## Domain and lifecycle

`WorkOrder`, `WorkOrderId`, `WorkOrderStatus` and `WorkOrderRepository` replace the
generic repair names. IDs still use `ORDER-<uppercase UUID>`. ServiceType.REPAIR is
unchanged. No inventory, billing, CRM or scheduling subsystem is introduced.

Repair:

```
RECEIVED -> DIAGNOSIS -> WAITING_CUSTOMER
                           | reject -> NOT_APPROVED (terminal)
                           | approve, parts available -> IN_PROGRESS -> COMPLETED
                           | approve, parts missing -> WAITING_PARTS -> IN_PROGRESS -> COMPLETED
```

Maintenance:

```
RECEIVED -> IN_PROGRESS -> COMPLETED
    | parts missing -> WAITING_PARTS -> IN_PROGRESS -> COMPLETED
```

Diagnosis can be recorded/updated only during DIAGNOSIS; completing diagnosis
requires nonblank text. Approval is a dedicated action with mandatory parts
availability. Approval with available parts begins work atomically. Approval with
missing parts records the decision and waits. Starting from WAITING_PARTS asserts
that parts are now available. Maintenance does not enter diagnosis/customer-decision
states. NOT_APPROVED closes the order without a completion timestamp because no
work was completed. There is no arbitrary status setter or reopening action.

## Explicit historical compatibility

The migration records `lifecycle_version=LEGACY` and copies each original status
into `legacy_status`. It does not generate diagnosis, approval, or timestamps.
`customer_decision` remains NULL. Existing field values remain unchanged.

- Original COMPLETED: closed and unchanged, including historical timestamps.
- Original IN_PROGRESS: may complete without retroactive approval; provenance stays
  LEGACY and original status stays IN_PROGRESS after completion.
- Original RECEIVED: retains its provenance but must meet the new service-specific
  prerequisites; it cannot borrow the exemption for previously started work.
- New orders: constructor always creates V1 with no legacy status or decision.
  Creation DTOs do not expose provenance/decision setters. Extra JSON fields cannot
  grant a legacy exemption. Restoration rejects inconsistent provenance/states.
- Only historical rows with a null service type fall back to REPAIR. A V1 row with
  a null service type is invalid.

Historical maintenance diagnosis is retained as historical data. New maintenance
never requires diagnosis. The UI explicitly labels legacy provenance and absence
of a recorded customer approval; it never labels the absence as approval.

`reportedIssue` remains required for REPAIR. MAINTENANCE accepts missing/null/blank
observations and stores/returns an empty string. The existing NOT NULL SQL constraint
on that column can remain compatible. Text is trimmed.

## API

Base resource: `/api/work-orders`. No alias for `/api/repair-orders` is provided.
Create uses customerName, customerContact, heaterBrand, heaterModel, serviceType,
reportedIssue. Example:

```json
{"customerName":"Juan Pérez","customerContact":"+56912345678","heaterBrand":"Junkers","heaterModel":"WR11","serviceType":"MAINTENANCE","reportedIssue":""}
```

POST returns 201; collection/item GET and business PATCH actions return 200.
Actions appended to `/{id}`:

| PATCH action | Body | Preconditions |
|---|---|---|
| `/diagnosis/begin` | none | REPAIR, RECEIVED |
| `/diagnosis` | `{"diagnosis":"Sensor failure"}` | REPAIR, DIAGNOSIS |
| `/diagnosis/complete` | none | REPAIR, DIAGNOSIS, recorded diagnosis |
| `/approve` | `{"partsAvailable":true}` or false | REPAIR, WAITING_CUSTOMER |
| `/reject` | none | REPAIR, WAITING_CUSTOMER |
| `/waiting-parts` | none | MAINTENANCE, RECEIVED |
| `/start` | none | MAINTENANCE RECEIVED, or eligible WAITING_PARTS |
| `/complete` | none | IN_PROGRESS |

Responses retain the original order data and add `lifecycleVersion` (LEGACY/V1),
`legacyStatus` (original status/null), `customerDecision` (APPROVED/REJECTED/null).
Invalid input: 400; absent order: 404; invalid transition: 409. Existing safe error
handling, session, CSRF, logout and cookie behavior remain. A technical user records
the customer's decision; this is not a customer portal or digital signature.

Bruno's numbered requests follow the repair path with unavailable parts. Authenticate
first with its cookie jar and set `csrfToken` from `/api/auth/csrf` after login.
Reject is an alternative to approval, not an action after completion.

## PostgreSQL migration runbook

Hibernate now uses `ddl-auto: validate` so a missed migration fails startup rather
than silently creating an empty work_orders table. No migration framework or startup
migration is installed. Do not override this with update/create on an existing DB.

1. Back up the target database and stop the old backend before changing the schema.
2. Verify the selected database/schema contains the expected repair_orders and no
   work_orders. Inspect any custom status constraints before executing the script.
3. With an authorized connection, run:

   ```sh
   psql -v ON_ERROR_STOP=1 -f db/work-orders-v1.sql <connection-options>
   ```

4. Verify row counts and original fields against the backup; verify every migrated
   row is LEGACY, legacy_status matches its original state, and customer_decision
   remains NULL. Users are not modified.
5. Start the new backend with schema validation, then the matching frontend.

The script uses one transaction and an exclusive table lock. It renames the populated
table, adds provenance/decision columns, and replaces the previous status enum CHECK.
It refuses a replay/collision. Unknown status-related constraint shapes fail instead
of being silently removed. Failure rolls back the transaction. Do not run the old
backend against the migrated schema. Application rollback requires a separately
reviewed database recovery plan; do not rename back after v1 writes arbitrarily.

For an empty database only, use `db/schema-v1.sql`, which creates work_orders and
workshop_users. It intentionally fails if these tables already exist. Existing
Docker volumes require the migration too; starting Compose does not apply it.

No migration was applied to an existing installation during implementation.

## Verification

```
mvn clean verify
python3 scripts/test-work-orders-migration.py
```

The migration test requires a local postgres:17-alpine Docker image. Its database
uses a disposable tmpfs container, no network, no host ports, and no mounted data.
It compares nine legacy records before/after (both services plus historical null
service types), checks absent approvals, enum expansion, fresh initialization,
replay refusal, collision rollback, and rollback for unsupported historical states
or custom status constraints. It does not read DB_URL.

Backend tests cover valid/forbidden transitions, restoration, legacy/V1 separation,
JPA round trips, API decisions and protected routes. Frontend tests cover repair
approval/rejection, parts waits, maintenance without diagnosis, terminal states,
legacy labels, duplicate blocking, retries and late responses after logout.

Frontend routes are #work-orders, #work-orders/new, #work-orders/search and
#work-orders/{id}. All seven statuses are counted independently. NOT_APPROVED is
not counted as COMPLETED. Attention remains newly received work; activity derives
only reception/completion timestamps. The UI displays the actual current state,
recorded diagnosis/decision and valid next actions, without fabricating a linear
history through optional waits. No startedAt or full event history is introduced.

## Limits retained from the existing application

No optimistic locking, per-order ownership, external notification transport,
server-side search/pagination, or full audit trail is added. The existing notifier
logs after completion; it is not an atomic external delivery guarantee. Deployment,
existing database migration and integrated production QA remain separate actions.
