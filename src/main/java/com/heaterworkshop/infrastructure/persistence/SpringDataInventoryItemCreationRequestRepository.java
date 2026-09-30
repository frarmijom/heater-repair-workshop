package com.heaterworkshop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SpringDataInventoryItemCreationRequestRepository extends JpaRepository<JpaInventoryItemCreationRequestEntity, String> {
    @Modifying
    @Query(value = "INSERT INTO inventory_item_creation_requests(request_id,item_id,payload_hash,created_at) " +
            "VALUES (:requestId,:itemId,:payloadHash,:createdAt) ON CONFLICT (request_id) DO NOTHING", nativeQuery = true)
    int claim(@Param("requestId") String requestId, @Param("itemId") java.util.UUID itemId,
              @Param("payloadHash") String payloadHash, @Param("createdAt") Instant createdAt);

        @Modifying
        @Query(value = "MERGE INTO inventory_item_creation_requests target " +
            "USING (VALUES (:requestId,:itemId,:payloadHash,:createdAt)) AS incoming(request_id,item_id,payload_hash,created_at) " +
            "ON target.request_id=incoming.request_id " +
            "WHEN NOT MATCHED THEN INSERT(request_id,item_id,payload_hash,created_at) " +
            "VALUES(incoming.request_id,incoming.item_id,incoming.payload_hash,incoming.created_at)", nativeQuery = true)
        int claimH2(@Param("requestId") String requestId, @Param("itemId") java.util.UUID itemId,
            @Param("payloadHash") String payloadHash, @Param("createdAt") Instant createdAt);

    Optional<JpaInventoryItemCreationRequestEntity> findByRequestId(String requestId);
}