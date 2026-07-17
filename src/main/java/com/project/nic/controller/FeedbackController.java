package com.project.nic.controller;

import com.project.nic.model.Feedback;
import com.project.nic.repository.FeedbackRepository;
import com.project.nic.service.AuthSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private AuthSessionService authSessionService;

    private boolean canManageFeedback(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "PRO");
    }

    @PostMapping
    public Feedback submitFeedback(@RequestBody Feedback feedback) {
        // Normalize type to expected values (convert 'complaint' to 'complain')
        if (feedback.getType() != null && feedback.getType().equalsIgnoreCase("complaint")) {
            feedback.setType("complain");
        }
        return feedbackRepository.save(feedback);
    }

    @GetMapping
    public ResponseEntity<?> getAllFeedbacks(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!canManageFeedback(token)) {
            return ResponseEntity.status(403).body("PRO access required");
        }
        return ResponseEntity.ok(feedbackRepository.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFeedback(
            @PathVariable Long id,
            @RequestBody Feedback feedback,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageFeedback(token)) {
            return ResponseEntity.status(403).body("PRO access required");
        }
        feedback.setId(id);
        return ResponseEntity.ok(feedbackRepository.save(feedback));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFeedback(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageFeedback(token)) {
            return ResponseEntity.status(403).body("PRO access required");
        }
        feedbackRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
