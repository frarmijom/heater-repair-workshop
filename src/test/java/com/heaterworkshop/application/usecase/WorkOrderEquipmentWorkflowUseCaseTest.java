package com.heaterworkshop.application.usecase;

import com.heaterworkshop.domain.entity.EquipmentIntakeRoute;
import com.heaterworkshop.domain.entity.EquipmentType;
import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.entity.WorkOrderEquipment;
import com.heaterworkshop.domain.entity.WorkOrderEquipmentLifecycle;
import com.heaterworkshop.domain.entity.WorkOrderStatus;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkOrderEquipmentWorkflowUseCaseTest {

    @Test
    void rejectsUnknownWorkOrder() {
        WorkOrderRepository repository = mock(WorkOrderRepository.class);
        WorkOrderId orderId =
                new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440404");
        EquipmentId equipmentId =
                new EquipmentId(UUID.fromString("550e8400-e29b-41d4-a716-446655440011"));

        when(repository.findById(orderId)).thenReturn(Optional.empty());

        WorkOrderEquipmentWorkflowUseCase useCase =
                new WorkOrderEquipmentWorkflowUseCase(repository);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.heaterworkshop.domain.exception.WorkOrderNotFoundException.class,
                () -> useCase.beginDiagnosis(orderId, equipmentId));

        verify(repository, org.mockito.Mockito.never()).save(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsEquipmentThatDoesNotBelongToWorkOrder() {
        WorkOrder order = newV2Order();
        WorkOrderRepository repository = mock(WorkOrderRepository.class);
        EquipmentId unknownEquipmentId =
                new EquipmentId(UUID.fromString("550e8400-e29b-41d4-a716-446655440099"));

        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        WorkOrderEquipmentWorkflowUseCase useCase =
                new WorkOrderEquipmentWorkflowUseCase(repository);

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> useCase.beginDiagnosis(order.id(), unknownEquipmentId));

        verify(repository, org.mockito.Mockito.never()).save(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsV1WorkOrder() {
        WorkOrderRepository repository = mock(WorkOrderRepository.class);
        WorkOrder order = new WorkOrder(
                new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440020"),
                "Maria Gonzalez",
                new CustomerContact("+56911112222"),
                "Bosch",
                "Therm 5700",
                com.heaterworkshop.domain.entity.ServiceType.REPAIR,
                "Does not ignite",
                Instant.parse("2026-10-03T12:00:00Z"));

        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        WorkOrderEquipmentWorkflowUseCase useCase =
                new WorkOrderEquipmentWorkflowUseCase(repository);

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> useCase.beginDiagnosis(
                        order.id(),
                        order.equipments().get(0).id()));

        verify(repository, org.mockito.Mockito.never()).save(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void runsDiagnosisWorkflowOnOneEquipmentAndPersistsTheWorkOrder() {
        WorkOrderRepository repository = mock(WorkOrderRepository.class);
        WorkOrderId orderId =
                new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440010");
        EquipmentId equipmentId =
                new EquipmentId(UUID.fromString("550e8400-e29b-41d4-a716-446655440011"));
        Instant receivedAt = Instant.parse("2026-10-03T12:00:00Z");

        WorkOrderEquipmentLifecycle lifecycle = WorkOrderEquipmentLifecycle.create(
                EquipmentIntakeRoute.DIAGNOSIS_REQUIRED,
                "Does not ignite",
                receivedAt);

        WorkOrderEquipment equipment = new WorkOrderEquipment(
                equipmentId,
                EquipmentType.CALEFONT,
                "Bosch",
                "Therm 5700",
                "10 L",
                null,
                null,
                1,
                lifecycle);

        WorkOrder order = WorkOrder.createV2(
                orderId,
                "Maria Gonzalez",
                new CustomerContact("+56911112222"),
                List.of(equipment),
                receivedAt);

        when(repository.findById(orderId)).thenReturn(Optional.of(order));

        WorkOrderEquipmentWorkflowUseCase useCase =
                new WorkOrderEquipmentWorkflowUseCase(repository);

        WorkOrder afterBegin = useCase.beginDiagnosis(orderId, equipmentId);

        assertEquals(order, afterBegin);
        assertEquals(WorkOrderStatus.DIAGNOSIS, lifecycle.status());
        verify(repository).save(order);

        WorkOrder afterRecord = useCase.recordDiagnosis(
                orderId,
                equipmentId,
                new Diagnosis("Ignition electrode damaged"));

        assertEquals(order, afterRecord);
        assertEquals("Ignition electrode damaged", lifecycle.diagnosis().value());

        WorkOrder afterComplete = useCase.completeDiagnosis(orderId, equipmentId);

        assertEquals(order, afterComplete);
        assertEquals(WorkOrderStatus.WAITING_CUSTOMER, lifecycle.status());
        verify(repository, times(3)).save(order);
    }

    private WorkOrder newV2Order() {
        WorkOrderId orderId =
                new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440030");
        EquipmentId equipmentId =
                new EquipmentId(UUID.fromString("550e8400-e29b-41d4-a716-446655440031"));
        Instant receivedAt = Instant.parse("2026-10-03T12:00:00Z");

        WorkOrderEquipmentLifecycle lifecycle = WorkOrderEquipmentLifecycle.create(
                EquipmentIntakeRoute.DIAGNOSIS_REQUIRED,
                "Does not ignite",
                receivedAt);

        WorkOrderEquipment equipment = new WorkOrderEquipment(
                equipmentId,
                EquipmentType.CALEFONT,
                "Bosch",
                "Therm 5700",
                "10 L",
                null,
                null,
                1,
                lifecycle);

        return WorkOrder.createV2(
                orderId,
                "Maria Gonzalez",
                new CustomerContact("+56911112222"),
                List.of(equipment),
                receivedAt);
    }

    @Test
    void continuesApprovedDiagnosisEquipmentThroughWorkCompletion() {
        WorkOrder order = newV2Order();
        WorkOrderRepository repository = mock(WorkOrderRepository.class);
        when(repository.findById(order.id())).thenReturn(Optional.of(order));

        WorkOrderEquipmentWorkflowUseCase useCase =
                new WorkOrderEquipmentWorkflowUseCase(repository);

        EquipmentId equipmentId = order.equipments().get(0).id();

        useCase.beginDiagnosis(order.id(), equipmentId);
        useCase.recordDiagnosis(
                order.id(),
                equipmentId,
                new Diagnosis("Ignition electrode damaged"));
        useCase.completeDiagnosis(order.id(), equipmentId);
        useCase.approve(order.id(), equipmentId, false);

        org.junit.jupiter.api.Assertions.assertEquals(
                WorkOrderStatus.WAITING_PARTS,
                order.equipments().get(0).lifecycle().status());

        useCase.startWork(order.id(), equipmentId);

        org.junit.jupiter.api.Assertions.assertEquals(
                WorkOrderStatus.IN_PROGRESS,
                order.equipments().get(0).lifecycle().status());

        Instant completedAt = Instant.parse("2026-10-03T16:00:00Z");
        useCase.complete(order.id(), equipmentId, completedAt);

        org.junit.jupiter.api.Assertions.assertEquals(
                WorkOrderStatus.COMPLETED,
                order.equipments().get(0).lifecycle().status());

        org.junit.jupiter.api.Assertions.assertEquals(
                completedAt,
                order.equipments().get(0).lifecycle().completedAt());
    }

    @Test
    void supportsDirectServiceRouteAndRejectionFlow() {
        WorkOrderRepository repository = mock(WorkOrderRepository.class);

        WorkOrder directOrder = newV2DirectServiceOrder();
        when(repository.findById(directOrder.id()))
                .thenReturn(Optional.of(directOrder));

        WorkOrderEquipmentWorkflowUseCase useCase =
                new WorkOrderEquipmentWorkflowUseCase(repository);

        EquipmentId directEquipmentId = directOrder.equipments().get(0).id();

        useCase.waitForParts(directOrder.id(), directEquipmentId);

        org.junit.jupiter.api.Assertions.assertEquals(
                WorkOrderStatus.WAITING_PARTS,
                directOrder.equipments().get(0).lifecycle().status());

        useCase.startWork(directOrder.id(), directEquipmentId);

        org.junit.jupiter.api.Assertions.assertEquals(
                WorkOrderStatus.IN_PROGRESS,
                directOrder.equipments().get(0).lifecycle().status());

        WorkOrder diagnosisOrder = newV2Order();
        when(repository.findById(diagnosisOrder.id()))
                .thenReturn(Optional.of(diagnosisOrder));

        EquipmentId diagnosisEquipmentId =
                diagnosisOrder.equipments().get(0).id();

        useCase.beginDiagnosis(diagnosisOrder.id(), diagnosisEquipmentId);
        useCase.recordDiagnosis(
                diagnosisOrder.id(),
                diagnosisEquipmentId,
                new Diagnosis("Ignition electrode damaged"));
        useCase.completeDiagnosis(diagnosisOrder.id(), diagnosisEquipmentId);
        useCase.reject(diagnosisOrder.id(), diagnosisEquipmentId);

        org.junit.jupiter.api.Assertions.assertEquals(
                WorkOrderStatus.NOT_APPROVED,
                diagnosisOrder.equipments().get(0).lifecycle().status());
    }

    private WorkOrder newV2DirectServiceOrder() {
        WorkOrderId orderId =
                new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440040");
        EquipmentId equipmentId =
                new EquipmentId(
                        UUID.fromString("550e8400-e29b-41d4-a716-446655440041"));
        Instant receivedAt = Instant.parse("2026-10-03T12:00:00Z");

        WorkOrderEquipmentLifecycle lifecycle =
                WorkOrderEquipmentLifecycle.create(
                        EquipmentIntakeRoute.DIRECT_SERVICE,
                        null,
                        receivedAt);

        WorkOrderEquipment equipment = new WorkOrderEquipment(
                equipmentId,
                EquipmentType.CALEFONT,
                "Bosch",
                "Therm 5700",
                "10 L",
                null,
                null,
                1,
                lifecycle);

        return WorkOrder.createV2(
                orderId,
                "Maria Gonzalez",
                new CustomerContact("+56911112222"),
                List.of(equipment),
                receivedAt);
    }

}
