package com.project.nic.controller;

import com.project.nic.model.Feedback;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private AuthSessionService authSessionService;

    private boolean canManageFeedback(String token) {
        return authSessionService.hasAnyRole(token, "ADMIN", "PRO");
    }

    @PostMapping
    public Feedback submitFeedback(@RequestBody Feedback feedback) {
        return feedbackService.saveFeedback(feedback);
    }

    @GetMapping
    public ResponseEntity<?> getAllFeedbacks(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!canManageFeedback(token)) {
            return ResponseEntity.status(403).body("PRO access required");
        }
        return ResponseEntity.ok(feedbackService.getAll());
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
        try {
            return ResponseEntity.ok(feedbackService.updateFeedback(id, feedback));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFeedback(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!canManageFeedback(token)) {
            return ResponseEntity.status(403).body("PRO access required");
        }
        feedbackService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
