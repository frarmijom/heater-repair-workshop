package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.entity.RepairStatus;
import com.heaterworkshop.domain.entity.ServiceType;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JpaRepairOrderSchemaUpdateTest {
    @Test
    void updatesPopulatedLegacySchemaAndRoundTripsBothServices() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        String historicalId = "ORDER-550E8400-E29B-41D4-A716-446655440000";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            // Model the pre-I6 table: no service_type and a NOT NULL reported_issue.
            statement.execute("""
                    create table repair_orders (
                        order_id varchar(64) primary key, customer_name varchar(200) not null,
                        customer_contact varchar(16) not null, heater_brand varchar(120) not null,
                        heater_model varchar(120) not null, reported_issue varchar(2000) not null,
                        status varchar(32) not null, diagnosis varchar(1000),
                        received_at timestamp with time zone not null, completed_at timestamp with time zone)
                    """);
            statement.execute("""
                    insert into repair_orders values ('%s', 'Historical customer', '+56911112222',
                        'Bosch', 'Therm', 'No enciende', 'RECEIVED', null,
                        TIMESTAMP WITH TIME ZONE '2026-09-03 18:30:00+00', null)
                    """.formatted(historicalId));
            var configuration = new Configuration().addAnnotatedClass(JpaRepairOrderEntity.class)
                    .setProperty("hibernate.connection.url", url)
                    .setProperty("hibernate.connection.username", "sa")
                    .setProperty("hibernate.connection.password", "")
                    .setProperty("hibernate.hbm2ddl.auto", "update")
                    .setProperty("hibernate.hbm2ddl.halt_on_error", "true");
            try (var factory = configuration.buildSessionFactory(); var em = factory.createEntityManager()) {
                var springRepository = new JpaRepositoryFactory(em).getRepository(SpringDataRepairOrderRepository.class);
                var adapter = new JpaRepairOrderRepositoryAdapter(springRepository);
                var historical = adapter.findById(new RepairOrderId(historicalId)).orElseThrow();
                assertEquals(ServiceType.REPAIR, historical.serviceType());
                assertEquals("No enciende", historical.reportedIssue());
                assertEquals("Historical customer", historical.customerName());
                try (var rows = statement.executeQuery("select service_type from repair_orders")) {
                    assertTrue(rows.next());
                    assertNull(rows.getString(1)); // Reading does not backfill or change the old row.
                }
                try (var columns = connection.getMetaData().getColumns(null, null, "REPAIR_ORDERS", "REPORTED_ISSUE")) {
                    assertTrue(columns.next());
                    assertEquals(java.sql.DatabaseMetaData.columnNoNulls, columns.getInt("NULLABLE"));
                }
                for (ServiceType type : ServiceType.values()) {
                    var order = new RepairOrder(new RepairOrderId("ORDER-" + UUID.randomUUID().toString().toUpperCase()),
                            "Maria", new CustomerContact("+56911112222"), "Bosch", "Therm", type,
                            type == ServiceType.REPAIR ? "  No enciende  " : null,
                            Instant.parse("2026-09-03T19:30:00Z"));
                    for (RepairStatus status : RepairStatus.values()) {
                        if (status == RepairStatus.IN_PROGRESS) order.start(new Diagnosis("Inspection"));
                        if (status == RepairStatus.COMPLETED) order.complete(order.receivedAt().plusSeconds(60));
                        em.getTransaction().begin();
                        adapter.save(order);
                        em.getTransaction().commit();
                        em.clear();
                        var restored = adapter.findById(order.id()).orElseThrow();
                        assertEquals(type, restored.serviceType());
                        assertEquals(order.reportedIssue(), restored.reportedIssue());
                        assertEquals(status, restored.status());
                        assertEquals(order.diagnosis(), restored.diagnosis());
                        assertEquals(order.receivedAt(), restored.receivedAt());
                        assertEquals(order.completedAt(), restored.completedAt());
                    }
                    try (var rows = statement.executeQuery("select service_type, reported_issue from repair_orders where order_id='" + order.id().value() + "'")) {
                        assertTrue(rows.next());
                        assertEquals(type.name(), rows.getString(1));
                        assertEquals(order.reportedIssue(), rows.getString(2));
                    }
                }
                assertEquals(3, adapter.findAllByReceivedAtDescending().size());
                assertEquals(ServiceType.REPAIR, adapter.findAllByReceivedAtDescending().get(2).serviceType());
            }
        }
    }
}
