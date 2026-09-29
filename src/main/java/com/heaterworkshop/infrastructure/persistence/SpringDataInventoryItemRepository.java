package com.heaterworkshop.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataInventoryItemRepository extends JpaRepository<JpaInventoryItemEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from JpaInventoryItemEntity item where item.id = :id")
    Optional<JpaInventoryItemEntity> findByIdForUpdate(@Param("id") UUID id);
}