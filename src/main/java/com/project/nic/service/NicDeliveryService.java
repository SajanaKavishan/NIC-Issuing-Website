
package com.project.nic.service;

import com.project.nic.model.NicDelivery;
import com.project.nic.repository.NicDeliveryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NicDeliveryService {

    @Autowired
    private NicDeliveryRepository repository;

    public List<NicDelivery> getAllDeliveries() {
        return repository.findAll();
    }

    public Optional<NicDelivery> getDeliveryById(Long id) {
        return repository.findById(id);
    }

    public NicDelivery saveDelivery(NicDelivery delivery) {
        return repository.save(delivery);
    }

    public void deleteDelivery(Long id) {
        repository.deleteById(id);
    }
}
