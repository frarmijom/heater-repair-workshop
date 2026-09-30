package com.heaterworkshop.infrastructure.persistence;

import com.heaterworkshop.domain.inventory.InventoryMovement;
import com.heaterworkshop.domain.inventory.InventoryMovementRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public class JpaInventoryMovementRepositoryAdapter implements InventoryMovementRepository {
    private final SpringDataInventoryMovementRepository repository;
    private final SpringDataInventoryItemRepository items;

    public JpaInventoryMovementRepositoryAdapter(SpringDataInventoryMovementRepository repository,
                                                  SpringDataInventoryItemRepository items) {
        this.repository = repository;
        this.items = items;
    }

    @Override @Transactional(readOnly = true)
    public Optional<InventoryMovement> findByRequestId(String requestId) {
        return repository.findByRequestId(requestId).map(JpaInventoryMovementEntity::toDomain);
    }

    @Override @Transactional(readOnly = true)
    public Optional<InventoryMovement> findById(UUID id) {
        return repository.findById(id).map(JpaInventoryMovementEntity::toDomain);
    }

    @Override @Transactional(readOnly = true)
    public Optional<InventoryMovement> findByReversalOfMovementId(UUID id) {
        return repository.findByReversalOfMovementId(id).map(JpaInventoryMovementEntity::toDomain);
    }

    @Override
    public InventoryMovement append(InventoryMovement movement) {
        UUID unitId = items.findById(movement.itemId()).orElseThrow().toDomain().unitId();
        return repository.saveAndFlush(new JpaInventoryMovementEntity(movement, unitId)).toDomain();
    }

    @Override @Transactional(readOnly = true)
    public boolean existsByUnitId(UUID unitId) {
        return repository.existsByUnitId(unitId);
    }

    @Override @Transactional(readOnly = true)
    public long countByItemId(UUID itemId) {
        return repository.countByItemId(itemId);
    }

    @Override @Transactional(readOnly = true)
    public List<InventoryMovement> findByItemId(UUID itemId) {
        return repository.findByItemIdOrderByOccurredAtDesc(itemId).stream()
                .map(JpaInventoryMovementEntity::toDomain).toList();
    }
}