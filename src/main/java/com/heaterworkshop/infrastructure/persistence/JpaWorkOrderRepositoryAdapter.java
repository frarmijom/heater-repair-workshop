package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.WorkOrder;
import com.heaterworkshop.domain.entity.WorkOrderEquipment;
import com.heaterworkshop.domain.entity.ServiceType;
import com.heaterworkshop.domain.repository.WorkOrderRepository;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
@Transactional
public class JpaWorkOrderRepositoryAdapter implements WorkOrderRepository {
    private final SpringDataWorkOrderRepository repository;
    public JpaWorkOrderRepositoryAdapter(SpringDataWorkOrderRepository repository) { this.repository = repository; }

    @Override
    public void save(WorkOrder order) {
        repository.save(toEntity(order));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkOrder> findById(WorkOrderId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrder> findAllByReceivedAtDescending() {
        return repository.findAllByOrderByReceivedAtDesc().stream().map(this::toDomain).toList();
    }

    private JpaWorkOrderEntity toEntity(WorkOrder order) {
        return new JpaWorkOrderEntity(order.id().value(), order.customerName(),
                order.customerContact().value(), order.heaterBrand(), order.heaterModel(),
                order.serviceType(), order.reportedIssue(), order.status(),
                order.diagnosis() == null ? null : order.diagnosis().value(),
                order.receivedAt(), order.completedAt(), order.lifecycleVersion(), order.legacyStatus(), order.customerDecision(),
                order.equipments().stream().map(this::toEquipmentEntity).toList());
    }

    private WorkOrder toDomain(JpaWorkOrderEntity entity) {
        return WorkOrder.restore(new WorkOrderId(entity.getId()), entity.getCustomerName(),
                new CustomerContact(entity.getCustomerContact()),
                entity.getEquipments().stream().map(this::toEquipmentDomain).toList(),
                // Only persisted historical rows may omit the service type.
                entity.getServiceType() == null && entity.getLifecycleVersion() == com.heaterworkshop.domain.entity.LifecycleVersion.LEGACY
                        ? ServiceType.REPAIR : entity.getServiceType(),
                entity.getReportedIssue(), entity.getStatus(),
                entity.getDiagnosis() == null ? null : new Diagnosis(entity.getDiagnosis()),
                entity.getReceivedAt(), entity.getCompletedAt(), entity.getLifecycleVersion(),
                entity.getLegacyStatus(), entity.getCustomerDecision());
    }

    private JpaWorkOrderEquipmentEntity toEquipmentEntity(WorkOrderEquipment equipment) {
        return new JpaWorkOrderEquipmentEntity(equipment.id().value(), equipment.brand(), equipment.model(),
                equipment.capacity(), equipment.serialNumber(), equipment.notes(), equipment.position());
    }

    private WorkOrderEquipment toEquipmentDomain(JpaWorkOrderEquipmentEntity entity) {
        return new WorkOrderEquipment(new EquipmentId(entity.getId()), entity.getBrand(), entity.getModel(),
                entity.getCapacity(), entity.getSerialNumber(), entity.getNotes(), entity.getPosition());
    }
}
