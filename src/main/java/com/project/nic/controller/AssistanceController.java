package com.project.nic.controller;

import com.project.nic.model.AssistanceRequest;
import com.project.nic.service.AssistanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assistance")
@CrossOrigin("*")
public class AssistanceController {

    @Autowired
    private AssistanceService assistanceService;

    @PostMapping("/request")
    public ResponseEntity<String> createRequest(@RequestBody AssistanceRequest request) {
        AssistanceRequest saved = assistanceService.createRequest(request);
        return ResponseEntity.ok("Request submitted successfully: id=" + (saved != null ? saved.getId() : "null"));
    }

    @GetMapping("/all")
    public ResponseEntity<Iterable<AssistanceRequest>> getAllRequests() {
        return ResponseEntity.ok(assistanceService.getAllRequests());
    }

    @PostMapping("/reply/{id}")
    public ResponseEntity<String> replyToRequest(@PathVariable Long id, @RequestBody String replyMessage) {
        assistanceService.replyToRequest(id, replyMessage);
        return ResponseEntity.ok("Reply sent successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRequest(@PathVariable Long id) {
        try {
            assistanceService.deleteRequest(id);
            return ResponseEntity.ok("Request deleted successfully");
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/request/{id}")
    public ResponseEntity<String> deleteRequestByRequestPath(@PathVariable Long id) {
        try {
            assistanceService.deleteRequest(id);
            return ResponseEntity.ok("Request deleted successfully");
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/request/{id}")
    public ResponseEntity<AssistanceRequest> updateRequest(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String newQuery = body.getOrDefault("query", "").toString();
        try {
            AssistanceRequest updated = assistanceService.updateRequest(id, newQuery);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/request/{userId}")
    public ResponseEntity<AssistanceRequest> getRequestByUserId(@PathVariable Long userId) {
        AssistanceRequest request = assistanceService.getRequestByUserId(userId);
        return request != null ? ResponseEntity.ok(request) : ResponseEntity.notFound().build();
    }

    @GetMapping("/requestsByEmail")
    public ResponseEntity<List<AssistanceRequest>> getRequestsByEmail(@RequestParam String email) {
        List<AssistanceRequest> requests = assistanceService.getRequestsByEmail(email);
        return ResponseEntity.ok(requests);
    }
}