-- PostgreSQL. Run once with the old application stopped, after taking a backup.
-- psql -v ON_ERROR_STOP=1 -f db/work-orders-v1.sql ...
-- Everything below succeeds together or rolls back. Never infer customer approval.
BEGIN;
LOCK TABLE repair_orders IN ACCESS EXCLUSIVE MODE;
ALTER TABLE repair_orders RENAME TO work_orders;
ALTER TABLE work_orders ADD COLUMN IF NOT EXISTS service_type varchar(32);
ALTER TABLE work_orders ADD COLUMN lifecycle_version varchar(16);
ALTER TABLE work_orders ADD COLUMN legacy_status varchar(32);
ALTER TABLE work_orders ADD COLUMN customer_decision varchar(16);
UPDATE work_orders SET lifecycle_version = 'LEGACY', legacy_status = status;
ALTER TABLE work_orders ALTER COLUMN lifecycle_version SET NOT NULL;
-- Replace only the old status-enum check. Refuse unfamiliar status-related checks.
DO $$
DECLARE c record;
DECLARE normalized text;
BEGIN
    FOR c IN SELECT conname, conkey, pg_get_constraintdef(oid) AS definition
        FROM pg_constraint WHERE conrelid = 'work_orders'::regclass AND contype = 'c'
    LOOP
        IF c.definition ~ '\mstatus\M' THEN
            normalized := regexp_replace(c.definition, '::(character varying|text)', '', 'g');
            normalized := regexp_replace(normalized, '[[:space:]()]', '', 'g');
            IF cardinality(c.conkey) <> 1
                OR normalized !~ '^CHECKstatus=ANYARRAY\[''(RECEIVED|IN_PROGRESS|COMPLETED)'',''(RECEIVED|IN_PROGRESS|COMPLETED)'',''(RECEIVED|IN_PROGRESS|COMPLETED)''\]\[\]$'
                OR c.definition NOT LIKE '%RECEIVED%' OR c.definition NOT LIKE '%IN_PROGRESS%'
                OR c.definition NOT LIKE '%COMPLETED%' THEN
                RAISE EXCEPTION 'Unrecognized status constraint: %', c.definition;
            END IF;
            EXECUTE format('ALTER TABLE work_orders DROP CONSTRAINT %I', c.conname);
        END IF;
    END LOOP;
END $$;
ALTER TABLE work_orders ADD CONSTRAINT work_orders_status_check CHECK
    (status IN ('RECEIVED','DIAGNOSIS','WAITING_CUSTOMER','WAITING_PARTS','IN_PROGRESS','COMPLETED','NOT_APPROVED'));
ALTER TABLE work_orders ADD CONSTRAINT work_orders_lifecycle_check CHECK
    (lifecycle_version IN ('LEGACY','V1') AND
     ((lifecycle_version = 'V1' AND legacy_status IS NULL) OR
      (lifecycle_version = 'LEGACY' AND legacy_status IS NOT NULL AND legacy_status IN ('RECEIVED','IN_PROGRESS','COMPLETED'))));
ALTER TABLE work_orders ADD CONSTRAINT work_orders_decision_check CHECK
    (customer_decision IS NULL OR customer_decision IN ('APPROVED','REJECTED'));
COMMIT;
