package com.project.nic.service;

import com.project.nic.model.PaymentRecord;
import com.project.nic.repository.PaymentRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentRecordService {

    @Autowired
    private PaymentRecordRepository repository;

    public PaymentRecord save(PaymentRecord record) {
        return repository.save(record);
    }

    public List<PaymentRecord> getAll() {
        return repository.findAll();
    }
}
