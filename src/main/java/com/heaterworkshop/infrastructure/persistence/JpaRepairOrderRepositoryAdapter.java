package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.entity.RepairOrder;
import com.heaterworkshop.domain.repository.RepairOrderRepository;
import com.heaterworkshop.domain.valueobject.CustomerContact;
import com.heaterworkshop.domain.valueobject.Diagnosis;
import com.heaterworkshop.domain.valueobject.RepairOrderId;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Primary
@Transactional
public class JpaRepairOrderRepositoryAdapter implements RepairOrderRepository {
    private final SpringDataRepairOrderRepository repository;
    public JpaRepairOrderRepositoryAdapter(SpringDataRepairOrderRepository repository) { this.repository = repository; }

    @Override
    public void save(RepairOrder order) {
        repository.save(new JpaRepairOrderEntity(order.id().value(), order.customerContact().value(),
                order.status(), order.diagnosis() == null ? null : order.diagnosis().value()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RepairOrder> findById(RepairOrderId id) {
        return repository.findById(id.value()).map(entity -> RepairOrder.restore(
                new RepairOrderId(entity.getId()),
                new CustomerContact(entity.getCustomerContact()),
                entity.getStatus(),
                entity.getDiagnosis() == null ? null : new Diagnosis(entity.getDiagnosis())));
    }
}
