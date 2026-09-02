package com.heaterworkshop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRepairOrderRepository extends JpaRepository<JpaRepairOrderEntity, String> {
}
