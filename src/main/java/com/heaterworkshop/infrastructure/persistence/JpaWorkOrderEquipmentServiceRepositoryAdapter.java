package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.service.WorkOrderEquipmentService;
import com.heaterworkshop.domain.service.WorkOrderEquipmentServiceRepository;
import com.heaterworkshop.domain.valueobject.EquipmentId;
import com.heaterworkshop.domain.valueobject.WorkOrderId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class JpaWorkOrderEquipmentServiceRepositoryAdapter
        implements WorkOrderEquipmentServiceRepository {

    private final SpringDataWorkOrderEquipmentServiceRepository repository;

    public JpaWorkOrderEquipmentServiceRepositoryAdapter(
            SpringDataWorkOrderEquipmentServiceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrderEquipmentService> findByWorkOrderIdAndEquipmentId(
            WorkOrderId workOrderId,
            EquipmentId equipmentId) {

        return repository
                .findByWorkOrderIdAndEquipmentId(
                        workOrderId.value(),
                        equipmentId.value())
                .stream()
                .map(JpaWorkOrderEquipmentServiceEntity::toDomain)
                .toList();
    }

    @Override
    public void replace(
            WorkOrderId workOrderId,
            EquipmentId equipmentId,
            List<WorkOrderEquipmentService> services) {

        repository.deleteByWorkOrderIdAndEquipmentId(
                workOrderId.value(),
                equipmentId.value());

        repository.flush();

        repository.saveAllAndFlush(
                services.stream()
                        .map(JpaWorkOrderEquipmentServiceEntity::new)
                        .toList());
    }
}
