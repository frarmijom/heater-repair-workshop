#!/usr/bin/env python3
"""I1 PostgreSQL verification. Disposable local image only; never reads DB_URL."""
import json
from pathlib import Path
import subprocess
import time
from concurrent.futures import ThreadPoolExecutor

root = Path(__file__).resolve().parents[1]
def docker(*args, **kwargs):
    return subprocess.run(['docker', *args], text=True, capture_output=True, **kwargs)
container = docker('run', '--pull=never', '--rm', '-d', '--network=none',
                  '--tmpfs', '/var/lib/postgresql/data', '-e', 'POSTGRES_HOST_AUTH_METHOD=trust',
                  'postgres:17-alpine', check=True).stdout.strip()
try:
    for _ in range(100):
        if docker('exec', container, 'pg_isready', '-h', '127.0.0.1', '-U', 'postgres').returncode == 0:
            break
        time.sleep(.2)
    else:
        raise RuntimeError('PostgreSQL did not start')
    def sql(query, check=True):
        result = docker('exec', '-i', container, 'psql', '-h', '127.0.0.1', '-U', 'postgres', '-d', 'postgres',
                        '-v', 'ON_ERROR_STOP=1', '-At', input=query)
        if check and result.returncode:
            raise RuntimeError(result.stderr)
        return result
    sql((root/'db/schema-v1.sql').read_text())
    sql("""INSERT INTO workshop_users VALUES ('fixture-user','fixture@example.test','unused-test-hash',true,now(),now());
    INSERT INTO work_orders(order_id,customer_name,customer_contact,heater_brand,heater_model,service_type,
      reported_issue,status,received_at,lifecycle_version) VALUES
      ('ORDER-550E8400-E29B-41D4-A716-446655440001','Fixture','+56911112222','Bosch','Therm','MAINTENANCE','','RECEIVED',now(),'V1');""")
    def snapshot(table):
        return json.loads(sql(f'SELECT json_agg(t) FROM {table} t').stdout)
    before = {table: snapshot(table) for table in ['work_orders', 'workshop_users']}
    migration = (root/'db/inventory-catalogs-v1.sql').read_text()
    sql(migration)
    assert all(snapshot(table) == data for table, data in before.items())
    assert sql('SELECT count(*) FROM inventory_categories').stdout.strip() == '0'
    assert sql('SELECT count(*) FROM inventory_units').stdout.strip() == '0'
    assert sql(migration, False).returncode != 0
    def category(identifier, name='Gas', normalized='gas', version=0):
        return f"INSERT INTO inventory_categories VALUES ('00000000-0000-0000-0000-{identifier:012d}','{name}','{normalized}',true,{version},now(),now());"
    sql(category(1))
    assert sql(category(2, 'GAS', 'gas'), False).returncode != 0
    sql('UPDATE inventory_categories SET active=false WHERE name=\'Gas\'')
    assert sql(category(2, 'Gas', 'gas'), False).returncode != 0
    for command in [category(2,'',''), category(2,' Gas',' gas'), category(2,'Other','wrong'), category(2,'Other','other',-1),
                    'UPDATE inventory_categories SET active=NULL', 'UPDATE inventory_categories SET updated_at=created_at-interval \'1 second\'']:
        assert sql(command, False).returncode != 0, command
    sql("INSERT INTO inventory_units VALUES ('00000000-0000-0000-0000-000000000001','Metro','metro','m','m',true,true,0,now(),now());")
    for name, key, symbol, symbol_key in [('Metro','metro','mt','mt'),('Longitud','longitud','M','m'),('Litro','litro','','')]:
        command=f"INSERT INTO inventory_units VALUES ('00000000-0000-0000-0000-000000000002','{name}','{key}','{symbol}','{symbol_key}',false,true,0,now(),now());"
        assert sql(command, False).returncode != 0
    # Independent connections exercise the expected-version update used by optimistic locking.
    command="WITH changed AS (UPDATE inventory_categories SET name='Nuevo',name_normalized='nuevo',version=version+1 WHERE version=0 RETURNING id) SELECT count(*) FROM changed;"
    with ThreadPoolExecutor(max_workers=2) as pool:
        counts = sorted(int(result.stdout.strip()) for result in pool.map(sql, [command, command]))
    assert counts == [0, 1]
    assert sql('SELECT version FROM inventory_categories').stdout.strip() == '1'
    # Existing conflicting catalog table must cause complete rollback, not partial installation.
    sql('CREATE SCHEMA collision; CREATE TABLE collision.work_orders(id int); CREATE TABLE collision.workshop_users(id int); CREATE TABLE collision.inventory_units(id int);')
    assert sql('SET search_path TO collision;' + migration, False).returncode != 0
    assert sql("SELECT to_regclass('collision.inventory_categories') IS NULL").stdout.strip() == 't'
    sql('CREATE SCHEMA missing;')
    assert sql('SET search_path TO missing;' + migration, False).returncode != 0
    assert sql("SELECT count(*) FROM information_schema.tables WHERE table_schema='missing'").stdout.strip() == '0'
    assert all(snapshot(table) == data for table, data in before.items())
    print('PASS: baseline preservation, empty catalogs, uniqueness including inactive records, constraints, replay/collision/missing-baseline rollback and concurrent version checks.')
finally:
    docker('rm', '-f', container)
