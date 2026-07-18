package com.project.nic.controller;

import com.project.nic.dto.ApiDtos.FeedbackDto;
import com.project.nic.service.AuthAccessService;
import com.project.nic.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private AuthAccessService authAccessService;

    @PostMapping
    public FeedbackDto submitFeedback(@Valid @RequestBody FeedbackDto feedback) {
        return FeedbackDto.from(feedbackService.saveFeedback(feedback.toEntity()));
    }

    @GetMapping
    public ResponseEntity<?> getAllFeedbacks(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authAccessService.canManageFeedback(token)) {
            return ResponseEntity.status(403).body("PRO access required");
        }
        return ResponseEntity.ok(feedbackService.getAll().stream().map(FeedbackDto::from).collect(Collectors.toList()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFeedback(
            @PathVariable Long id,
            @Valid @RequestBody FeedbackDto feedback,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageFeedback(token)) {
            return ResponseEntity.status(403).body("PRO access required");
        }
        try {
            return ResponseEntity.ok(FeedbackDto.from(feedbackService.updateFeedback(id, feedback.toEntity())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFeedback(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageFeedback(token)) {
            return ResponseEntity.status(403).body("PRO access required");
        }
        feedbackService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
