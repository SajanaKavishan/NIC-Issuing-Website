package com.project.nic.service;

import com.project.nic.model.AssistanceRequest;
import com.project.nic.repository.AssistanceRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@Service
public class AssistanceService {

    private static final Logger logger = LoggerFactory.getLogger(AssistanceService.class);

    @Autowired
    private AssistanceRequestRepository repo;

    @PersistenceContext
    private EntityManager entityManager;

    public AssistanceRequest createRequest(AssistanceRequest request) {
        request.setStatus("pending");
        return repo.save(request);
    }

    public List<AssistanceRequest> getPendingRequests() {
        return repo.findByStatus("pending");
    }

    public List<AssistanceRequest> getAllRequests() {
        return repo.findAll();
    }

    public void replyToRequest(Long id, String replyMessage) {
        AssistanceRequest request = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        request.setReply(replyMessage);
        request.setStatus("resolved");
        repo.save(request);
    }

    @Transactional
    public void deleteRequest(Long id) {
        logger.info("Starting transaction to delete request with ID: {}", id);
        AssistanceRequest request = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found with ID: " + id));
        logger.info("Found request in transaction: {}", request);
        repo.delete(request);
        entityManager.flush();
        logger.info("Request with ID: {} deleted and flushed to database", id);
        if (!repo.existsById(id)) {
            logger.info("Verification: Request with ID: {} no longer exists", id);
        } else {
            logger.warn("Verification failed: Request with ID: {} still exists", id);
        }
        logger.info("Transaction committed for delete request with ID: {}", id);
    }

    public AssistanceRequest getRequestByUserId(Long userId) {
        return repo.findByUserId(userId).stream().findFirst().orElse(null);
    }

    public List<AssistanceRequest> getRequestsByEmail(String email) {
        logger.info("Fetching requests for email: {}", email);
        return repo.findByEmail(email);
    }

    @Transactional
    public AssistanceRequest updateRequest(Long id, String newQuery) {
        AssistanceRequest request = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found with ID: " + id));
        request.setQuery(newQuery);
        AssistanceRequest updated = repo.save(request);
        logger.info("Updated request with ID: {}", id);
        return updated;
    }

}