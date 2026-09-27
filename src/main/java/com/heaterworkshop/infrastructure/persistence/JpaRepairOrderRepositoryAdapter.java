package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.entity.ServiceType;
import com.heaterworkshop.domain.repository.RepairOrderRepository;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
@Transactional
public class JpaRepairOrderRepositoryAdapter implements RepairOrderRepository {
    private final SpringDataRepairOrderRepository repository;
    public JpaRepairOrderRepositoryAdapter(SpringDataRepairOrderRepository repository) { this.repository = repository; }

    @Override
    public void save(RepairOrder order) {
        repository.save(toEntity(order));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RepairOrder> findById(RepairOrderId id) {
        return repository.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairOrder> findAllByReceivedAtDescending() {
        return repository.findAllByOrderByReceivedAtDesc().stream().map(this::toDomain).toList();
    }

    private JpaRepairOrderEntity toEntity(RepairOrder order) {
        return new JpaRepairOrderEntity(order.id().value(), order.customerName(),
                order.customerContact().value(), order.heaterBrand(), order.heaterModel(),
                order.serviceType(), order.reportedIssue(), order.status(),
                order.diagnosis() == null ? null : order.diagnosis().value(),
                order.receivedAt(), order.completedAt());
    }

    private RepairOrder toDomain(JpaRepairOrderEntity entity) {
        return RepairOrder.restore(new RepairOrderId(entity.getId()), entity.getCustomerName(),
                new CustomerContact(entity.getCustomerContact()), entity.getHeaterBrand(),
                entity.getHeaterModel(),
                // Only persisted historical rows may omit the service type.
                entity.getServiceType() == null ? ServiceType.REPAIR : entity.getServiceType(),
                entity.getReportedIssue(), entity.getStatus(),
                entity.getDiagnosis() == null ? null : new Diagnosis(entity.getDiagnosis()),
                entity.getReceivedAt(), entity.getCompletedAt());
    }
}
