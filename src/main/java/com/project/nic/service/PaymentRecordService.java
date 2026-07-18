package com.project.nic.service;

import com.project.nic.model.PaymentRecord;
import com.project.nic.repository.PaymentRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentRecordService {

    private final PaymentRecordRepository repository;

    public PaymentRecordService(PaymentRecordRepository repository) {
        this.repository = repository;
    }

    public PaymentRecord save(PaymentRecord record) {
        return repository.save(record);
    }

    public List<PaymentRecord> getAll() {
        return repository.findAll();
    }

    public List<PaymentRecord> getByUserId(Long userId) {
        return repository.findByUserId(String.valueOf(userId));
    }
}
