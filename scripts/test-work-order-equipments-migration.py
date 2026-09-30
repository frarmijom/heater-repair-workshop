#!/usr/bin/env python3
"""Validate the multi-equipment migration in a disposable PostgreSQL container."""
from pathlib import Path
import subprocess, time
root = Path(__file__).resolve().parents[1]
def docker(*args, **kwargs):
    return subprocess.run(['docker', *args], text=True, capture_output=True, **kwargs)
container = docker('run','--pull=never','--rm','-d','--network=none','--tmpfs','/var/lib/postgresql/data','-e','POSTGRES_HOST_AUTH_METHOD=trust','postgres:17-alpine',check=True).stdout.strip()
try:
    for _ in range(100):
        if docker('exec',container,'pg_isready','-U','postgres').returncode == 0: break
        time.sleep(0.2)
    else: raise RuntimeError('Disposable PostgreSQL did not become ready.')
    def sql(query=None,file=None,check=True):
        return docker('exec','-i',container,'psql','-U','postgres','-d','postgres','-v','ON_ERROR_STOP=1','-At',input=file.read_text() if file else query,check=check)
    sql("""CREATE TABLE work_orders (order_id varchar(64) PRIMARY KEY, heater_brand varchar(120) NOT NULL, heater_model varchar(120) NOT NULL);
    INSERT INTO work_orders VALUES ('ORDER-1','Junkers','WR10'),('ORDER-2','Mademsa','Vitality 10'),('ORDER-3','Splendid','Master 11');""")
    sql(file=root/'db/work-order-equipments-v2.sql')
    assert sql('SELECT count(*) FROM work_order_equipments').stdout.strip() == '3'
    assert sql('SELECT count(*) FROM work_order_equipments WHERE position=1').stdout.strip() == '3'
    assert sql('SELECT count(*) FROM work_orders wo JOIN work_order_equipments e ON e.work_order_id=wo.order_id WHERE e.brand=wo.heater_brand AND e.model=wo.heater_model').stdout.strip() == '3'
    assert sql('SELECT count(DISTINCT equipment_id) FROM work_order_equipments').stdout.strip() == '3'
    assert sql("SELECT heater_brand||':'||heater_model FROM work_orders WHERE order_id='ORDER-1'").stdout.strip() == 'Junkers:WR10'
    replay=sql(file=root/'db/work-order-equipments-v2.sql',check=False); assert replay.returncode != 0
    assert sql('SELECT count(*) FROM work_order_equipments').stdout.strip() == '3'
    assert sql("INSERT INTO work_order_equipments VALUES ('00000000-0000-0000-0000-000000000099','ORDER-1','X','Y',NULL,NULL,NULL,1)",check=False).returncode != 0
    assert sql("INSERT INTO work_order_equipments VALUES ('00000000-0000-0000-0000-000000000098','ORDER-1','X','Y',NULL,NULL,NULL,0)",check=False).returncode != 0
    sql("DELETE FROM work_orders WHERE order_id='ORDER-3'"); assert sql("SELECT count(*) FROM work_order_equipments WHERE work_order_id='ORDER-3'").stdout.strip() == '0'
    sql("CREATE SCHEMA fresh; SET search_path TO fresh;"+(root/'db/schema-v1.sql').read_text())
    assert sql("SELECT count(*) FROM information_schema.tables WHERE table_schema='fresh'").stdout.strip() == '3'
    assert sql('SELECT count(*) FROM fresh.work_order_equipments').stdout.strip() == '0'
    print('PASS: multi-equipment migration backfills historical OTs, preserves compatibility columns, enforces invariants, refuses replay and supports fresh schema.')
finally:
    docker('rm','-f',container)
