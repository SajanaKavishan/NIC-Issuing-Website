
package com.project.nic.service;

import com.project.nic.model.RenewNic;
import com.project.nic.repository.RenewNicRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RenewNicService {
    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING", "PROCESSING", "APPROVED", "REJECTED", "DELIVERED");

    private final RenewNicRepository repository;
    private final NotificationService notificationService;

    public RenewNicService(RenewNicRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public RenewNic save(RenewNic renewNic) {
        boolean isNew = renewNic.getId() == null;
        if (renewNic.getStatus() == null || renewNic.getStatus().isBlank()) {
            renewNic.setStatus("PENDING");
        }
        RenewNic saved = repository.save(renewNic);
        if (isNew) {
            notificationService.applicationSubmitted("renew", saved.getId(), saved.getUserEmail(), saved.getContactNumber());
        }
        return saved;
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
            String previousStatus = existing.getStatus();
            existing.setStatus(normalizedStatus);
            RenewNic saved = repository.save(existing);
            if (!normalizedStatus.equalsIgnoreCase(previousStatus == null ? "" : previousStatus)) {
                notificationService.applicationStatusChanged("renew", saved.getId(), saved.getStatus(), saved.getUserEmail(), saved.getContactNumber());
            }
            return saved;
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
