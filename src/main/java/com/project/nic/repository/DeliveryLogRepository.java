package com.project.nic.repository;

import com.project.nic.model.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {
    // No custom methods required for now; basic CRUD will suffice
}

