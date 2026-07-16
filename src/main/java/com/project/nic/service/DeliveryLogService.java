package com.project.nic.service;

import com.project.nic.model.DeliveryLog;
import com.project.nic.repository.DeliveryLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DeliveryLogService {

    @Autowired
    private DeliveryLogRepository deliveryLogRepository;

    public List<DeliveryLog> getAll() {
        return deliveryLogRepository.findAll();
    }

    public Optional<DeliveryLog> getById(Long id) {
        return deliveryLogRepository.findById(id);
    }

    public DeliveryLog save(DeliveryLog log) {
        return deliveryLogRepository.save(log);
    }

    public void delete(Long id) {
        deliveryLogRepository.deleteById(id);
    }
}

