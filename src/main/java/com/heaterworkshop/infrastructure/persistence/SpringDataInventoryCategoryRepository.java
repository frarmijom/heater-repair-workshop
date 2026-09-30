package com.heaterworkshop.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import java.util.Optional;
public interface SpringDataInventoryCategoryRepository extends JpaRepository<JpaInventoryCategoryEntity, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select category from JpaInventoryCategoryEntity category where category.id = :id")
	Optional<JpaInventoryCategoryEntity> findByIdForUpdate(@Param("id") UUID id);
}
