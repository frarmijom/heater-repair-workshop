package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.entity.RepairStatus;
import com.heaterworkshop.domain.entity.ServiceType;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
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

class JpaRepairOrderRepositoryAdapterTest {

    @ParameterizedTest
    @EnumSource(ServiceType.class)
    void savesEveryContractField(ServiceType type) {
        SpringDataRepairOrderRepository springRepository = mock(SpringDataRepairOrderRepository.class);
        JpaRepairOrderRepositoryAdapter adapter = new JpaRepairOrderRepositoryAdapter(springRepository);
        Instant receivedAt = Instant.parse("2026-09-03T18:30:00Z");
        RepairOrder order = new RepairOrder(new RepairOrderId("ORDER-550E8400-E29B-41D4-A716-446655440001"), "Maria Gonzalez",
                new CustomerContact("+56911112222"), "Bosch", "Therm 5700",
                type, type == ServiceType.REPAIR ? "Turns off" : null, receivedAt);

        adapter.save(order);

        ArgumentCaptor<JpaRepairOrderEntity> captor = ArgumentCaptor.forClass(JpaRepairOrderEntity.class);
        verify(springRepository).save(captor.capture());
        JpaRepairOrderEntity saved = captor.getValue();
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
        SpringDataRepairOrderRepository springRepository = mock(SpringDataRepairOrderRepository.class);
        JpaRepairOrderRepositoryAdapter adapter = new JpaRepairOrderRepositoryAdapter(springRepository);
        Instant receivedAt = Instant.parse("2026-09-03T18:30:00Z");
        Instant completedAt = Instant.parse("2026-09-03T19:30:00Z");
        JpaRepairOrderEntity entity = new JpaRepairOrderEntity("ORDER-550E8400-E29B-41D4-A716-446655440001", "Maria Gonzalez",
                "+56911112222", "Bosch", "Therm 5700", type, type == ServiceType.MAINTENANCE ? null : "Turns off",
                RepairStatus.COMPLETED, "Damaged ignition sensor", receivedAt, completedAt);
        when(springRepository.findAllByOrderByReceivedAtDesc()).thenReturn(List.of(entity));

        RepairOrder restored = adapter.findAllByReceivedAtDescending().get(0);

        assertEquals("Maria Gonzalez", restored.customerName());
        assertEquals(type == null ? ServiceType.REPAIR : type, restored.serviceType());
        assertEquals(type == ServiceType.MAINTENANCE ? "" : "Turns off", restored.reportedIssue());
        assertEquals(new Diagnosis("Damaged ignition sensor"), restored.diagnosis());
        assertEquals(RepairStatus.COMPLETED, restored.status());
        assertEquals(receivedAt, restored.receivedAt());
        assertEquals(completedAt, restored.completedAt());
    }
}
