package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.entity.WorkOrderStatus;
import com.heaterworkshop.domain.entity.ServiceType;
import com.heaterworkshop.domain.entity.LifecycleVersion;
import com.heaterworkshop.domain.entity.CustomerDecision;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaWorkOrderRepositoryAdapterTest {

    @ParameterizedTest
    @EnumSource(ServiceType.class)
    void savesEveryContractField(ServiceType type) {
        SpringDataWorkOrderRepository springRepository = mock(SpringDataWorkOrderRepository.class);
        JpaWorkOrderRepositoryAdapter adapter = new JpaWorkOrderRepositoryAdapter(springRepository);
        Instant receivedAt = Instant.parse("2026-09-03T18:30:00Z");
        WorkOrder order = new WorkOrder(new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440001"), "Maria Gonzalez",
                new CustomerContact("+56911112222"), "Bosch", "Therm 5700",
                type, type == ServiceType.REPAIR ? "Turns off" : null, receivedAt);

        adapter.save(order);

        ArgumentCaptor<JpaWorkOrderEntity> captor = ArgumentCaptor.forClass(JpaWorkOrderEntity.class);
        verify(springRepository).save(captor.capture());
        JpaWorkOrderEntity saved = captor.getValue();
        assertEquals("Maria Gonzalez", saved.getCustomerName());
        assertEquals("Bosch", saved.getHeaterBrand());
        assertEquals("Therm 5700", saved.getHeaterModel());
        assertEquals(type, saved.getServiceType());
        assertEquals(type == ServiceType.REPAIR ? "Turns off" : "", saved.getReportedIssue());
        assertEquals(receivedAt, saved.getReceivedAt());
        assertNull(saved.getCompletedAt());
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(ServiceType.class)
    void restoresAndListsCompleteOrdersNewestFirst(ServiceType type) {
        SpringDataWorkOrderRepository springRepository = mock(SpringDataWorkOrderRepository.class);
        JpaWorkOrderRepositoryAdapter adapter = new JpaWorkOrderRepositoryAdapter(springRepository);
        Instant receivedAt = Instant.parse("2026-09-03T18:30:00Z");
        Instant completedAt = Instant.parse("2026-09-03T19:30:00Z");
        JpaWorkOrderEntity entity = new JpaWorkOrderEntity("ORDER-550E8400-E29B-41D4-A716-446655440001", "Maria Gonzalez",
                "+56911112222", "Bosch", "Therm 5700", type, type == ServiceType.MAINTENANCE ? null : "Turns off",
                WorkOrderStatus.COMPLETED, type == ServiceType.MAINTENANCE ? null : "Damaged ignition sensor", receivedAt, completedAt,
                type == null ? LifecycleVersion.LEGACY : LifecycleVersion.V1,
                type == null ? WorkOrderStatus.COMPLETED : null,
                type == ServiceType.REPAIR ? CustomerDecision.APPROVED : null);
        when(springRepository.findAllByOrderByReceivedAtDesc()).thenReturn(List.of(entity));

        WorkOrder restored = adapter.findAllByReceivedAtDescending().get(0);

        assertEquals("Maria Gonzalez", restored.customerName());
        assertEquals(type == null ? ServiceType.REPAIR : type, restored.serviceType());
        assertEquals(type == ServiceType.MAINTENANCE ? "" : "Turns off", restored.reportedIssue());
        assertEquals(type == ServiceType.MAINTENANCE ? null : new Diagnosis("Damaged ignition sensor"), restored.diagnosis());
        assertEquals(WorkOrderStatus.COMPLETED, restored.status());
        assertEquals(receivedAt, restored.receivedAt());
        assertEquals(completedAt, restored.completedAt());
    }
}
