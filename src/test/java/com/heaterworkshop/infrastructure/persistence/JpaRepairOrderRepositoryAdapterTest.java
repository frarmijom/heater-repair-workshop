package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.entity.RepairStatus;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaRepairOrderRepositoryAdapterTest {

    @Test
    void savesEveryContractField() {
        SpringDataRepairOrderRepository springRepository = mock(SpringDataRepairOrderRepository.class);
        JpaRepairOrderRepositoryAdapter adapter = new JpaRepairOrderRepositoryAdapter(springRepository);
        Instant receivedAt = Instant.parse("2026-09-03T18:30:00Z");
        RepairOrder order = new RepairOrder(new RepairOrderId("ORDER-550E8400-E29B-41D4-A716-446655440001"), "Maria Gonzalez",
                new CustomerContact("+56911112222"), "Bosch", "Therm 5700",
                "Turns off", receivedAt);

        adapter.save(order);

        ArgumentCaptor<JpaRepairOrderEntity> captor = ArgumentCaptor.forClass(JpaRepairOrderEntity.class);
        verify(springRepository).save(captor.capture());
        JpaRepairOrderEntity saved = captor.getValue();
        assertEquals("Maria Gonzalez", saved.getCustomerName());
        assertEquals("Bosch", saved.getHeaterBrand());
        assertEquals("Therm 5700", saved.getHeaterModel());
        assertEquals("Turns off", saved.getReportedIssue());
        assertEquals(receivedAt, saved.getReceivedAt());
        assertNull(saved.getCompletedAt());
    }

    @Test
    void restoresAndListsCompleteOrdersNewestFirst() {
        SpringDataRepairOrderRepository springRepository = mock(SpringDataRepairOrderRepository.class);
        JpaRepairOrderRepositoryAdapter adapter = new JpaRepairOrderRepositoryAdapter(springRepository);
        Instant receivedAt = Instant.parse("2026-09-03T18:30:00Z");
        Instant completedAt = Instant.parse("2026-09-03T19:30:00Z");
        JpaRepairOrderEntity entity = new JpaRepairOrderEntity("ORDER-550E8400-E29B-41D4-A716-446655440001", "Maria Gonzalez",
                "+56911112222", "Bosch", "Therm 5700", "Turns off",
                RepairStatus.COMPLETED, "Damaged ignition sensor", receivedAt, completedAt);
        when(springRepository.findAllByOrderByReceivedAtDesc()).thenReturn(List.of(entity));

        RepairOrder restored = adapter.findAllByReceivedAtDescending().get(0);

        assertEquals("Maria Gonzalez", restored.customerName());
        assertEquals(new Diagnosis("Damaged ignition sensor"), restored.diagnosis());
        assertEquals(RepairStatus.COMPLETED, restored.status());
        assertEquals(receivedAt, restored.receivedAt());
        assertEquals(completedAt, restored.completedAt());
    }
}
