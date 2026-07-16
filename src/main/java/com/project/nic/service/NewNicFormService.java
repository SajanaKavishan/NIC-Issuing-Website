package com.project.nic.service;

import com.project.nic.model.NewNicForm;
import com.project.nic.repository.NewNicFormRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class NewNicFormService {
    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED", "PROCESSING", "DELIVERED");

    @Autowired
    private NewNicFormRepository repository;

    public NewNicForm save(NewNicForm form) {
        if (form.getStatus() == null || form.getStatus().isBlank()) {
            form.setStatus("PENDING");
        }
        return repository.save(form);
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
            existing.setStatus(normalizedStatus);
            return repository.save(existing);
        }).orElseThrow(() -> new IllegalArgumentException("New NIC application with id " + id + " not found"));
    }

    private String normalizeStatus(String status) {
        String normalizedStatus = status == null ? "PENDING" : status.trim().toUpperCase();
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        return normalizedStatus;
    }
}
