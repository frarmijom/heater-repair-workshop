package com.heaterworkshop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataRepairOrderRepository extends JpaRepository<JpaRepairOrderEntity, String> {
    List<JpaRepairOrderEntity> findAllByOrderByReceivedAtDesc();
}
