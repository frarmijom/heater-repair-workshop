package com.heaterworkshop.infrastructure.web;

import com.heaterworkshop.application.service.WorkOrderEquipmentServiceUseCases;
import com.heaterworkshop.application.usecase.CompleteWorkUseCase;
import com.heaterworkshop.application.usecase.CreateWorkOrderUseCase;
import com.heaterworkshop.application.usecase.GetWorkOrderUseCase;
import com.heaterworkshop.application.usecase.ListWorkOrdersUseCase;
import com.heaterworkshop.application.usecase.StartWorkUseCase;
import com.heaterworkshop.application.usecase.WorkOrderEquipmentWorkflowUseCase;
import com.heaterworkshop.application.usecase.WorkOrderWorkflowUseCase;
import com.heaterworkshop.domain.entity.EquipmentIntakeRoute;
import com.heaterworkshop.domain.entity.EquipmentType;
import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.entity.WorkOrderEquipment;
import com.heaterworkshop.domain.entity.WorkOrderEquipmentLifecycle;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import com.heaterworkshop.infrastructure.persistence.InMemoryWorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkOrderEquipmentWorkflowControllerTest {

    @Test
    void exposesDiagnosisLifecyclePerEquipment() throws Exception {
        InMemoryWorkOrderRepository repository = new InMemoryWorkOrderRepository();
        WorkOrder order = newV2Order(EquipmentIntakeRoute.DIAGNOSIS_REQUIRED);
        repository.save(order);

        MockMvc mockMvc = mockMvc(repository);
        String orderId = order.id().value();
        UUID equipmentId = order.equipments().get(0).id().value();

        mockMvc.perform(patch(
                        "/api/work-orders/{id}/equipments/{equipmentId}/diagnosis/begin",
                        orderId, equipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifecycleVersion").value("V2"))
                .andExpect(jsonPath("$.equipments[0].type").value("CALEFONT"))
                .andExpect(jsonPath("$.equipments[0].intakeRoute").value("DIAGNOSIS_REQUIRED"))
                .andExpect(jsonPath("$.equipments[0].status").value("DIAGNOSIS"))
                .andExpect(jsonPath("$.equipments[0].reportedIssue").value("Does not ignite"));

        mockMvc.perform(patch(
                        "/api/work-orders/{id}/equipments/{equipmentId}/diagnosis",
                        orderId, equipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\":\"Ignition electrode damaged\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipments[0].diagnosis")
                        .value("Ignition electrode damaged"));

        mockMvc.perform(patch(
                        "/api/work-orders/{id}/equipments/{equipmentId}/diagnosis/complete",
                        orderId, equipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipments[0].status").value("WAITING_CUSTOMER"));

        mockMvc.perform(patch(
                        "/api/work-orders/{id}/equipments/{equipmentId}/approve",
                        orderId, equipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partsAvailable\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipments[0].customerDecision").value("APPROVED"))
                .andExpect(jsonPath("$.equipments[0].status").value("WAITING_PARTS"));

        mockMvc.perform(patch(
                        "/api/work-orders/{id}/equipments/{equipmentId}/start",
                        orderId, equipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipments[0].status").value("IN_PROGRESS"));

        mockMvc.perform(patch(
                        "/api/work-orders/{id}/equipments/{equipmentId}/complete",
                        orderId, equipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipments[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.equipments[0].completedAt").isNotEmpty());
    }

    @Test
    void exposesDirectServiceAndRejectionRoutesPerEquipment() throws Exception {
        InMemoryWorkOrderRepository repository = new InMemoryWorkOrderRepository();

        WorkOrder directOrder = newV2Order(EquipmentIntakeRoute.DIRECT_SERVICE);
        repository.save(directOrder);

        MockMvc mockMvc = mockMvc(repository);
        UUID directEquipmentId = directOrder.equipments().get(0).id().value();

        mockMvc.perform(patch(
                        "/api/work-orders/{id}/equipments/{equipmentId}/waiting-parts",
                        directOrder.id().value(), directEquipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipments[0].status").value("WAITING_PARTS"));

        WorkOrder diagnosisOrder = newV2Order(EquipmentIntakeRoute.DIAGNOSIS_REQUIRED);
        repository.save(diagnosisOrder);
        UUID diagnosisEquipmentId = diagnosisOrder.equipments().get(0).id().value();

        mockMvc.perform(patch(
                        "/api/work-orders/{id}/equipments/{equipmentId}/diagnosis/begin",
                        diagnosisOrder.id().value(), diagnosisEquipmentId))
                .andExpect(status().isOk());

        mockMvc.perform(patch(
                        "/api/work-orders/{id}/equipments/{equipmentId}/diagnosis",
                        diagnosisOrder.id().value(), diagnosisEquipmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\":\"Ignition electrode damaged\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch(
                        "/api/work-orders/{id}/equipments/{equipmentId}/diagnosis/complete",
                        diagnosisOrder.id().value(), diagnosisEquipmentId))
                .andExpect(status().isOk());

        mockMvc.perform(patch(
                        "/api/work-orders/{id}/equipments/{equipmentId}/reject",
                        diagnosisOrder.id().value(), diagnosisEquipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equipments[0].customerDecision").value("REJECTED"))
                .andExpect(jsonPath("$.equipments[0].status").value("NOT_APPROVED"));
    }

    private MockMvc mockMvc(InMemoryWorkOrderRepository repository) {
        WorkOrderController controller = new WorkOrderController(
                new CreateWorkOrderUseCase(repository),
                new ListWorkOrdersUseCase(repository),
                new GetWorkOrderUseCase(repository),
                new StartWorkUseCase(repository),
                new CompleteWorkUseCase(repository, (destination, message) -> { }),
                new WorkOrderWorkflowUseCase(repository),
                mock(WorkOrderEquipmentServiceUseCases.class),
                new WorkOrderEquipmentWorkflowUseCase(repository));

        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private WorkOrder newV2Order(EquipmentIntakeRoute route) {
        Instant receivedAt = Instant.parse("2026-10-03T12:00:00Z");
        WorkOrderId orderId = new WorkOrderId(
                "ORDER-" + UUID.randomUUID().toString().toUpperCase());
        EquipmentId equipmentId = new EquipmentId(UUID.randomUUID());

        WorkOrderEquipmentLifecycle lifecycle =
                WorkOrderEquipmentLifecycle.create(
                        route,
                        route == EquipmentIntakeRoute.DIAGNOSIS_REQUIRED
                                ? "Does not ignite"
                                : null,
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
