#!/usr/bin/env python3
"""Run migration checks in a disposable PostgreSQL cluster; never uses DB_URL or existing servers.
Usage: python3 scripts/test-work-orders-migration.py
Requires Docker and an already available postgres:17-alpine image. No Python dependencies.
"""
import json
from pathlib import Path
import subprocess
import time

root = Path(__file__).resolve().parents[1]

def docker(*args, **kwargs):
    return subprocess.run(['docker', *args], text=True, capture_output=True, **kwargs)

# No host ports, host data mounts or existing database credentials are used.
container = docker('run', '--pull=never', '--rm', '-d', '--network=none',
                   '--tmpfs', '/var/lib/postgresql/data', '-e', 'POSTGRES_HOST_AUTH_METHOD=trust',
                   'postgres:17-alpine', check=True).stdout.strip()
try:
    for attempt in range(100):
        if docker('exec', container, 'pg_isready', '-U', 'postgres').returncode == 0:
            break
        time.sleep(0.2)
    else:
        raise RuntimeError('Disposable PostgreSQL did not become ready.')
    def sql(query=None, file=None, check=True):
        return docker('exec', '-i', container, 'psql', '-U', 'postgres', '-d', 'postgres',
                      '-v', 'ON_ERROR_STOP=1', '-At', input=file.read_text() if file else query, check=check)
    sql('''CREATE TABLE repair_orders (
        order_id varchar(64) PRIMARY KEY, customer_name varchar(200) NOT NULL,
        customer_contact varchar(16) NOT NULL, heater_brand varchar(120) NOT NULL,
        heater_model varchar(120) NOT NULL, service_type varchar(32), reported_issue varchar(2000) NOT NULL,
        status varchar(32) NOT NULL CHECK(status IN ('RECEIVED','IN_PROGRESS','COMPLETED')),
        diagnosis varchar(1000), received_at timestamptz NOT NULL, completed_at timestamptz);
        INSERT INTO repair_orders SELECT 'ORDER-550E8400-E29B-41D4-A716-' || lpad(n::text,12,'0'),
            'Historical customer','+56911112222','Bosch','Therm',
            CASE WHEN n < 4 THEN 'REPAIR' WHEN n < 7 THEN 'MAINTENANCE' ELSE NULL END,
            'Original issue', (ARRAY['RECEIVED','IN_PROGRESS','COMPLETED'])[(n-1)%3+1],
            CASE WHEN (n-1)%3=0 THEN NULL ELSE 'Original diagnosis' END,
            '2026-09-01T12:00:00Z'::timestamptz,
            CASE WHEN (n-1)%3=2 THEN '2026-09-02T12:00:00Z'::timestamptz ELSE NULL END
            FROM generate_series(1,9) n;''')
    before = json.loads(sql("SELECT json_agg(r ORDER BY order_id) FROM repair_orders r").stdout)
    sql(file=root/'db/work-orders-v1.sql')
    after = json.loads(sql("SELECT json_agg(r ORDER BY order_id) FROM work_orders r").stdout)
    for old, new in zip(before,after):
        assert new.pop('lifecycle_version') == 'LEGACY'
        assert new.pop('legacy_status') == old['status']
        assert new.pop('customer_decision') is None
        assert new == old
    assert len(after) == len(before) == 9
    assert sql("SELECT to_regclass('repair_orders') IS NULL").stdout.strip() == 't'
    assert sql(file=root/'db/work-orders-v1.sql',check=False).returncode != 0
    assert sql('SELECT count(*) FROM work_orders').stdout.strip() == '9'
    # Only disposable data is changed to exercise the new enum and explicit decision representation.
    sql("UPDATE work_orders SET status='DIAGNOSIS' WHERE legacy_status='RECEIVED'")
    sql("UPDATE work_orders SET status='WAITING_CUSTOMER',diagnosis='Recorded' WHERE status='DIAGNOSIS'")
    sql("UPDATE work_orders SET status='NOT_APPROVED',customer_decision='REJECTED' WHERE status='WAITING_CUSTOMER'")
    sql("CREATE SCHEMA fresh; SET search_path TO fresh;" + (root/'db/schema-v1.sql').read_text())
    assert sql("SELECT count(*) FROM information_schema.tables WHERE table_schema='fresh'").stdout.strip() == '2'
    # A collision must roll back and leave both existing tables intact.
    sql("CREATE SCHEMA collision; CREATE TABLE collision.repair_orders (id int); CREATE TABLE collision.work_orders (id int);")
    result = sql("SET search_path TO collision;" + (root/'db/work-orders-v1.sql').read_text(),check=False)
    assert result.returncode != 0
    assert sql("SELECT count(*) FROM information_schema.tables WHERE table_schema='collision'").stdout.strip() == '2'
    # Unsupported historical states must roll back the rename and all column changes.
    sql("CREATE SCHEMA invalid; CREATE TABLE invalid.repair_orders (status varchar(32)); INSERT INTO invalid.repair_orders VALUES ('UNKNOWN');")
    result = sql("SET search_path TO invalid;" + (root/'db/work-orders-v1.sql').read_text(), check=False)
    assert result.returncode != 0
    assert sql("SELECT status FROM invalid.repair_orders").stdout.strip() == 'UNKNOWN'
    assert sql("SELECT to_regclass('invalid.work_orders') IS NULL").stdout.strip() == 't'
    # Never discard a custom constraint just because it mentions the three old states.
    sql("CREATE SCHEMA custom; CREATE TABLE custom.repair_orders (status varchar(32) CHECK(status IN ('RECEIVED','IN_PROGRESS','COMPLETED') AND status <> 'RECEIVED'));")
    result = sql("SET search_path TO custom;" + (root/'db/work-orders-v1.sql').read_text(), check=False)
    assert result.returncode != 0
    assert 'Unrecognized status constraint' in result.stderr
    assert sql("SELECT to_regclass('custom.repair_orders') IS NOT NULL AND to_regclass('custom.work_orders') IS NULL").stdout.strip() == 't'
    print('PASS: PostgreSQL migration preserves all 9 historical rows, provenance and absent decisions; new statuses, fresh schema, replay refusal and collision rollback verified.')
finally:
    docker('rm', '-f', container)
