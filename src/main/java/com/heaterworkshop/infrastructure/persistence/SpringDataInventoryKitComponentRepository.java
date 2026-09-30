package com.heaterworkshop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataInventoryKitComponentRepository extends JpaRepository<JpaInventoryKitComponentEntity, JpaInventoryKitComponentId> {
    List<JpaInventoryKitComponentEntity> findByKitItemId(UUID kitItemId);
    void deleteByKitItemId(UUID kitItemId);
}
