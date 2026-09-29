#!/usr/bin/env python3
"""I2 PostgreSQL migration and concurrency verification using a disposable container."""
import json
from pathlib import Path
import subprocess
import threading
import time
from concurrent.futures import ThreadPoolExecutor

root = Path(__file__).resolve().parents[1]


def docker(*args, **kwargs):
    return subprocess.run(["docker", *args], text=True, capture_output=True, **kwargs)


container = docker("run", "--pull=never", "--rm", "-d", "-p", "127.0.0.1::5432",
                  "--tmpfs", "/var/lib/postgresql/data", "-e", "POSTGRES_HOST_AUTH_METHOD=trust",
                  "postgres:17-alpine", check=True).stdout.strip()

try:
    for _ in range(100):
        if docker("exec", container, "pg_isready", "-h", "127.0.0.1", "-U", "postgres").returncode == 0:
            break
        time.sleep(0.2)
    else:
        raise RuntimeError("PostgreSQL did not start")

    def sql(query, check=True):
        result = docker("exec", "-i", container, "psql", "-h", "127.0.0.1", "-U", "postgres",
                        "-d", "postgres", "-v", "ON_ERROR_STOP=1", "-At", input=query)
        if check and result.returncode:
            raise RuntimeError(result.stderr)
        return result

    sql((root / "db/schema-v1.sql").read_text())
    sql("""INSERT INTO workshop_users VALUES ('fixture-user','fixture@example.test','unused-test-hash',true,now(),now());
    INSERT INTO work_orders(order_id,customer_name,customer_contact,heater_brand,heater_model,service_type,
      reported_issue,status,received_at,lifecycle_version) VALUES
      ('ORDER-550E8400-E29B-41D4-A716-446655440001','Fixture','+56911112222','Bosch','Therm','MAINTENANCE','','RECEIVED',now(),'V1');""")
    sql((root / "db/inventory-catalogs-v1.sql").read_text())
    sql("""INSERT INTO inventory_categories VALUES
      ('00000000-0000-0000-0000-000000000001','Parts','parts',true,0,now(),now());
    INSERT INTO inventory_units VALUES
      ('00000000-0000-0000-0000-000000000001','Unidad','unidad','un','un',false,true,0,now(),now());""")

    def snapshot(table):
        return json.loads(sql(f"SELECT json_agg(t) FROM {table} t").stdout)

    baseline = {table: snapshot(table) for table in
                ("work_orders", "workshop_users", "inventory_categories", "inventory_units")}
    migration = (root / "db/inventory-stock-v2.sql").read_text()
    sql(migration)
    assert all(snapshot(table) == values for table, values in baseline.items())
    assert sql("SELECT count(*) FROM inventory_items").stdout.strip() == "0"
    assert sql("SELECT count(*) FROM inventory_movements").stdout.strip() == "0"
    assert sql(migration, False).returncode != 0
    item_migration = (root / "db/inventory-items-v3.sql").read_text()
    sql(item_migration)
    assert sql("SELECT count(*) FROM inventory_item_creation_requests").stdout.strip() == "0"
    assert sql(item_migration, False).returncode != 0

    precisions = sql("""SELECT table_name || '.' || column_name || ':' || numeric_precision || ',' || numeric_scale
      FROM information_schema.columns WHERE table_name IN ('inventory_items','inventory_movements')
      AND data_type='numeric' ORDER BY table_name,column_name""").stdout.splitlines()
    assert precisions == [
        "inventory_items.reference_unit_cost:19,4", "inventory_items.stock_current:19,3",
        "inventory_items.stock_minimum:19,3", "inventory_movements.quantity:19,3",
        "inventory_movements.stock_after:19,3", "inventory_movements.stock_before:19,3",
        "inventory_movements.unit_cost_snapshot:19,4",
    ], precisions

    item_id = "00000000-0000-0000-0000-000000000010"
    entry_id = "00000000-0000-0000-0000-000000000020"
    sql(f"""INSERT INTO inventory_items VALUES
      ('{item_id}','FILTER-1','FILTER-1','Filter',NULL,'00000000-0000-0000-0000-000000000001',
       '00000000-0000-0000-0000-000000000001',1,0,1.2345,false,0,now(),now());
      INSERT INTO inventory_movements VALUES
      ('{entry_id}','{item_id}','00000000-0000-0000-0000-000000000001','INITIAL_ENTRY','INCREASE',
       1,0,1,1.2345,'request-entry',now(),'fixture-actor',NULL,NULL,NULL,NULL,NULL,
       'FILTER-1','Filter','Unidad','un');""")
    assert sql("SELECT reference_unit_cost || ':' || stock_current FROM inventory_items WHERE id='" + item_id + "'").stdout.strip() == "1.2345:1.000"

    duplicate_sku = """INSERT INTO inventory_items VALUES
      ('00000000-0000-0000-0000-000000000011','filter-1','FILTER-1','Filter',NULL,
       '00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000001',0,0,0,true,0,now(),now());"""
    assert sql(duplicate_sku, False).returncode != 0
    for command in (
        f"UPDATE inventory_items SET active=false WHERE id='{item_id}';" + duplicate_sku,
        f"UPDATE inventory_items SET stock_current=-1 WHERE id='{item_id}'",
        f"UPDATE inventory_items SET stock_minimum=-1 WHERE id='{item_id}'",
        f"UPDATE inventory_items SET reference_unit_cost=-1 WHERE id='{item_id}'",
        """INSERT INTO inventory_items VALUES
          ('00000000-0000-0000-0000-000000000012','BAD-FK','BAD-FK','Filter',NULL,
           '00000000-0000-0000-0000-000000000099','00000000-0000-0000-0000-000000000001',0,0,0,true,0,now(),now());""",
        """INSERT INTO inventory_items VALUES
          ('00000000-0000-0000-0000-000000000013','BAD-UNIT','BAD-UNIT','Filter',NULL,
           '00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000099',0,0,0,true,0,now(),now());""",
        f"UPDATE inventory_movements SET quantity=2 WHERE id='{entry_id}'",
        f"DELETE FROM inventory_movements WHERE id='{entry_id}'",
    ):
        assert sql(command, False).returncode != 0, command

    reversal_id = "00000000-0000-0000-0000-000000000021"
    reversal = f"""INSERT INTO inventory_movements VALUES
      ('{reversal_id}','{item_id}','00000000-0000-0000-0000-000000000001','REVERSAL','DECREASE',
       1,1,0,1.2345,'request-reversal',now(),'fixture-actor',NULL,NULL,NULL,NULL,'{entry_id}',
       'FILTER-1','Filter','Unidad','un');"""
    sql(reversal)
    duplicate_reversal = reversal.replace(reversal_id, "00000000-0000-0000-0000-000000000022").replace(
        "request-reversal", "request-reversal-2")
    assert sql(duplicate_reversal, False).returncode != 0
    duplicate_request = reversal.replace("request-reversal", "request-entry").replace(
        reversal_id, "00000000-0000-0000-0000-000000000023").replace(entry_id, "NULL")
    assert sql(duplicate_request, False).returncode != 0

    rollback = f"""BEGIN;
      UPDATE inventory_items SET stock_current=0 WHERE id='{item_id}';
      INSERT INTO inventory_movements VALUES
        ('00000000-0000-0000-0000-000000000024','{item_id}','00000000-0000-0000-0000-000000000001',
         'WORK_ORDER_CONSUMPTION','DECREASE',1,1,0,1.2345,'rollback-invalid',now(),'fixture-actor',
         NULL,NULL,NULL,'ORDER-00000000-0000-0000-0000-000000000099',NULL,
         'FILTER-1','Filter','Unidad','un');
      COMMIT;"""
    assert sql(rollback, False).returncode != 0
    assert sql(f"SELECT stock_current FROM inventory_items WHERE id='{item_id}'").stdout.strip() == "1.000"
    assert sql("SELECT count(*) FROM inventory_movements WHERE request_id='rollback-invalid'").stdout.strip() == "0"

    # A failure in the second DDL table must roll back the first table too.
    sql("""CREATE SCHEMA collision;
      CREATE TABLE collision.work_orders(order_id varchar(64) PRIMARY KEY);
      CREATE TABLE collision.workshop_users(id varchar(64) PRIMARY KEY);
      CREATE TABLE collision.inventory_categories(id uuid PRIMARY KEY);
      CREATE TABLE collision.inventory_units(id uuid PRIMARY KEY);
      CREATE TABLE collision.inventory_movements(id uuid PRIMARY KEY);""")
    assert sql("SET search_path TO collision;" + migration, False).returncode != 0
    assert sql("SELECT to_regclass('collision.inventory_items') IS NULL").stdout.strip() == "t"

    # Two independent PostgreSQL sessions serialize on the item row; one must roll back.
    concurrent_id = "00000000-0000-0000-0000-000000000030"
    sql(f"""INSERT INTO inventory_items VALUES
      ('{concurrent_id}','RACE-1','RACE-1','Race',NULL,'00000000-0000-0000-0000-000000000001',
       '00000000-0000-0000-0000-000000000001',1,0,0,true,0,now(),now());""")
    barrier = threading.Barrier(2)

    def consume(number):
        request = f"race-{number}"
        command = f"""DO $$ DECLARE available numeric; BEGIN
          SELECT stock_current INTO available FROM inventory_items WHERE id='{concurrent_id}' FOR UPDATE;
          PERFORM pg_sleep(0.35);
          IF available < 1 THEN RAISE EXCEPTION 'insufficient stock'; END IF;
          UPDATE inventory_items SET stock_current=available-1,version=version+1,updated_at=now()
            WHERE id='{concurrent_id}';
          INSERT INTO inventory_movements VALUES
            ('00000000-0000-0000-0000-0000000000{40 + number}','{concurrent_id}',
             '00000000-0000-0000-0000-000000000001','WORK_ORDER_CONSUMPTION','DECREASE',1,
             available,available-1,0,'{request}',now(),'fixture-actor',NULL,NULL,NULL,
             'ORDER-550E8400-E29B-41D4-A716-446655440001',NULL,'RACE-1','Race','Unidad','un');
        END $$;"""
        barrier.wait()
        return sql(command, False)

    with ThreadPoolExecutor(max_workers=2) as pool:
        outcomes = list(pool.map(consume, (1, 2)))
    assert sorted(result.returncode == 0 for result in outcomes) == [False, True]
    assert sql(f"SELECT stock_current FROM inventory_items WHERE id='{concurrent_id}'").stdout.strip() == "0.000"
    assert sql(f"SELECT count(*) FROM inventory_movements WHERE item_id='{concurrent_id}'").stdout.strip() == "1"
    assert all(snapshot(table) == values for table, values in baseline.items())
    sql("""CREATE FUNCTION test_delay_inventory_stock_update() RETURNS trigger LANGUAGE plpgsql AS $$
      BEGIN PERFORM pg_sleep(0.4); RETURN NEW; END; $$;
      CREATE TRIGGER test_delay_inventory_stock_update BEFORE UPDATE OF stock_current ON inventory_items
      FOR EACH ROW EXECUTE FUNCTION test_delay_inventory_stock_update();""")
    mapped_port = docker("port", container, "5432/tcp", check=True).stdout.strip().rsplit(":", 1)[1]
    import os
    environment = os.environ.copy()
    environment.update({
      "I2_POSTGRES_TEST": "true",
      "I3_POSTGRES_TEST": "true",
      "DB_URL": f"jdbc:postgresql://127.0.0.1:{mapped_port}/postgres",
      "DB_USERNAME": "postgres",
      "DB_PASSWORD": "",
      "CORS_ALLOWED_ORIGINS": "http://localhost",
    })
    java_test = subprocess.run(["mvn", "-q", "-Dtest=InventoryStockPostgresConcurrencyTest,InventoryItemsPostgresIntegrationTest", "test"],
                   cwd=root, env=environment, text=True, capture_output=True)
    if java_test.returncode:
      raise RuntimeError(java_test.stdout + java_test.stderr)
    print("PASS: I1/HITO 03 preservation, I2 schema/precision/constraints, SKU and request idempotency,")
    print("      immutable movements, unique reversal, atomic rollback and PostgreSQL/JPA concurrent stock mutation.")
finally:
    docker("rm", "-f", container)