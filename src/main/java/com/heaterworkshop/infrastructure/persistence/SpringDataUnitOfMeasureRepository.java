package com.heaterworkshop.infrastructure.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import java.util.Optional;
public interface SpringDataUnitOfMeasureRepository extends JpaRepository<JpaUnitOfMeasureEntity, UUID> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select unit from JpaUnitOfMeasureEntity unit where unit.id = :id")
	Optional<JpaUnitOfMeasureEntity> findByIdForUpdate(@Param("id") UUID id);
}
