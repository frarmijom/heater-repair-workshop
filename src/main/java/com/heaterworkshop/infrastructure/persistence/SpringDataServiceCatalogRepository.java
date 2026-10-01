package com.heaterworkshop.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataServiceCatalogRepository extends JpaRepository<JpaServiceCatalogItemEntity, UUID> {
    Optional<JpaServiceCatalogItemEntity> findByCodeNormalized(String codeNormalized);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select service from JpaServiceCatalogItemEntity service where service.id = :id")
    Optional<JpaServiceCatalogItemEntity> findByIdForUpdate(@Param("id") UUID id);
}
