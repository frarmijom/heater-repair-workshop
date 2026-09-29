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
    private WorkOrder roundTrip(JpaWorkOrderRepositoryAdapter adapter, jakarta.persistence.EntityManager em, WorkOrder order) {
        em.getTransaction().begin(); adapter.save(order); em.getTransaction().commit(); em.clear();
        WorkOrder restored = adapter.findById(order.id()).orElseThrow();
        assertEquals(order.status(),restored.status()); assertEquals(order.serviceType(),restored.serviceType());
        assertEquals(order.reportedIssue(),restored.reportedIssue()); assertEquals(order.diagnosis(),restored.diagnosis());
        assertEquals(order.customerDecision(),restored.customerDecision()); assertEquals(order.lifecycleVersion(),restored.lifecycleVersion());
        assertEquals(order.legacyStatus(),restored.legacyStatus());
        return restored;
    }
}
