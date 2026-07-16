package com.project.nic.controller;

import com.project.nic.model.Feedback;
import com.project.nic.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin
public class FeedbackController {
    @Autowired
    private FeedbackRepository feedbackRepository;

    @PostMapping
    public Feedback submitFeedback(@RequestBody Feedback feedback) {
        // Normalize type to expected values (convert 'complaint' to 'complain')
        if (feedback.getType() != null && feedback.getType().equalsIgnoreCase("complaint")) {
            feedback.setType("complain");
        }
        return feedbackRepository.save(feedback);
    }

    @GetMapping
    public List<Feedback> getAllFeedbacks() {
        return feedbackRepository.findAll();
    }

    @PutMapping("/{id}")
    public Feedback updateFeedback(@PathVariable Long id, @RequestBody Feedback feedback) {
        feedback.setId(id);
        return feedbackRepository.save(feedback);
    }

    @DeleteMapping("/{id}")
    public void deleteFeedback(@PathVariable Long id) {
        feedbackRepository.deleteById(id);
    }
}
