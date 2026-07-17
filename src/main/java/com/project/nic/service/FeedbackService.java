package com.project.nic.service;

import com.project.nic.model.Feedback;
import com.project.nic.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class FeedbackService {
    private static final Set<String> ALLOWED_STATUSES = Set.of("Pending", "In Progress", "Resolved", "Reviewed");

    @Autowired
    private FeedbackRepository feedbackRepository;

    public Feedback saveFeedback(Feedback feedback) {
        normalize(feedback);
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getAll() {
        return feedbackRepository.findAll();
    }

    public Optional<Feedback> findById(Long id) {
        return feedbackRepository.findById(id);
    }

    public Feedback updateFeedback(Long id, Feedback updates) {
        return feedbackRepository.findById(id).map(existing -> {
            if (updates.getName() != null) existing.setName(updates.getName());
            if (updates.getMail() != null) existing.setMail(updates.getMail());
            if (updates.getType() != null) existing.setType(updates.getType());
            if (updates.getSubject() != null) existing.setSubject(updates.getSubject());
            if (updates.getMessage() != null) existing.setMessage(updates.getMessage());
            if (updates.getStatus() != null) existing.setStatus(updates.getStatus());
            if (updates.getReply() != null) existing.setReply(updates.getReply());
            normalize(existing);
            return feedbackRepository.save(existing);
        }).orElseThrow(() -> new IllegalArgumentException("Feedback with id " + id + " not found"));
    }

    public void deleteById(Long id) {
        feedbackRepository.deleteById(id);
    }

    private void normalize(Feedback feedback) {
        if (feedback.getType() != null && feedback.getType().equalsIgnoreCase("complaint")) {
            feedback.setType("complain");
        }

        String status = feedback.getStatus();
        if (status == null || status.isBlank()) {
            feedback.setStatus("Pending");
            return;
        }

        String normalized = status.trim().replace('_', ' ');
        for (String allowed : ALLOWED_STATUSES) {
            if (allowed.equalsIgnoreCase(normalized)) {
                feedback.setStatus(allowed);
                return;
            }
        }
        feedback.setStatus("Pending");
    }
}
