package com.project.nic.repository;

import com.project.nic.model.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    List<PaymentRecord> findByUserId(String userId);
}
