package com.project.nic.controller;

import com.project.nic.dto.ApiDtos.AssistanceRequestDto;
import com.project.nic.model.AssistanceRequest;
import com.project.nic.service.AuthAccessService;
import com.project.nic.service.AuthSessionService;
import com.project.nic.service.AssistanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assistance")
public class AssistanceController {

    @Autowired
    private AssistanceService assistanceService;

    @Autowired
    private AuthAccessService authAccessService;

    @PostMapping("/request")
    public ResponseEntity<String> createRequest(
            @RequestBody AssistanceRequestDto requestBody,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        AssistanceRequest request = requestBody.toEntity();
        authAccessService.currentUser(token).ifPresent(sessionUser -> {
            request.setUserId(sessionUser.userId());
            request.setEmail(sessionUser.email());
        });
        AssistanceRequest saved = assistanceService.createRequest(request);
        return ResponseEntity.ok("Request submitted successfully: id=" + (saved != null ? saved.getId() : "null"));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllRequests(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!authAccessService.canManageAssistance(token)) {
            return ResponseEntity.status(403).body("Assistant access required");
        }
        return ResponseEntity.ok(assistanceService.getAllRequests().stream().map(AssistanceRequestDto::from).collect(Collectors.toList()));
    }

    @PostMapping("/reply/{id}")
    public ResponseEntity<String> replyToRequest(
            @PathVariable Long id,
            @RequestBody String replyMessage,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageAssistance(token)) {
            return ResponseEntity.status(403).body("Assistant access required");
        }
        assistanceService.replyToRequest(id, replyMessage);
        return ResponseEntity.ok("Reply sent successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRequest(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        if (!authAccessService.canManageAssistance(token)) {
            return ResponseEntity.status(403).body("Assistant access required");
        }
        try {
            assistanceService.deleteRequest(id);
            return ResponseEntity.ok("Request deleted successfully");
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/request/{id}")
    public ResponseEntity<String> deleteRequestByRequestPath(
            @PathVariable Long id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        Optional<AuthSessionService.SessionUser> sessionUser = authAccessService.currentUser(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }

        Optional<AssistanceRequest> request = assistanceService.findById(id);
        if (request.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!authAccessService.ownsAssistanceRequest(sessionUser.get(), request.get())) {
            return ResponseEntity.status(403).body("You can only delete your own assistance requests");
        }

        try {
            assistanceService.deleteRequest(id);
            return ResponseEntity.ok("Request deleted successfully");
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/request/{id}")
    public ResponseEntity<?> updateRequest(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Auth-Token", required = false) String token
    ) {
        Optional<AuthSessionService.SessionUser> sessionUser = authAccessService.currentUser(token);
        if (sessionUser.isEmpty()) {
            return ResponseEntity.status(403).body("Login required");
        }

        Optional<AssistanceRequest> request = assistanceService.findById(id);
        if (request.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!authAccessService.ownsAssistanceRequest(sessionUser.get(), request.get())) {
            return ResponseEntity.status(403).body("You can only update your own assistance requests");
        }

        String newQuery = body.getOrDefault("query", "").toString();
        try {
            AssistanceRequest updated = assistanceService.updateRequest(id, newQuery);
            return ResponseEntity.ok(AssistanceRequestDto.from(updated));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/request/{userId}")
    public ResponseEntity<AssistanceRequestDto> getRequestByUserId(@PathVariable Long userId) {
        AssistanceRequest request = assistanceService.getRequestByUserId(userId);
        return request != null ? ResponseEntity.ok(AssistanceRequestDto.from(request)) : ResponseEntity.notFound().build();
    }

    @GetMapping("/requestsByEmail")
    public ResponseEntity<List<AssistanceRequestDto>> getRequestsByEmail(@RequestParam String email) {
        List<AssistanceRequest> requests = assistanceService.getRequestsByEmail(email);
        return ResponseEntity.ok(requests.stream().map(AssistanceRequestDto::from).collect(Collectors.toList()));
    }
}
