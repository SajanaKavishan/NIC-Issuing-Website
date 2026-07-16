package com.project.nic.service;

import com.project.nic.model.Feedback;
import com.project.nic.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {
    @Autowired
    private FeedbackRepository feedbackRepository;

    public Feedback saveFeedback(Feedback feedback) {
        // You can add validation or business logic here
        return feedbackRepository.save(feedback);
    }
}
