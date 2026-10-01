package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.service.WorkOrderEquipmentService;
import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaWorkOrderEquipmentServicePersistenceTest {

    @Test
    void persistsAndReplacesEquipmentServiceAssignments() {
        var configuration = new Configuration()
                .addAnnotatedClass(JpaWorkOrderEquipmentServiceEntity.class)
                .setProperty(
                        "hibernate.connection.url",
                        "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop");

        try (var factory = configuration.buildSessionFactory();
             var em = factory.createEntityManager()) {

            var springRepository = new JpaRepositoryFactory(em)
                    .getRepository(SpringDataWorkOrderEquipmentServiceRepository.class);

            var adapter =
                    new JpaWorkOrderEquipmentServiceRepositoryAdapter(springRepository);

            WorkOrderId workOrderId =
                    new WorkOrderId(
                            "ORDER-" + UUID.randomUUID().toString().toUpperCase());

            EquipmentId equipmentId =
                    new EquipmentId(UUID.randomUUID());

            UUID firstServiceId = UUID.randomUUID();
            UUID secondServiceId = UUID.randomUUID();

            var first =
                    new WorkOrderEquipmentService(
                            workOrderId,
                            equipmentId,
                            firstServiceId);

            var second =
                    new WorkOrderEquipmentService(
                            workOrderId,
                            equipmentId,
                            secondServiceId);

            em.getTransaction().begin();
            adapter.replace(workOrderId, equipmentId, List.of(first, second));
            em.getTransaction().commit();
            em.clear();

            var stored =
                    adapter.findByWorkOrderIdAndEquipmentId(
                            workOrderId,
                            equipmentId);

            assertEquals(2, stored.size());
            assertTrue(stored.contains(first));
            assertTrue(stored.contains(second));

            em.getTransaction().begin();
            adapter.replace(workOrderId, equipmentId, List.of(second));
            em.getTransaction().commit();
            em.clear();

            var replaced =
                    adapter.findByWorkOrderIdAndEquipmentId(
                            workOrderId,
                            equipmentId);

            assertEquals(List.of(second), replaced);

            em.getTransaction().begin();
            adapter.replace(workOrderId, equipmentId, List.of());
            em.getTransaction().commit();
            em.clear();

            assertTrue(
                    adapter.findByWorkOrderIdAndEquipmentId(
                            workOrderId,
                            equipmentId).isEmpty());
        }
    }

    @Test
    void allowsSameServiceForDifferentEquipments() {
        var configuration = new Configuration()
                .addAnnotatedClass(JpaWorkOrderEquipmentServiceEntity.class)
                .setProperty(
                        "hibernate.connection.url",
                        "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop");

        try (var factory = configuration.buildSessionFactory();
             var em = factory.createEntityManager()) {

            var springRepository = new JpaRepositoryFactory(em)
                    .getRepository(SpringDataWorkOrderEquipmentServiceRepository.class);

            var adapter =
                    new JpaWorkOrderEquipmentServiceRepositoryAdapter(springRepository);

            WorkOrderId workOrderId =
                    new WorkOrderId(
                            "ORDER-" + UUID.randomUUID().toString().toUpperCase());

            EquipmentId firstEquipment =
                    new EquipmentId(UUID.randomUUID());

            EquipmentId secondEquipment =
                    new EquipmentId(UUID.randomUUID());

            UUID serviceId = UUID.randomUUID();

            var firstAssignment =
                    new WorkOrderEquipmentService(
                            workOrderId,
                            firstEquipment,
                            serviceId);

            var secondAssignment =
                    new WorkOrderEquipmentService(
                            workOrderId,
                            secondEquipment,
                            serviceId);

            em.getTransaction().begin();
            adapter.replace(
                    workOrderId,
                    firstEquipment,
                    List.of(firstAssignment));

            adapter.replace(
                    workOrderId,
                    secondEquipment,
                    List.of(secondAssignment));
            em.getTransaction().commit();
            em.clear();

            assertEquals(
                    List.of(firstAssignment),
                    adapter.findByWorkOrderIdAndEquipmentId(
                            workOrderId,
                            firstEquipment));

            assertEquals(
                    List.of(secondAssignment),
                    adapter.findByWorkOrderIdAndEquipmentId(
                            workOrderId,
                            secondEquipment));
        }
    }
}
