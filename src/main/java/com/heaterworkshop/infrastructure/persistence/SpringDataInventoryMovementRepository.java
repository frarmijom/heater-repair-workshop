package com.heaterworkshop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataInventoryMovementRepository extends JpaRepository<JpaInventoryMovementEntity, UUID> {
    Optional<JpaInventoryMovementEntity> findByRequestId(String requestId);
    Optional<JpaInventoryMovementEntity> findByReversalOfMovementId(UUID id);
    boolean existsByUnitId(UUID unitId);
    long countByItemId(UUID itemId);
}