
package com.project.nic.service;

import com.project.nic.model.RenewNic;
import com.project.nic.repository.RenewNicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RenewNicService {
    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING", "PROCESSING", "APPROVED", "REJECTED", "DELIVERED");

    @Autowired
    private RenewNicRepository repository;

    public RenewNic save(RenewNic renewNic) {
        if (renewNic.getStatus() == null || renewNic.getStatus().isBlank()) {
            renewNic.setStatus("PENDING");
        }
        return repository.save(renewNic);
    }

    public List<RenewNic> findAll() {
        return repository.findAll();
    }

    public List<RenewNic> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public Optional<RenewNic> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<RenewNic> findLatestByOldNicNumber(String oldNicNumber) {
        if (oldNicNumber == null || oldNicNumber.isBlank()) {
            return Optional.empty();
        }
        return repository.findFirstByOldNicNumberIgnoreCaseOrderByIdDesc(oldNicNumber.trim());
    }

    public RenewNic updateStatus(Long id, String status) {
        String normalizedStatus = normalizeStatus(status);
        return repository.findById(id).map(existing -> {
            existing.setStatus(normalizedStatus);
            return repository.save(existing);
        }).orElseThrow(() -> new IllegalArgumentException("Renew NIC application with id " + id + " not found"));
    }

    private String normalizeStatus(String status) {
        String normalizedStatus = status == null ? "PENDING" : status.trim().toUpperCase().replaceAll("\\s+", "_");
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        return normalizedStatus;
    }
}
