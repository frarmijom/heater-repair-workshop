package com.heaterworkshop.application.service;

import com.heaterworkshop.domain.entity.ServiceType;
import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.entity.WorkOrderEquipment;
import com.heaterworkshop.domain.inventory.CatalogNotFoundException;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import com.heaterworkshop.domain.service.ServiceCatalogItem;
import com.heaterworkshop.domain.service.ServiceCatalogRepository;
import com.heaterworkshop.domain.service.WorkOrderEquipmentService;
import com.heaterworkshop.domain.service.WorkOrderEquipmentServiceRepository;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkOrderEquipmentServiceUseCasesTest {
    private WorkOrderRepository workOrders;
    private ServiceCatalogRepository services;
    private WorkOrderEquipmentServiceRepository assignments;
    private WorkOrderEquipmentServiceUseCases useCases;
    private WorkOrderId orderId;
    private EquipmentId equipmentId;

    @BeforeEach
    void setUp() {
        workOrders = mock(WorkOrderRepository.class);
        services = mock(ServiceCatalogRepository.class);
        assignments = mock(WorkOrderEquipmentServiceRepository.class);
        useCases = new WorkOrderEquipmentServiceUseCases(workOrders, services, assignments);
        orderId = new WorkOrderId("ORDER-550E8400-E29B-41D4-A716-446655440000");
        equipmentId = new EquipmentId(UUID.randomUUID());
        WorkOrder order = new WorkOrder(orderId, "Cliente", new CustomerContact("+56911112222"),
                List.of(new WorkOrderEquipment(equipmentId, "Junkers", "WR11", null, null, null, 1)),
                ServiceType.MAINTENANCE, "", Instant.now());
        when(workOrders.findById(orderId)).thenReturn(Optional.of(order));
    }

    @Test
    void replacesAndReadsServicesForEquipment() {
        UUID serviceId = UUID.randomUUID();
        when(services.findById(serviceId)).thenReturn(Optional.of(mock(ServiceCatalogItem.class)));
        var expected = List.of(new WorkOrderEquipmentService(orderId, equipmentId, serviceId));
        when(assignments.findByWorkOrderIdAndEquipmentId(orderId, equipmentId)).thenReturn(expected);

        assertEquals(expected, useCases.replace(orderId, equipmentId, List.of(serviceId)));
        verify(assignments).replace(orderId, equipmentId, expected);
        assertEquals(expected, useCases.get(orderId, equipmentId));
    }

    @Test
    void rejectsEquipmentFromAnotherOrder() {
        var other = new EquipmentId(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> useCases.get(orderId, other));
        verifyNoInteractions(assignments);
    }

    @Test
    void rejectsDuplicateServicesWithoutReplacingExistingAssignments() {
        UUID serviceId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> useCases.replace(orderId, equipmentId, List.of(serviceId, serviceId)));
        verify(assignments, never()).replace(any(), any(), anyList());
    }

    @Test
    void rejectsUnknownServiceWithoutReplacingExistingAssignments() {
        UUID serviceId = UUID.randomUUID();
        when(services.findById(serviceId)).thenReturn(Optional.empty());
        assertThrows(CatalogNotFoundException.class,
                () -> useCases.replace(orderId, equipmentId, List.of(serviceId)));
        verify(assignments, never()).replace(any(), any(), anyList());
    }

    @Test
    void emptyReplacementClearsServices() {
        when(assignments.findByWorkOrderIdAndEquipmentId(orderId, equipmentId)).thenReturn(List.of());
        assertTrue(useCases.replace(orderId, equipmentId, List.of()).isEmpty());
        verify(assignments).replace(orderId, equipmentId, List.of());
    }
}
