package com.project.nic.service;

import com.project.nic.model.NewNicForm;
import com.project.nic.repository.NewNicFormRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class NewNicFormService {
    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING", "PROCESSING", "APPROVED", "REJECTED", "DELIVERED");

    private final NewNicFormRepository repository;
    private final NotificationService notificationService;

    public NewNicFormService(NewNicFormRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public NewNicForm save(NewNicForm form) {
        boolean isNew = form.getId() == null;
        if (form.getStatus() == null || form.getStatus().isBlank()) {
            form.setStatus("PENDING");
        }
        NewNicForm saved = repository.save(form);
        if (isNew) {
            notificationService.applicationSubmitted("new", saved.getId(), saved.getUserEmail(), saved.getContactNumber());
        }
        return saved;
    }

    public List<NewNicForm> findAll() {
        return repository.findAll();
    }

    public List<NewNicForm> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public Optional<NewNicForm> findById(Long id) {
        return repository.findById(id);
    }

    public NewNicForm updateStatus(Long id, String status) {
        String normalizedStatus = normalizeStatus(status);
        return repository.findById(id).map(existing -> {
            String previousStatus = existing.getStatus();
            existing.setStatus(normalizedStatus);
            NewNicForm saved = repository.save(existing);
            if (!normalizedStatus.equalsIgnoreCase(previousStatus == null ? "" : previousStatus)) {
                notificationService.applicationStatusChanged("new", saved.getId(), saved.getStatus(), saved.getUserEmail(), saved.getContactNumber());
            }
            return saved;
        }).orElseThrow(() -> new IllegalArgumentException("New NIC application with id " + id + " not found"));
    }

    private String normalizeStatus(String status) {
        String normalizedStatus = status == null ? "PENDING" : status.trim().toUpperCase().replaceAll("\\s+", "_");
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        return normalizedStatus;
    }
}
