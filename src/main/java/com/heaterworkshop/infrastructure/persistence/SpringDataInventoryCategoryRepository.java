package com.heaterworkshop.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface SpringDataInventoryCategoryRepository extends JpaRepository<JpaInventoryCategoryEntity, UUID> {}
