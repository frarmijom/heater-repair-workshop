package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.*;
import com.heaterworkshop.domain.valueobject.*;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class JpaWorkOrderPersistenceTest {
    @Test void roundTripsServiceSpecificStatesAndLegacyProvenance() {
        var configuration = new Configuration().addAnnotatedClass(JpaWorkOrderEntity.class)
                .addAnnotatedClass(JpaWorkOrderEquipmentEntity.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try(var factory = configuration.buildSessionFactory(); var em = factory.createEntityManager()) {
            var adapter = new JpaWorkOrderRepositoryAdapter(new JpaRepositoryFactory(em).getRepository(SpringDataWorkOrderRepository.class));
            for(ServiceType type : ServiceType.values()) {
                var order = new WorkOrder(new WorkOrderId("ORDER-" + UUID.randomUUID().toString().toUpperCase()),
                        "Maria", new CustomerContact("+56911112222"),"Bosch","Therm",type,
                        type == ServiceType.REPAIR ? "Issue" : null, Instant.now());
                roundTrip(adapter,em,order);
                if(type == ServiceType.REPAIR) {
                    order.beginDiagnosis(); roundTrip(adapter,em,order);
                    order.recordDiagnosis(new Diagnosis("Sensor")); roundTrip(adapter,em,order);
                    order.completeDiagnosis(); roundTrip(adapter,em,order);
                    order.approveRepair(false);
                } else order.waitForParts();
                roundTrip(adapter,em,order);
                order.startWork(); roundTrip(adapter,em,order);
                order.complete(); roundTrip(adapter,em,order);
            }
            var legacy = WorkOrder.restore(new WorkOrderId("ORDER-"+UUID.randomUUID().toString().toUpperCase()),"Legacy",new CustomerContact("+56911112222"),
                    "Bosch","Therm",ServiceType.REPAIR,"Issue",WorkOrderStatus.IN_PROGRESS,new Diagnosis("Old diagnosis"),
                    Instant.now(),null,LifecycleVersion.LEGACY,WorkOrderStatus.IN_PROGRESS,null);
            WorkOrder loaded = roundTrip(adapter,em,legacy);
            loaded.complete(); loaded = roundTrip(adapter,em,loaded);
            assertNull(loaded.customerDecision()); assertEquals(LifecycleVersion.LEGACY,loaded.lifecycleVersion());
            assertEquals(WorkOrderStatus.IN_PROGRESS,loaded.legacyStatus());
            var rejected = new WorkOrder(new WorkOrderId("ORDER-" + UUID.randomUUID().toString().toUpperCase()),
                    "Maria", new CustomerContact("+56911112222"), "Bosch", "Therm", ServiceType.REPAIR, "Issue", Instant.now());
            rejected.beginDiagnosis(); rejected.recordDiagnosis(new Diagnosis("Sensor"));
            rejected.completeDiagnosis(); rejected.rejectRepair();
            var persistedRejection = roundTrip(adapter, em, rejected);
            assertEquals(WorkOrderStatus.NOT_APPROVED, persistedRejection.status());
            assertNull(persistedRejection.completedAt());
            assertEquals(4,adapter.findAllByReceivedAtDescending().size());
        }
    }
    @Test void roundTripsMultipleEquipmentsWithOptionalFieldsAndStableOrder() {
        var configuration = new Configuration().addAnnotatedClass(JpaWorkOrderEntity.class)
                .addAnnotatedClass(JpaWorkOrderEquipmentEntity.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try(var factory = configuration.buildSessionFactory(); var em = factory.createEntityManager()) {
            var adapter = new JpaWorkOrderRepositoryAdapter(new JpaRepositoryFactory(em).getRepository(SpringDataWorkOrderRepository.class));
            var equipments = java.util.List.of(
                    new WorkOrderEquipment(new EquipmentId(UUID.fromString("550e8400-e29b-41d4-a716-446655440012")),
                            "Mademsa", "Vitality 11", null, null, null, 2),
                    new WorkOrderEquipment(new EquipmentId(UUID.fromString("550e8400-e29b-41d4-a716-446655440011")),
                            "Junkers", "WR10", "10 L", "SN-100", "Equipo principal", 1));
            var order = new WorkOrder(new WorkOrderId("ORDER-" + UUID.randomUUID().toString().toUpperCase()),
                    "Maria", new CustomerContact("+56911112222"), equipments, ServiceType.MAINTENANCE, null, Instant.now());

            WorkOrder restored = roundTrip(adapter, em, order);

            assertEquals(2, restored.equipments().size());
            assertEquals(1, restored.equipments().get(0).position());
            assertEquals("Junkers", restored.equipments().get(0).brand());
            assertEquals("10 L", restored.equipments().get(0).capacity());
            assertEquals("SN-100", restored.equipments().get(0).serialNumber());
            assertEquals("Equipo principal", restored.equipments().get(0).notes());
            assertEquals(2, restored.equipments().get(1).position());
            assertEquals("Mademsa", restored.equipments().get(1).brand());
        }
    }

    @Test
    void roundTripsEquipmentLifecycleV2WithoutFabricatingHistoricalLifecycle() {
        var configuration = new Configuration().addAnnotatedClass(JpaWorkOrderEntity.class)
                .addAnnotatedClass(JpaWorkOrderEquipmentEntity.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop");

        try (var factory = configuration.buildSessionFactory(); var em = factory.createEntityManager()) {
            var adapter = new JpaWorkOrderRepositoryAdapter(
                    new JpaRepositoryFactory(em).getRepository(SpringDataWorkOrderRepository.class));

            Instant receivedAt = Instant.parse("2026-10-01T15:00:00Z");
            Instant completedAt = Instant.parse("2026-10-01T17:00:00Z");

            var lifecycle = WorkOrderEquipmentLifecycle.create(
                    EquipmentIntakeRoute.DIAGNOSIS_REQUIRED,
                    "No enciende",
                    receivedAt);
            lifecycle.beginDiagnosis();
            lifecycle.recordDiagnosis(new Diagnosis("Sensor de encendido defectuoso"));
            lifecycle.completeDiagnosis();
            lifecycle.approve(true);
            lifecycle.complete(completedAt);

            var v2Equipment = new WorkOrderEquipment(
                    new EquipmentId(UUID.fromString("550e8400-e29b-41d4-a716-446655440099")),
                    EquipmentType.CALEFONT,
                    "Junkers",
                    "WR10",
                    "10 L",
                    "SN-V2-001",
                    "Equipo V2",
                    1,
                    lifecycle);

            var order = WorkOrder.createV2(
                    new WorkOrderId("ORDER-" + UUID.randomUUID().toString().toUpperCase()),
                    "Maria",
                    new CustomerContact("+56911112222"),
                    java.util.List.of(v2Equipment),
                    receivedAt);

            WorkOrder restored = roundTrip(adapter, em, order);

            var restoredEquipment = restored.equipments().get(0);
            assertEquals(EquipmentType.CALEFONT, restoredEquipment.type());
            assertNotNull(restoredEquipment.lifecycle());
            assertEquals(EquipmentIntakeRoute.DIAGNOSIS_REQUIRED,
                    restoredEquipment.lifecycle().intakeRoute());
            assertEquals(WorkOrderStatus.COMPLETED, restoredEquipment.lifecycle().status());
            assertEquals("No enciende", restoredEquipment.lifecycle().reportedIssue());
            assertEquals(new Diagnosis("Sensor de encendido defectuoso"),
                    restoredEquipment.lifecycle().diagnosis());
            assertEquals(CustomerDecision.APPROVED,
                    restoredEquipment.lifecycle().customerDecision());
            assertEquals(receivedAt, restoredEquipment.lifecycle().receivedAt());
            assertEquals(completedAt, restoredEquipment.lifecycle().completedAt());

            assertEquals(LifecycleVersion.V2, restored.lifecycleVersion());
            assertNull(restored.serviceType());
            assertNull(restored.reportedIssue());
            assertNull(restored.status());
            assertNull(restored.diagnosis());
            assertNull(restored.completedAt());
            assertNull(restored.customerDecision());
        }
    }

    private WorkOrder roundTrip(JpaWorkOrderRepositoryAdapter adapter, jakarta.persistence.EntityManager em, WorkOrder order) {
        em.getTransaction().begin(); adapter.save(order); em.getTransaction().commit(); em.clear();
        WorkOrder restored = adapter.findById(order.id()).orElseThrow();
        assertEquals(order.status(),restored.status()); assertEquals(order.serviceType(),restored.serviceType());
        assertEquals(order.reportedIssue(),restored.reportedIssue()); assertEquals(order.diagnosis(),restored.diagnosis());
        assertEquals(order.customerDecision(),restored.customerDecision()); assertEquals(order.lifecycleVersion(),restored.lifecycleVersion());
        assertEquals(order.legacyStatus(),restored.legacyStatus());
        assertEquals(order.equipments().size(), restored.equipments().size());
        for (int i = 0; i < order.equipments().size(); i++) {
            assertEquals(order.equipments().get(i).id(), restored.equipments().get(i).id());
            assertEquals(order.equipments().get(i).brand(), restored.equipments().get(i).brand());
            assertEquals(order.equipments().get(i).model(), restored.equipments().get(i).model());
            assertEquals(order.equipments().get(i).position(), restored.equipments().get(i).position());
        }
        return restored;
    }
}
