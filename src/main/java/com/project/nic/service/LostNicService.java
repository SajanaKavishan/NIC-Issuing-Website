package com.project.nic.service;

import com.project.nic.model.LostNic;
import com.project.nic.repository.LostNicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class LostNicService {
    private static final Logger logger = LoggerFactory.getLogger(LostNicService.class);

    @Autowired
    private LostNicRepository repository;

    public LostNic save(LostNic lostNic) {
        return repository.save(lostNic);
    }

    // Additions for admin CRUD
    public List<LostNic> findAll() {
        return repository.findAll();
    }

    public Optional<LostNic> findById(Long id) {
        return repository.findById(id);
    }

    public LostNic update(Long id, LostNic updates) {
        return repository.findById(id).map(existing -> {
            // Only update editable fields
            if (updates.getNicNumber() != null) existing.setNicNumber(updates.getNicNumber());
            if (updates.getLostDate() != null) existing.setLostDate(updates.getLostDate());
            if (updates.getContactNumber() != null) existing.setContactNumber(updates.getContactNumber());
            // File paths are not updated here via JSON edits
            return repository.save(existing);
        }).orElseThrow(() -> new IllegalArgumentException("LostNic with id " + id + " not found"));
    }

    public void deleteById(Long id) {
        logger.info("Attempting to delete LostNic with ID: {}", id);
        if (!repository.existsById(id)) {
            logger.error("LostNic with ID: {} not found for deletion", id);
            throw new IllegalArgumentException("LostNic with id " + id + " not found");
        }
        try {
            repository.deleteById(id);
            logger.info("Successfully deleted LostNic with ID: {}", id);
        } catch (Exception e) {
            logger.error("Failed to delete LostNic with ID: {}", id, e);
            throw e;
        }
    }

    // New: update the status of a LostNic (PENDING, APPROVED, REJECTED)
    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED");

    public LostNic updateStatus(Long id, String status) {
        final String s = (status == null) ? "PENDING" : status.trim().toUpperCase();
        if (!ALLOWED_STATUSES.contains(s)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        return repository.findById(id).map(existing -> {
            existing.setStatus(s);
            logger.info("Updating status of LostNic id {} to {}", id, s);
            return repository.save(existing);
        }).orElseThrow(() -> new IllegalArgumentException("LostNic with id " + id + " not found"));
    }
}
