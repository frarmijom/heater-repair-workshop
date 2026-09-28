package com.heaterworkshop.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataWorkOrderRepository extends JpaRepository<JpaWorkOrderEntity, String> {
    List<JpaWorkOrderEntity> findAllByOrderByReceivedAtDesc();
}
